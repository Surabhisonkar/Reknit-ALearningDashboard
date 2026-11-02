/**
 * Demo-mode stand-in for the Cognito Hosted UI: a session for a fixed demo
 * user, made on the spot. The tokens are unsigned placeholders - in demo
 * mode no request ever leaves the browser (MSW answers it), so nothing
 * checks them. The ID token is shaped like a JWT only so
 * decodeIdTokenClaims can read the display name from it as usual.
 */
const DEMO_USER = { name: "Demo Learner", email: "demo@example.com" };

export function createDemoSession() {
  const payload = btoa(JSON.stringify(DEMO_USER));
  return {
    accessToken: "demo-access-token",
    idToken: `demo.${payload}.unsigned`,
    refreshToken: null, // nothing to refresh - the proactive refresh loop stays idle
    expiresAtMs: Date.now() + 365 * 86_400_000,
  };
}
