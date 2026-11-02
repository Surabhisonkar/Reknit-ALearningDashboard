import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./app/App";
import { AuthProvider } from "./auth";
import { config } from "./config/env.js";
import "./index.css";

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
