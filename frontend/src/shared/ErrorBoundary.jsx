import { Component } from "react";

/**
 * Class component is required here - React has no hook-based error
 * boundary API as of this version, `componentDidCatch`/
 * `getDerivedStateFromError` are the only way to catch render errors.
 * Wraps anything rendering dynamic, AI-generated content (visualization
 * renderers, the explanation renderer) so a malformed/unexpected shape
 * that slips past backend validation degrades to an inline message
 * instead of crashing the surrounding page.
 */
export class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error, info) {
    console.error("ErrorBoundary caught a render error:", error, info);
  }

  render() {
    if (this.state.hasError) {
      return this.props.fallback ?? <p className="error-boundary-fallback">Something didn't render correctly here.</p>;
    }
    return this.props.children;
  }
}
