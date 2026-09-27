/**
 * All runtime config comes from Vite env vars (`VITE_*`), never hardcoded.
 * This is the one place that reads `import.meta.env` - everything else
 * imports from here, so there's a single point to check when config is
 * wrong instead of hunting through the codebase for stray `import.meta.env`
 * reads.
 */
function required(name) {
  const value = import.meta.env[name];
  if (!value) {
    // Fails fast and loud at startup rather than producing a confusing
    // runtime error deep inside a fetch call later.
    throw new Error(
      `Missing required environment variable ${name}. Check your .env file (see .env.example).`
    );
  }
  return value;
}

export const config = {
  apiBaseUrl: required("VITE_API_BASE_URL"),

  cognito: {
    domain: required("VITE_COGNITO_DOMAIN"), // e.g. your-app.auth.us-east-1.amazoncognito.com
    clientId: required("VITE_COGNITO_CLIENT_ID"),
    redirectUri: required("VITE_COGNITO_REDIRECT_URI"), // e.g. http://localhost:5173/auth/callback
    logoutRedirectUri: required("VITE_COGNITO_LOGOUT_REDIRECT_URI"), // e.g. http://localhost:5173/
  },
};
