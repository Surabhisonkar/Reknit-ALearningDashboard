import { useEffect, useRef, useState } from "react";
import { Link, NavLink, Outlet, useLocation } from "react-router-dom";

import { Icon } from "../shared/ui/Icon";
import { useAuth } from "../auth";
import { config } from "../config/env.js";
import { ErrorBoundary } from "../shared/ErrorBoundary.jsx";

const menuLinks = [
  { to: "/library", label: "Library" },
  { to: "/create", label: "New concept" },
  { to: "/about", label: "About" },
];

const footerLinks = [
  { to: "/privacy", label: "Privacy Policy" },
  { to: "/terms", label: "Terms of Service" },
  { to: "/accessibility", label: "Accessibility" },
  { to: "/support", label: "Contact Support" },
];

/** Closes a popover on an outside click or Escape. */
function useDismiss(ref, setOpen) {
  useEffect(() => {
    function handleClickOutside(event) {
      if (ref.current && !ref.current.contains(event.target)) {
        setOpen(false);
      }
    }
    function handleEscape(event) {
      if (event.key === "Escape") setOpen(false);
    }
    document.addEventListener("mousedown", handleClickOutside);
    document.addEventListener("keydown", handleEscape);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      document.removeEventListener("keydown", handleEscape);
    };
  }, [ref, setOpen]);
}

function HamburgerMenu() {
  const [open, setOpen] = useState(false);
  const menuRef = useRef(null);
  const { isAuthenticated, login, logout } = useAuth();
  useDismiss(menuRef, setOpen);

  return (
    <div className="relative" ref={menuRef}>
      <button
        type="button"
        aria-label="Open menu"
        aria-expanded={open}
        onClick={() => setOpen((prev) => !prev)}
        className="menu-button"
      >
        <Icon>{open ? "×" : "☰"}</Icon>
      </button>

      {open && (
        <nav className="nav-popover">
          {menuLinks.map((link) => (
            <NavLink key={link.to} to={link.to} onClick={() => setOpen(false)}>
              {link.label}
            </NavLink>
          ))}
          <hr className="nav-popover-rule" />
          <button
            type="button"
            className="nav-popover-action"
            onClick={() => {
              setOpen(false);
              if (isAuthenticated) logout();
              else login("/library");
            }}
          >
            {isAuthenticated ? "Sign out" : "Sign in"}
          </button>
        </nav>
      )}
    </div>
  );
}

/**
 * Signed out: a "Log in" / "Get started" pill. Signed in: the user's name,
 * which opens a small menu with Sign out. The demo build is always signed in
 * as the demo user, so there the pill says so and Get started opens /create.
 */
function AccountControl() {
  const { isAuthenticated, user, login, signUp, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const ref = useRef(null);
  useDismiss(ref, setOpen);

  if (config.demoMode && isAuthenticated) {
    return (
      <div className="auth-pill">
        <span className="auth-pill-label">Demo</span>
        <Link className="auth-pill-primary" to="/create">
          Get started
        </Link>
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <div className="auth-pill">
        <button type="button" className="auth-pill-secondary" onClick={() => login("/library")}>
          Log in
        </button>
        <button type="button" className="auth-pill-primary" onClick={() => signUp("/create")}>
          Get started
        </button>
      </div>
    );
  }

  const name = user?.name || user?.email || "Account";
  return (
    <div className="relative" ref={ref}>
      <button
        type="button"
        className="account-control"
        aria-haspopup="menu"
        aria-expanded={open}
        title={user?.email}
        onClick={() => setOpen((o) => !o)}
      >
        <span className="account-avatar" aria-hidden="true">
          {name.charAt(0).toUpperCase()}
        </span>
        <span className="account-name">{name}</span>
      </button>
      {open && (
        <div className="nav-popover" role="menu">
          <button
            type="button"
            role="menuitem"
            className="nav-popover-action"
            onClick={() => {
              setOpen(false);
              logout();
            }}
          >
            Sign out
          </button>
        </div>
      )}
    </div>
  );
}

/** Only in demo builds: says what's simulated and offers a way back to the starting library. */
function DemoBanner() {
  async function handleReset() {
    // Same module instance the mock API uses, loaded on demand like the mocks themselves.
    const { resetDemoData } = await import("../mocks/db.js");
    resetDemoData();
    window.location.reload();
  }

  return (
    <div className="demo-banner" role="note">
      <span>
        <strong>Live demo.</strong> No backend here: AI responses are simulated, and your changes are saved only in this browser.
      </span>
      <button type="button" onClick={handleReset}>
        Reset demo
      </button>
    </div>
  );
}

function Layout() {
  const location = useLocation();
  return (
    <div className="app-shell">
      <a href="#main" className="skip-link">
        Skip to content
      </a>

      {config.demoMode && <DemoBanner />}

      <header className="topbar">
        <Link className="brand" to="/" aria-label="Reknit home">
          Reknit
        </Link>
        <div className="topbar-actions">
          <AccountControl />
          <HamburgerMenu />
          <Link className="spark-button" to="/spark" aria-label="Spark — revisit your concepts">
            <Icon> ⚡︎ </Icon>
          </Link>
        </div>
      </header>

      <main id="main">
        {/* Last safety net: a page that throws shows a message with the header still usable. Keyed by
            path so navigating away clears it. */}
        <ErrorBoundary
          key={location.pathname}
          fallback={
            <div className="page-width page-error" role="alert">
              <h1>Something went wrong on this page.</h1>
              <p>Reloading usually fixes it.</p>
              <button type="button" className="button" onClick={() => window.location.reload()}>
                Reload
              </button>
            </div>
          }
        >
          <Outlet />
        </ErrorBoundary>
      </main>

      <footer className="site-footer">
        <span className="footer-brand">Reknit</span>
        <span>&copy; {new Date().getFullYear()} Reknit. Built for clear minds.</span>
        <div className="footer-links">
          {footerLinks.map((link) => (
            <Link key={link.to} to={link.to}>
              {link.label}
            </Link>
          ))}
        </div>
      </footer>
    </div>
  );
}

export default Layout;
