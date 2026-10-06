import { lazy, Suspense, useEffect, useState } from "react";

import { getArtifactDownloadUrl } from "../../../api/artifactsApi.js";
import { useAuth } from "../../../auth";

const DiagramRenderer = lazy(() => import("./DiagramRenderer.jsx"));

function FallbackDiagram({ diagram }) {
  return (
    <Suspense fallback={<div className="image-renderer-placeholder" aria-hidden="true" />}>
      <DiagramRenderer visualization={diagram} />
    </Suspense>
  );
}

/**
 * Consumes the shape produced by domain/visualizationMappers.js's mapImage.
 *
 * An image usually arrives with `fallbackDiagram` - the same idea as a
 * flowchart, written by the AI in the same call. It is used two ways:
 * - automatically, when the picture cannot be shown (no artifact, the
 *   download URL fails, or the image itself fails to load);
 * - on demand, via "Show as diagram" - the manual answer to a picture
 *   that loaded fine but came out wrong or unreadable, which no code can
 *   detect.
 */
function ImageRenderer({ visualization }) {
  const { accessToken } = useAuth();
  const { artifactId, fallbackDiagram, altText } = visualization;
  const [url, setUrl] = useState(null);
  const [error, setError] = useState(null);
  const [showDiagram, setShowDiagram] = useState(false);

  useEffect(() => {
    if (!artifactId) return undefined;
    let cancelled = false;
    getArtifactDownloadUrl(accessToken, artifactId)
      .then((res) => !cancelled && setUrl(res.downloadUrl))
      .catch((err) => !cancelled && setError(err.message ?? "This image couldn't be loaded."));
    return () => {
      cancelled = true;
    };
  }, [accessToken, artifactId]);

  const pictureUnavailable = !artifactId || error !== null;

  if (pictureUnavailable) {
    if (fallbackDiagram) return <FallbackDiagram diagram={fallbackDiagram} />;
    return (
      <p className="image-renderer-error">{error ?? "No image was generated for this concept."}</p>
    );
  }

  return (
    <div className="image-renderer-frame">
      {fallbackDiagram && (
        <button
          type="button"
          className="image-renderer-toggle"
          aria-pressed={showDiagram}
          onClick={() => setShowDiagram((shown) => !shown)}
        >
          {showDiagram ? "Show image" : "Show as diagram"}
        </button>
      )}

      {showDiagram ? (
        <FallbackDiagram diagram={fallbackDiagram} />
      ) : url ? (
        <img
          src={url}
          alt={altText}
          className="image-renderer"
          onError={() => setError("This image couldn't be loaded.")}
        />
      ) : (
        <div className="image-renderer-placeholder" aria-hidden="true" />
      )}
    </div>
  );
}

export default ImageRenderer;
