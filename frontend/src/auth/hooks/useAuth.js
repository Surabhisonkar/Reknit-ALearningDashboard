import { useContext, useMemo } from "react";

import { AuthContext } from "../context/authContextObject.js";
import { decodeIdTokenClaims } from "../storage/sessionStore.js";

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within an AuthProvider");

  const { session } = ctx;

  return useMemo(() => {
    const claims = session ? decodeIdTokenClaims(session.idToken) : null;
    return {
      isAuthenticated: ctx.isAuthenticated,
      isLoading: ctx.isLoading,
      accessToken: session?.accessToken ?? null,
      user: claims ? { email: claims.email, name: claims.name } : null,
      login: ctx.login,
      logout: ctx.logout,
      _applySession: ctx._applySession,
    };
  }, [ctx, session]);
}
