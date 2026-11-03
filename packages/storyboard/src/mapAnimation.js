/**
 * Maps the backend's animation payload (generation.model.AnimationPayload)
 * into the scene shape StoryboardVideo draws. The frontend's
 * visualizationMappers and the render service both use it, so the video and
 * the live preview are built from the same data.
 */
export function mapAnimation(json) {
  const scenes = [...(json.scenes ?? [])].sort((a, b) => a.order - b.order);
  return {
    type: "animation",
    version: json.version,
    scenes: scenes.map((s) => ({
      id: s.id,
      order: s.order,
      title: s.title,
      narration: s.narration,
      durationSeconds: s.durationSeconds ?? 5,
      transition: s.transitionToNext ?? "none",
      assetArtifactIds: s.assetArtifactIds ?? [],
      layout: s.layout ?? "free",
      elements: (s.visualElements ?? []).map((el) => ({
        id: el.id,
        kind: el.kind,
        text: el.text ?? "",
        icon: el.icon ?? "",
        x: el.x ?? 50,
        y: el.y ?? 50,
        enterAt: el.enterAtSeconds ?? 0,
        animation: el.animation ?? "fade",
        color: el.color ?? "ink",
        fromId: el.fromId ?? null,
        toId: el.toId ?? null,
      })),
    })),
    videoArtifactId: json.videoArtifactId || null,
    totalDurationSeconds: scenes.reduce((sum, s) => sum + (s.durationSeconds ?? 5), 0),
  };
}
