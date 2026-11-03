import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./app/App";
import { AuthProvider } from "./auth";
import { config } from "./config/env.js";
import "./index.css";

/*
 * After a deploy the lazily loaded files (mind maps, animations, Mermaid)
 * get new names and the old ones are deleted, so a page loaded before the
 * deploy can't fetch them. Reload once to pick up the new version; the
 * session flag stops a reload loop if the failure is something else, and
 * the renderer's error boundary then shows a message instead.
 */
const RELOAD_FLAG = "reknit-reloaded-for-new-version";
window.addEventListener("vite:preloadError", (event) => {
  try {
    if (sessionStorage.getItem(RELOAD_FLAG)) return;
    sessionStorage.setItem(RELOAD_FLAG, "1");
  } catch {
    return; // storage blocked: no loop guard, so don't risk reloading
  }
  event.preventDefault();
  window.location.reload();
});
// Once the page has run a while, a later deploy may trigger one reload again.
setTimeout(() => {
  try {
    sessionStorage.removeItem(RELOAD_FLAG);
  } catch {
    // storage blocked - nothing was set
  }
}, 15000);

async function render() {
  const rootElement = document.getElementById("root");
  if (!rootElement) return;

  if (config.demoMode) {
    // Loaded on demand, so the mocks never ship in the real build's main bundle.
    const { startDemoApi } = await import("./mocks/browser.js");
    await startDemoApi();
  }

  ReactDOM.createRoot(rootElement).render(
    <React.StrictMode>
      <BrowserRouter basename={config.basePath}>
        <AuthProvider>
          <App />
        </AuthProvider>
      </BrowserRouter>
    </React.StrictMode>
  );
}

render();
