import { useEffect, useRef, useState } from "react";
import { Link, NavLink, Outlet } from "react-router-dom";

import { Icon } from "../shared/ui/Icon";
import { useAuth } from "../auth";

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

function HamburgerMenu() {
  const [open, setOpen] = useState(false);
  const menuRef = useRef(null);

  useEffect(() => {
    function handleClickOutside(event) {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
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
  }, []);

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
        </nav>
      )}
    </div>
  );
}

function AccountControl() {
  const { isAuthenticated, user, login, logout } = useAuth();

  if (!isAuthenticated) {
    return (
      <button type="button" className="account-control" onClick={() => login("/library")}>
        Sign in
      </button>
    );
  }

  return (
    <button type="button" className="account-control" onClick={logout} title={user?.email}>
      {user?.name || user?.email || "Account"} · Sign out
    </button>
  );
}

function Layout() {
  return (
    <div className="app-shell">
      <a href="#main" className="skip-link">
        Skip to content
      </a>

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
        <Outlet />
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
