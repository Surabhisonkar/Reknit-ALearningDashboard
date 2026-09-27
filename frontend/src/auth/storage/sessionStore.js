const SESSION_KEY = "auth_session";

/**
 * sessionStorage, not localStorage: the session should not silently
 * persist forever across browser restarts on a shared machine. This is
 * a deliberate choice distinct from (and unrelated to) the "no user data
 * in localStorage" rule for concepts - that rule is about not being a
 * substitute backend; this is a security/session-lifetime choice.
 */
export function loadSession() {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function saveSession(session) {
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

export function clearSession() {
  sessionStorage.removeItem(SESSION_KEY);
}

export function isExpired(session) {
  return !session || Date.now() >= session.expiresAtMs;
}

/** Decoded for display only (name/email) - the backend is what actually verifies the token's signature. */
export function decodeIdTokenClaims(idToken) {
  try {
    const payload = idToken.split(".")[1];
    const json = atob(payload.replace(/-/g, "+").replace(/_/g, "/"));
    return JSON.parse(json);
  } catch {
    return {};
  }
}
