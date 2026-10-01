/**
 * The Spark feed's mix, in percent of all cards (user decision, Phase 8).
 * Images and diagrams share one "still" bucket. Games are inserted by the
 * frontend; the concept-type shares are applied by the backend's spark-feed
 * query, which reads the same numbers from app.spark.mix (application.yml).
 */
export const FEED_MIX = Object.freeze({
  game: 20,
  animation: 60,
  mind_map: 15,
  still: 5,
});

/** Chance of a game slot after each concept, so games end up ~FEED_MIX.game % of all cards. */
export const GAME_CHANCE_AFTER_CONCEPT = FEED_MIX.game / (100 - FEED_MIX.game);
