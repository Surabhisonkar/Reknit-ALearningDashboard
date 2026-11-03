import { useContext, useEffect, useState } from "react";

import { getArtifactDownloadUrl } from "../../../api/artifactsApi.js";
import { useAuth } from "../../../auth";
import { ArtifactUrlsContext } from "./artifactUrls.js";

/**
 * Where a label's tag sits relative to its anchor: pushed toward the
 * nearer side edge, so tags spread outward instead of covering the subject.
 */
function tagPlacement(label, size) {
  const toLeft = label.x < 0.5;
  const dx = size.width * 0.16 * (toLeft ? -1 : 1);
  const dy = size.height * (label.y < 0.5 ? -0.06 : 0.06);
  const x = Math.min(size.width * 0.97, Math.max(size.width * 0.03, label.x * size.width + dx));
  return { x, y: label.y * size.height + dy, anchor: toLeft ? "start" : "end" };
}

/**
 * The image itself carries no words (the model is told not to draw any);
 * this overlay draws each label as SVG - a numbered marker on the part, a
 * leader line, and the name - so the text is always exact. Tapping a
 * marker or a legend entry highlights both.
 */
function LabelOverlay({ labels, size, activeId, onSelect }) {
  const unit = size.width / 100;
  return (
    <svg className="illustration-overlay" viewBox={`0 0 ${size.width} ${size.height}`} role="list" aria-label="Labels">
      {labels.map((label, i) => {
        const ax = label.x * size.width;
        const ay = label.y * size.height;
        const tag = tagPlacement(label, size);
        const active = label.id === activeId;
        const fontSize = unit * 2.6;
        const tagWidth = Math.min(size.width * 0.42, (label.text.length * 0.56 + 2) * fontSize);
        const tagX = tag.anchor === "start" ? Math.max(0, tag.x - tagWidth) : Math.min(size.width - tagWidth, tag.x);
        return (
          <g
            key={label.id}
            role="listitem"
            className={`illustration-label ${active ? "illustration-label-active" : ""}`}
            onClick={() => onSelect(active ? null : label.id)}
          >
            <line x1={ax} y1={ay} x2={tag.anchor === "start" ? tagX + tagWidth : tagX} y2={tag.y} strokeWidth={unit * 0.35} />
            <rect x={tagX} y={tag.y - fontSize * 0.95} width={tagWidth} height={fontSize * 1.9} rx={fontSize * 0.95} />
            <text x={tagX + tagWidth / 2} y={tag.y} fontSize={fontSize}>
              {label.text}
            </text>
            <circle cx={ax} cy={ay} r={unit * 2.2} strokeWidth={unit * 0.45} />
            <text x={ax} y={ay} fontSize={unit * 2.4} className="illustration-marker-number">
              {i + 1}
            </text>
          </g>
        );
      })}
    </svg>
  );
}

/** Consumes the shape produced by domain/visualizationMappers.js's mapImage. */
function ImageRenderer({ visualization }) {
  const { accessToken } = useAuth();
  const knownUrl = useContext(ArtifactUrlsContext)[visualization.artifactId];
  const [fetchedUrl, setUrl] = useState(null);
  const url = knownUrl ?? fetchedUrl;
  const [error, setError] = useState(null);
  const [size, setSize] = useState(null);
  const [activeId, setActiveId] = useState(null);
  const { labels } = visualization;

  useEffect(() => {
    if (!visualization.artifactId || knownUrl) return;
    let cancelled = false;
    getArtifactDownloadUrl(accessToken, visualization.artifactId)
      .then((res) => !cancelled && setUrl(res.downloadUrl))
      .catch((err) => !cancelled && setError(err.message));
    return () => {
      cancelled = true;
    };
  }, [accessToken, visualization.artifactId, knownUrl]);

  if (!visualization.artifactId) return <p className="image-renderer-error">No image was generated for this concept.</p>;
  if (error) return <p className="image-renderer-error">{error}</p>;
  if (!url) return <div className="image-renderer-placeholder" aria-hidden="true" />;

  if (labels.length === 0) {
    return <img src={url} alt={visualization.altText} className="image-renderer" />;
  }

  return (
    <div className="illustration">
      <figure className="illustration-frame">
        <img
          src={url}
          alt={visualization.altText}
          className="image-renderer"
          onLoad={(event) => setSize({ width: event.currentTarget.naturalWidth || 1000, height: event.currentTarget.naturalHeight || 1000 })}
        />
        {size && <LabelOverlay labels={labels} size={size} activeId={activeId} onSelect={setActiveId} />}
      </figure>

      <ol className="illustration-legend">
        {labels.map((label) => (
          <li key={label.id} className={label.id === activeId ? "illustration-legend-active" : ""}>
            <button type="button" onClick={() => setActiveId(label.id === activeId ? null : label.id)}>
              <strong>{label.text}</strong>
              <span>{label.explanation}</span>
            </button>
          </li>
        ))}
      </ol>
    </div>
  );
}

export default ImageRenderer;
