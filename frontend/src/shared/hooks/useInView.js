import { useEffect, useRef, useState } from "react";

/**
 * Tracks whether the returned ref's element is substantially visible in
 * its scroll container, plus how many times it has entered. Spark's reel
 * uses `enterCount` (rather than deriving "have I entered before" from a
 * ref mutated during render, which this project's lint config forbids)
 * to know when a slide should restart/remount: it's plain state,
 * incremented from inside the IntersectionObserver's own callback, which
 * is exactly the "external system" callback the state-in-effect rule
 * expects setState to live in.
 */
export function useInView({ threshold = 0.6 } = {}) {
  const ref = useRef(null);
  const [inView, setInView] = useState(false);
  const [enterCount, setEnterCount] = useState(0);

  useEffect(() => {
    const node = ref.current;
    if (!node || typeof IntersectionObserver === "undefined") {
      setInView(true);
      setEnterCount((count) => (count === 0 ? 1 : count));
      return undefined;
    }

    const observer = new IntersectionObserver(
      ([entry]) => {
        setInView(entry.isIntersecting);
        if (entry.isIntersecting) {
          setEnterCount((count) => count + 1);
        }
      },
      { threshold }
    );
    observer.observe(node);
    return () => observer.disconnect();
  }, [threshold]);

  return { ref, inView, enterCount };
}
