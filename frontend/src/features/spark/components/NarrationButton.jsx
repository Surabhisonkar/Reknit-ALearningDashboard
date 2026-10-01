import { SvgIcon } from "../../../shared/ui";

/** Mute / unmute the narrator. Hidden when the browser has no voice. */
export default function NarrationButton({ narration }) {
  if (!narration.supported) return null;
  const label = narration.muted ? "Turn narration on" : "Turn narration off";
  return (
    <button type="button" className="spark-icon-button" aria-label={label} title={label} aria-pressed={!narration.muted} onClick={narration.toggleMuted}>
      <SvgIcon name={narration.muted ? "speakerOff" : "speaker"} />
    </button>
  );
}
