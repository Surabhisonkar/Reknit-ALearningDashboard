import { setupWorker } from "msw/browser";

import { config } from "../config/env.js";
import { handlers } from "./handlers.js";

/** Starts intercepting /api calls. Only ever imported when VITE_DEMO_MODE=true (see main.jsx). */
export function startDemoApi() {
  return setupWorker(...handlers).start({
    serviceWorker: { url: `${config.basePath}mockServiceWorker.js` },
    onUnhandledRequest: "bypass", // fonts, images and the app's own assets go to the network as usual
    quiet: true,
  });
}
