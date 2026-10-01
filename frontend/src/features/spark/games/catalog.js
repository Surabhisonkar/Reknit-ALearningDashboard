import { GameRegistry } from "./GameRegistry.js";
import QuickMatchGame from "./QuickMatchGame.jsx";
import TapDashGame from "./TapDashGame.jsx";

/** Every game in Spark. Both are placeholders until the real games are chosen (roadmap Phase 13). */
export const gameRegistry = new GameRegistry()
  .register({ id: "quick-match", label: "Quick Match", Component: QuickMatchGame })
  .register({ id: "tap-dash", label: "Tap Dash", Component: TapDashGame });
