import { useEffect, useRef } from "react";

const SWIPE_PX = 50;
const WHEEL_PX = 60;
const WHEEL_LOCK_MS = 650;
/** Controls inside these keep their own taps, drags and scrolling. */
const IGNORE = "[data-gesture-ignore], input, textarea, select, [role=dialog]";

/**
 * Reels-style gestures on `ref`: swipe up/down/left/right (touch or mouse
 * drag) and the mouse wheel for up/down. A swipe cancels the click that the
 * browser would fire at the end of it, so swiping never also "taps".
 */
export function useSwipeGestures(ref, handlers) {
  const latest = useRef(handlers);
  useEffect(() => {
    latest.current = handlers;
  });

  useEffect(() => {
    const el = ref.current;
    if (!el) return undefined;
    let start = null;
    let suppressClick = false;
    let wheelTotal = 0;
    let wheelLockedUntil = 0;

    function onPointerDown(event) {
      // A new gesture: forget any click-cancel left over from the last swipe
      // (when a swipe ends on a different element, the browser never fires
      // that click, and the flag would otherwise swallow this tap).
      suppressClick = false;
      if (event.target.closest(IGNORE)) return;
      start = { x: event.clientX, y: event.clientY };
    }
    function onPointerUp(event) {
      if (!start) return;
      const dx = event.clientX - start.x;
      const dy = event.clientY - start.y;
      start = null;
      const h = latest.current;
      if (Math.abs(dx) < SWIPE_PX && Math.abs(dy) < SWIPE_PX) return;
      suppressClick = true;
      if (Math.abs(dx) > Math.abs(dy)) (dx < 0 ? h.onSwipeLeft : h.onSwipeRight)?.();
      else (dy < 0 ? h.onSwipeUp : h.onSwipeDown)?.();
    }
    function onClickCapture(event) {
      if (suppressClick) {
        event.stopPropagation();
        event.preventDefault();
        suppressClick = false;
      }
    }
    function onWheel(event) {
      if (event.target.closest(IGNORE)) return;
      const now = Date.now();
      if (now < wheelLockedUntil) return;
      wheelTotal += event.deltaY;
      if (Math.abs(wheelTotal) < WHEEL_PX) return;
      const h = latest.current;
      (wheelTotal > 0 ? h.onSwipeUp : h.onSwipeDown)?.();
      wheelTotal = 0;
      wheelLockedUntil = now + WHEEL_LOCK_MS;
    }

    el.addEventListener("pointerdown", onPointerDown);
    el.addEventListener("pointerup", onPointerUp);
    el.addEventListener("click", onClickCapture, true);
    el.addEventListener("wheel", onWheel, { passive: true });
    return () => {
      el.removeEventListener("pointerdown", onPointerDown);
      el.removeEventListener("pointerup", onPointerUp);
      el.removeEventListener("click", onClickCapture, true);
      el.removeEventListener("wheel", onWheel);
    };
  }, [ref]);
}
