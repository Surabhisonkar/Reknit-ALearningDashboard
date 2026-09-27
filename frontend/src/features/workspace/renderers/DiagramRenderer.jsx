const ELEMENT_COLORS = {
  process: "var(--teal)",
  decision: "var(--yellow)",
  terminator: "var(--coral)",
  data_store: "var(--coral-dark)",
  actor: "var(--muted)",
  note: "var(--line)",
};

/** Consumes the shape produced by domain/visualizationMappers.js's mapDiagram. */
function DiagramRenderer({ visualization }) {
  const { elements, connections } = visualization;
  const elementsById = new Map(elements.map((el) => [el.id, el]));

  const maxX = Math.max(...elements.map((el) => el.x + el.width), 800);
  const maxY = Math.max(...elements.map((el) => el.y + el.height), 600);

  function centerOf(el) {
    return { x: el.x + el.width / 2, y: el.y + el.height / 2 };
  }

  return (
    <div className="diagram-renderer">
      <svg viewBox={`0 0 ${maxX} ${maxY}`} className="diagram-svg" role="img" aria-label="Diagram">
        {connections.map((conn) => {
          const from = elementsById.get(conn.sourceId);
          const to = elementsById.get(conn.targetId);
          if (!from || !to) return null;
          const fromCenter = centerOf(from);
          const toCenter = centerOf(to);
          return (
            <g key={conn.id}>
              <line x1={fromCenter.x} y1={fromCenter.y} x2={toCenter.x} y2={toCenter.y} className="diagram-connection" />
              {conn.label && (
                <text x={(fromCenter.x + toCenter.x) / 2} y={(fromCenter.y + toCenter.y) / 2} className="diagram-connection-label">
                  {conn.label}
                </text>
              )}
            </g>
          );
        })}

        {elements.map((el) => (
          <g key={el.id} role="img" aria-label={el.accessibilityLabel}>
            <rect
              x={el.x} y={el.y} width={el.width} height={el.height}
              rx={el.elementType === "decision" ? 16 : 8}
              className="diagram-element"
              style={{ fill: ELEMENT_COLORS[el.elementType] ?? "var(--paper-deep)" }}
            />
            <text x={el.x + el.width / 2} y={el.y + el.height / 2} className="diagram-element-label">
              {el.label}
            </text>
          </g>
        ))}
      </svg>

      <ul className="diagram-legend" aria-hidden="true">
        {[...new Set(elements.map((el) => el.elementType))].map((type) => (
          <li key={type}>
            <span className="diagram-legend-swatch" style={{ background: ELEMENT_COLORS[type] ?? "var(--paper-deep)" }} />
            {type.replace("_", " ")}
          </li>
        ))}
      </ul>
    </div>
  );
}

export default DiagramRenderer;
