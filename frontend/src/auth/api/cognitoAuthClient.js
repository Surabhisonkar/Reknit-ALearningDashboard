import { config } from "../../config/env.js";
import { deriveCodeChallenge, generateCodeVerifier, generateState } from "../utils/pkce.js";

const CODE_VERIFIER_KEY = "auth_pkce_code_verifier";
const STATE_KEY = "auth_pkce_state";
const RETURN_TO_KEY = "auth_return_to";

/**
 * Starts the login redirect: generates PKCE values, stashes them in
 * sessionStorage (survives the redirect round-trip, cleared when the tab
 * closes - never localStorage), then sends the browser to the Hosted UI.
 * {@code returnTo} is the in-app path to come back to after login.
 */
export async function redirectToLogin(returnTo = "/") {
  const codeVerifier = generateCodeVerifier();
  const codeChallenge = await deriveCodeChallenge(codeVerifier);
  const state = generateState();

  sessionStorage.setItem(CODE_VERIFIER_KEY, codeVerifier);
  sessionStorage.setItem(STATE_KEY, state);
  sessionStorage.setItem(RETURN_TO_KEY, returnTo);

  const url = new URL(`https://${config.cognito.domain}/oauth2/authorize`);
  url.searchParams.set("client_id", config.cognito.clientId);
  url.searchParams.set("response_type", "code");
  url.searchParams.set("scope", "openid email profile");
  url.searchParams.set("redirect_uri", config.cognito.redirectUri);
  url.searchParams.set("code_challenge", codeChallenge);
  url.searchParams.set("code_challenge_method", "S256");
  url.searchParams.set("state", state);

  window.location.assign(url.toString());
}

/**
 * Called from the /auth/callback route once Cognito redirects back with
 * ?code=...&state=.... Validates state (CSRF protection), exchanges the
 * code for tokens, and returns both the tokens and the original
 * {@code returnTo} path so the caller can navigate back there.
 */
export async function handleLoginCallback(searchParams) {
  const code = searchParams.get("code");
  const returnedState = searchParams.get("state");
  const error = searchParams.get("error");

  if (error) {
    throw new Error(searchParams.get("error_description") || `Login failed: ${error}`);
  }

  const expectedState = sessionStorage.getItem(STATE_KEY);
  const codeVerifier = sessionStorage.getItem(CODE_VERIFIER_KEY);
  const returnTo = sessionStorage.getItem(RETURN_TO_KEY) || "/";

  sessionStorage.removeItem(STATE_KEY);
  sessionStorage.removeItem(CODE_VERIFIER_KEY);
  sessionStorage.removeItem(RETURN_TO_KEY);

  if (!code || !expectedState || returnedState !== expectedState) {
    throw new Error("Login response was invalid or expired. Please try signing in again.");
  }

  const tokens = await exchangeCodeForTokens(code, codeVerifier);
  return { tokens, returnTo };
}

async function exchangeCodeForTokens(code, codeVerifier) {
  return requestTokenEndpoint({
    grant_type: "authorization_code",
    client_id: config.cognito.clientId,
    code,
    redirect_uri: config.cognito.redirectUri,
    code_verifier: codeVerifier,
  });
}

export async function refreshTokens(refreshToken) {
  return requestTokenEndpoint({
    grant_type: "refresh_token",
    client_id: config.cognito.clientId,
    refresh_token: refreshToken,
  });
}

async function requestTokenEndpoint(params) {
  const response = await fetch(`https://${config.cognito.domain}/oauth2/token`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams(params),
  });

  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.error_description || "Could not complete sign-in. Please try again.");
  }

  const data = await response.json();
  return {
    accessToken: data.access_token,
    idToken: data.id_token,
    refreshToken: data.refresh_token, // absent on a refresh-grant response - caller keeps the existing one
    expiresInSeconds: data.expires_in,
  };
}

export function redirectToLogout() {
  const url = new URL(`https://${config.cognito.domain}/logout`);
  url.searchParams.set("client_id", config.cognito.clientId);
  url.searchParams.set("logout_uri", config.cognito.logoutRedirectUri);
  window.location.assign(url.toString());
}

/**
 * Best-effort server-side revocation of the refresh token. This is
 * separate from (and doesn't replace) {@link redirectToLogout}, which is
 * what actually ends Cognito's Hosted UI SSO session cookie - revocation
 * only guarantees the specific refresh token this app was holding can
 * never be exchanged again, even if a copy of it somehow persisted
 * somewhere. Never throws: a failed revoke must never block the user
 * from completing logout.
 *
 * Uses sendBeacon rather than a plain fetch because this is always
 * called immediately before navigating away (see AuthContext's logout) -
 * a normal in-flight fetch risks being cancelled by that navigation;
 * sendBeacon (and the fetch keepalive fallback below) are specifically
 * designed to survive it.
 */
export function revokeToken(refreshToken) {
  if (!refreshToken) return;

  const body = new URLSearchParams({ token: refreshToken, client_id: config.cognito.clientId }).toString();
  const url = `https://${config.cognito.domain}/oauth2/revoke`;

  const blob = new Blob([body], { type: "application/x-www-form-urlencoded" });
  const queued = navigator.sendBeacon?.(url, blob);

  if (!queued) {
    // sendBeacon unavailable or its queue rejected the payload - fall
    // back to a keepalive fetch, which the browser also protects from
    // being aborted by the page navigation that follows immediately.
    fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body,
      keepalive: true,
    }).catch(() => {});
  }
}
