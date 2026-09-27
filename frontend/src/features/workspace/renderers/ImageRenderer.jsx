import { useEffect, useState } from "react";

import { getArtifactDownloadUrl } from "../../../api/artifactsApi.js";
import { useAuth } from "../../../auth";

/** Consumes the shape produced by domain/visualizationMappers.js's mapImage. */
function ImageRenderer({ visualization }) {
  const { accessToken } = useAuth();
  const [url, setUrl] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!visualization.artifactId) return;
    let cancelled = false;
    getArtifactDownloadUrl(accessToken, visualization.artifactId)
      .then((res) => !cancelled && setUrl(res.downloadUrl))
      .catch((err) => !cancelled && setError(err.message));
    return () => {
      cancelled = true;
    };
  }, [accessToken, visualization.artifactId]);

  if (!visualization.artifactId) return <p className="image-renderer-error">No image was generated for this concept.</p>;
  if (error) return <p className="image-renderer-error">{error}</p>;
  if (!url) return <div className="image-renderer-placeholder" aria-hidden="true" />;

  return <img src={url} alt={visualization.altText} className="image-renderer" />;
}

export default ImageRenderer;
