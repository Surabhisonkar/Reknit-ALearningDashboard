import { useEffect } from "react";
import { useLocation } from "react-router-dom";

import { useAuth } from "./hooks/useAuth.js";

/** Wrap any page element that requires a signed-in user: {@code <RequireAuth><CapturePage /></RequireAuth>}. */
function RequireAuth({ children }) {
  const { isAuthenticated, login } = useAuth();
  const location = useLocation();

  useEffect(() => {
    if (!isAuthenticated) {
      login(location.pathname + location.search);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- only re-check when auth state itself changes
  }, [isAuthenticated]);

  if (!isAuthenticated) {
    return (
      <main className="page-width" style={{ padding: "4rem 0", textAlign: "center" }}>
        <p>Redirecting you to sign in...</p>
      </main>
    );
  }

  return children;
}

export default RequireAuth;
