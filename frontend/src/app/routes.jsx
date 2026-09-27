import {
  LandingPage,
  WorkspacePage,
  AboutPage,
  CapturePage,
  SparkPage,
  LibraryPage,
  AuthCallbackPage,
} from "../pages";
import { RequireAuth } from "../auth";

export const APP_ROUTES = [
  { path: "/", element: <LandingPage /> },
  { path: "/about", element: <AboutPage /> },
  { path: "/auth/callback", element: <AuthCallbackPage /> },

  // These touch real, per-user data via the backend - everything below needs a signed-in user.
  // Spark moved here now that it plays back your real saved concepts
  // (GET /api/concepts/spark-feed) instead of being a static mockup.
  { path: "/spark", element: <RequireAuth><SparkPage /></RequireAuth> },
  { path: "/library", element: <RequireAuth><LibraryPage /></RequireAuth> },
  { path: "/workspace", element: <RequireAuth><WorkspacePage /></RequireAuth> },
  { path: "/create", element: <RequireAuth><CapturePage /></RequireAuth> },
];
