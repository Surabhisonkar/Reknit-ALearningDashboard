import { useCallback, useEffect, useMemo, useState } from "react";

import { redirectToLogin, redirectToLogout, refreshTokens, revokeToken } from "../api/cognitoAuthClient.js";
import { clearSession, isExpired, loadSession, saveSession } from "../storage/sessionStore.js";
import { AuthContext } from "./authContextObject.js";

// Refresh a bit before actual expiry so an in-flight request never gets
// caught by a token that expires mid-request.
const REFRESH_SKEW_MS = 60_000;

function toStoredSession(tokens) {
  return {
    accessToken: tokens.accessToken,
    idToken: tokens.idToken,
    refreshToken: tokens.refreshToken,
    expiresAtMs: Date.now() + tokens.expiresInSeconds * 1000,
  };
}

export function AuthProvider({ children }) {
  const [session, setSession] = useState(() => loadSession());
  const [isLoading] = useState(false);

  const applySession = useCallback((tokens, previousRefreshToken) => {
    const stored = toStoredSession(tokens);
    if (!stored.refreshToken && previousRefreshToken) {
      // A refresh-grant response doesn't repeat the refresh token - keep the one we already had.
      stored.refreshToken = previousRefreshToken;
    }
    saveSession(stored);
    setSession(stored);
    return stored;
  }, []);

  const login = useCallback((returnTo) => redirectToLogin(returnTo), []);

  const logout = useCallback(() => {
    // Capture before clearing - revocation needs the token that's about
    // to be wiped from storage.
    revokeToken(session?.refreshToken);
    clearSession();
    // Deliberately NOT calling setSession(null) here. Doing so triggers
    // an immediate re-render, and if the user is on a page wrapped in
    // RequireAuth (Library/Create/Workspace - i.e. wherever they'd
    // realistically be when signing out), that re-render flips
    // isAuthenticated to false and fires RequireAuth's OWN redirect-to-
    // login effect: a second, competing window.location.assign() that
    // races the logout redirect below. Since Cognito's SSO session
    // cookie hasn't been cleared yet at that point (the real /logout
    // call hasn't run), that competing login redirect silently
    // re-authenticates the user via the still-active cookie before
    // logout ever gets a chance to complete - which is exactly the "I
    // click Sign out and nothing happens" symptom this fixes.
    // redirectToLogout() below is a full page navigation regardless, so
    // React state is about to be torn down and rebuilt fresh from
    // (now-empty) sessionStorage on reload - there's nothing left for
    // setSession to accomplish here that the navigation doesn't already
    // handle correctly.
    redirectToLogout();
  }, [session]);

  // Proactive refresh loop: while a session exists, refresh it shortly
  // before it expires so the user is never interrupted mid-task.
  useEffect(() => {
    if (!session?.refreshToken) return undefined;

    const msUntilRefresh = Math.max(0, session.expiresAtMs - Date.now() - REFRESH_SKEW_MS);
    const timer = setTimeout(async () => {
      try {
        const tokens = await refreshTokens(session.refreshToken);
        applySession(tokens, session.refreshToken);
      } catch {
        // Refresh token itself expired/revoked - fall back to a real login.
        clearSession();
        setSession(null);
      }
    }, msUntilRefresh);

    return () => clearTimeout(timer);
  }, [session, applySession]);

  const value = useMemo(() => {
    const authenticated = session && !isExpired(session);
    return {
      isAuthenticated: Boolean(authenticated),
      isLoading,
      session: authenticated ? session : null,
      login,
      logout,
      // Used only by the /auth/callback route.
      _applySession: applySession,
    };
  }, [session, isLoading, login, logout, applySession]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
