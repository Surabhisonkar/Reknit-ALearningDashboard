import { useEffect, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";

import { handleLoginCallback, decodeIdTokenClaims, useAuth } from "../auth";
import { updateProfile } from "../api/accountApi.js";

function AuthCallbackPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { _applySession } = useAuth();
  const [error, setError] = useState(null);

  // React 18/19 StrictMode intentionally runs effects twice in
  // development (mount -> cleanup -> mount again) to surface exactly
  // this kind of bug. The token exchange below must run exactly once:
  // it deletes the one-time PKCE verifier/state from sessionStorage and
  // trades a single-use authorization code for tokens - a second
  // invocation would find that data already gone (or the code already
  // consumed) and fail. This ref is what guarantees that: once set, no
  // later invocation of this effect starts a second exchange.
  //
  // Deliberately NOT using a "cancelled" flag alongside this ref: doing
  // so would let StrictMode's synthetic cleanup (step 2 of its double-
  // invoke) mark the *first*, real invocation's in-flight promise as
  // cancelled, so its eventually-successful result gets silently
  // discarded - the app would hang on "Signing you in..." forever with
  // no error. hasRun alone is enough; there is nothing else to guard.
  const hasRun = useRef(false);

  useEffect(() => {
    if (hasRun.current) return;
    hasRun.current = true;

    handleLoginCallback(searchParams)
      .then(({ tokens, returnTo }) => {
        _applySession(tokens);
        navigate(returnTo, { replace: true });

        // Fire-and-forget: sync display profile from the ID token this
        // page already has, since the access token used for every other
        // call doesn't reliably carry email/name. Never blocks
        // navigation and never affects auth if it fails.
        const claims = decodeIdTokenClaims(tokens.idToken);
        if (claims.email || claims.name) {
          updateProfile(tokens.accessToken, { email: claims.email, displayName: claims.name }).catch(() => {});
        }
      })
      .catch((err) => {
        setError(err.message);
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps -- must run exactly once per mount, guarded above
  }, []);

  if (error) {
    return (
      <main className="page-width" style={{ padding: "4rem 0", textAlign: "center" }}>
        <h1>Sign-in failed</h1>
        <p>{error}</p>
        <a href="/">Return home</a>
      </main>
    );
  }

  return (
    <main className="page-width" style={{ padding: "4rem 0", textAlign: "center" }}>
      <p>Signing you in...</p>
    </main>
  );
}

export default AuthCallbackPage;
