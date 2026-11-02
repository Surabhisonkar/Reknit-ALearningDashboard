/**
 * Stand-in for the images the worker would generate and upload to S3. Each
 * artifact is drawn as an SVG in the browser and served from an object URL,
 * so demo mode needs no storage bucket at all.
 */

const MOTIFS = {
  sun: (c) => `
    <circle cx="400" cy="220" r="90" fill="${c.accent}"/>
    ${Array.from({ length: 12 }, (_, i) => {
      const a = (i * Math.PI) / 6;
      return `<line x1="${400 + Math.cos(a) * 120}" y1="${220 + Math.sin(a) * 120}" x2="${400 + Math.cos(a) * 170}" y2="${220 + Math.sin(a) * 170}" stroke="${c.accent}" stroke-width="14" stroke-linecap="round"/>`;
    }).join("")}
    <path d="M0 470 Q200 400 400 450 T800 430 V600 H0Z" fill="${c.ground}"/>`,
  leaf: (c) => `
    <path d="M400 110 C560 160 600 340 400 500 C200 340 240 160 400 110Z" fill="${c.accent}"/>
    <path d="M400 130 V490" stroke="${c.ink}" stroke-width="8" opacity=".45"/>
    ${[200, 280, 360, 430].map((y) => `<path d="M400 ${y} L${470 + (y - 200) / 8} ${y - 50} M400 ${y} L${330 - (y - 200) / 8} ${y - 50}" stroke="${c.ink}" stroke-width="5" opacity=".35"/>`).join("")}`,
  glucose: (c) => `
    <polygon points="400,140 500,200 500,320 400,380 300,320 300,200" fill="none" stroke="${c.accent}" stroke-width="16"/>
    ${[[400, 140], [500, 200], [500, 320], [400, 380], [300, 320], [300, 200]].map(([x, y]) => `<circle cx="${x}" cy="${y}" r="22" fill="${c.ink}"/>`).join("")}
    <circle cx="590" cy="140" r="30" fill="${c.accent}" opacity=".6"/><circle cx="210" cy="420" r="40" fill="${c.accent}" opacity=".5"/>`,
  butterfly: (c) => `
    <path d="M400 300 C300 120 120 120 150 260 C170 340 300 330 400 300Z" fill="${c.accent}" stroke="${c.ink}" stroke-width="10"/>
    <path d="M400 300 C500 120 680 120 650 260 C630 340 500 330 400 300Z" fill="${c.accent}" stroke="${c.ink}" stroke-width="10"/>
    <path d="M400 310 C320 360 220 430 280 480 C330 520 380 420 400 310Z" fill="${c.accent2}" stroke="${c.ink}" stroke-width="10"/>
    <path d="M400 310 C480 360 580 430 520 480 C470 520 420 420 400 310Z" fill="${c.accent2}" stroke="${c.ink}" stroke-width="10"/>
    ${[[190, 200], [240, 160], [610, 200], [560, 160], [300, 450], [500, 450]].map(([x, y]) => `<circle cx="${x}" cy="${y}" r="11" fill="#fff"/>`).join("")}
    <rect x="388" y="200" width="24" height="260" rx="12" fill="${c.ink}"/>
    <path d="M396 205 Q370 140 340 120 M404 205 Q430 140 460 120" stroke="${c.ink}" stroke-width="6" fill="none"/>`,
  loop: (c) => `
    <circle cx="400" cy="290" r="170" fill="none" stroke="${c.accent}" stroke-width="22" stroke-dasharray="60 26"/>
    <polygon points="560,200 610,280 520,270" fill="${c.ink}"/>
    <rect x="320" y="250" width="160" height="80" rx="16" fill="${c.ink}"/>`,
  abstract: (c) => `
    <circle cx="260" cy="250" r="130" fill="${c.accent}" opacity=".85"/>
    <rect x="380" y="200" width="260" height="200" rx="28" fill="${c.accent2}" opacity=".85"/>
    <circle cx="520" cy="170" r="50" fill="${c.ink}" opacity=".7"/>`,
};

function palette(hue) {
  return {
    bg: `hsl(${hue} 60% 94%)`,
    ground: `hsl(${(hue + 80) % 360} 45% 62%)`,
    accent: `hsl(${hue} 75% 58%)`,
    accent2: `hsl(${(hue + 30) % 360} 70% 66%)`,
    ink: `hsl(${hue} 35% 22%)`,
  };
}

function escapeXml(text) {
  return String(text).replace(/[<>&"]/g, (ch) => ({ "<": "&lt;", ">": "&gt;", "&": "&amp;", '"': "&quot;" })[ch]);
}

export function renderArtworkSvg({ title, hue = 160, motif = "abstract" }) {
  const c = palette(hue);
  const draw = MOTIFS[motif] ?? MOTIFS.abstract;
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 600" width="800" height="600">
  <rect width="800" height="600" fill="${c.bg}"/>
  ${draw(c)}
  <text x="400" y="565" text-anchor="middle" font-family="system-ui, sans-serif" font-size="30" font-weight="600" fill="${c.ink}">${escapeXml(title)}</text>
</svg>`;
}

const objectUrls = new Map();

/** One object URL per artifact for the life of the page - the "presigned URL" of demo mode. */
export function artifactObjectUrl(id, artifact) {
  if (!objectUrls.has(id)) {
    const blob = new Blob([renderArtworkSvg(artifact)], { type: "image/svg+xml" });
    objectUrls.set(id, URL.createObjectURL(blob));
  }
  return objectUrls.get(id);
}

/** Picks a motif from words in the text, so generated images loosely match the topic. */
export function motifFor(text) {
  const t = text.toLowerCase();
  if (/butterfl|moth|insect|wing/.test(t)) return "butterfly";
  if (/sun|light|solar|energy/.test(t)) return "sun";
  if (/leaf|plant|tree|chloro/.test(t)) return "leaf";
  if (/sugar|glucose|molecule|atom|chemi/.test(t)) return "glucose";
  if (/loop|cycle|queue|event|repeat/.test(t)) return "loop";
  return "abstract";
}

/** Stable hue per string, so the same topic always gets the same colours. */
export function hueFor(text) {
  let hash = 0;
  for (const ch of text) hash = (hash * 31 + ch.charCodeAt(0)) | 0;
  return Math.abs(hash) % 360;
}
