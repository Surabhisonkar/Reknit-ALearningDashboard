/**
 * PKCE (Proof Key for Code Exchange, RFC 7636) for the Cognito Hosted UI
 * Authorization Code flow. Hand-rolled with the Web Crypto API rather
 * than pulling in AWS Amplify - the Hosted UI redirect flow itself is
 * simple enough that a full auth SDK is more dependency weight than this
 * app needs.
 */

function base64UrlEncode(bytes) {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

export function generateCodeVerifier() {
  const bytes = new Uint8Array(32);
  crypto.getRandomValues(bytes);
  return base64UrlEncode(bytes);
}

export async function deriveCodeChallenge(codeVerifier) {
  const data = new TextEncoder().encode(codeVerifier);
  const digest = await crypto.subtle.digest("SHA-256", data);
  return base64UrlEncode(new Uint8Array(digest));
}

export function generateState() {
  const bytes = new Uint8Array(16);
  crypto.getRandomValues(bytes);
  return base64UrlEncode(bytes);
}
