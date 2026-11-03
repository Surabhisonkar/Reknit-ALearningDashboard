/**
 * The landing page showcase topic, written the way a good model answer would
 * be, in the exact JSON the backend returns for each visualization type.
 * Plain data with no imports, so scripts can load it too (for example to
 * render the animation through animation-renderer).
 */

export const NOTE = {
  title: "The Water Cycle",
  points: [
    "The sun heats the sea, and water rises as vapour",
    "High up, the vapour cools into clouds",
    "Clouds get heavy and fall as rain or snow",
    "Rivers carry the water back to the sea",
  ],
};

export const SHOWCASE_IMAGE_ID = "showcase-water-cycle";

export const DIAGRAM = {
  type: "diagram",
  version: 2,
  syntax: "mermaid",
  caption: "The same water keeps going round: up as vapour, down as rain, back to the sea.",
  source: `flowchart TB
  evap["☀️ Evaporation<br/>the sun turns sea water into vapour"] -->|"warm air rises"| cond["☁️ Condensation<br/>vapour cools into clouds"]
  cond -->|"droplets grow heavy"| prec["🌧️ Precipitation<br/>rain, snow or hail falls"]
  prec -->|"runs downhill"| coll["🏞️ Collection<br/>rivers carry it to the sea"]
  coll -->|"the sun heats it again"| evap`,
};

export const MIND_MAP = {
  type: "mind_map",
  version: 2,
  rootLabel: "The Water Cycle",
  layoutHints: { orientation: "horizontal", rootNodeId: "root" },
  nodes: [
    { id: "root", label: "The Water Cycle", detail: "How the same water moves between sea, sky and land, again and again.", parentId: null },
    { id: "evap", label: "Evaporation", detail: "The sun warms water and it rises as invisible vapour.", parentId: "root" },
    { id: "evap-sun", label: "Powered by the sun", detail: "", parentId: "evap" },
    { id: "evap-plants", label: "Plants breathe out water too", detail: "This is called transpiration.", parentId: "evap" },
    { id: "cond", label: "Condensation", detail: "Rising vapour cools and turns into tiny droplets: clouds.", parentId: "root" },
    { id: "cond-cool", label: "Air is colder higher up", detail: "", parentId: "cond" },
    { id: "cond-mirror", label: "Like a steamy mirror", detail: "", parentId: "cond" },
    { id: "prec", label: "Precipitation", detail: "Droplets join, get heavy and fall.", parentId: "root" },
    { id: "prec-rain", label: "Rain", detail: "", parentId: "prec" },
    { id: "prec-snow", label: "Snow and hail when it's cold", detail: "", parentId: "prec" },
    { id: "coll", label: "Collection", detail: "Water gathers and flows back to the sea.", parentId: "root" },
    { id: "coll-rivers", label: "Rivers and lakes", detail: "", parentId: "coll" },
    { id: "coll-ground", label: "Soaks into the ground", detail: "", parentId: "coll" },
  ],
  edges: [{ sourceId: "coll", targetId: "evap", relationshipLabel: "starts again" }],
  citations: [],
};

export const IMAGE = {
  type: "image",
  version: 2,
  artifactId: SHOWCASE_IMAGE_ID,
  imagePrompt:
    "A friendly flat illustration of the water cycle: sun over the sea, vapour rising, clouds drifting inland, rain over a snowy mountain, a river flowing back to the sea. No text.",
  altText: "The sun warms the sea, vapour rises into clouds, rain falls on a mountain and a river carries it back to the sea.",
  labels: [
    { id: "l1", text: "Sun", explanation: "Gives the energy that keeps the whole cycle moving.", x: 0.13, y: 0.15 },
    { id: "l2", text: "Evaporation", explanation: "Warmed sea water rises as invisible vapour.", x: 0.23, y: 0.6 },
    { id: "l3", text: "Condensation", explanation: "High up the air is cold, so vapour turns into cloud droplets.", x: 0.53, y: 0.2 },
    { id: "l4", text: "Precipitation", explanation: "Heavy droplets fall as rain, or as snow on the mountain top.", x: 0.71, y: 0.42 },
    { id: "l5", text: "Collection", explanation: "Rivers carry the water downhill and back into the sea.", x: 0.64, y: 0.85 },
  ],
};

const scene = (order, id, title, narration, layout, transitionToNext, visualElements, durationSeconds = 6) => ({
  id,
  order,
  title,
  narration,
  durationSeconds,
  transitionToNext,
  layout,
  assetArtifactIds: [],
  visualElements,
});
const el = (id, kind, text, extra = {}) => ({ id, kind, text, enterAtSeconds: 0, animation: "pop", color: "teal", ...extra });

export const ANIMATION = {
  type: "animation",
  version: 2,
  scenes: [
    scene(1, "s1", "Meet a drop of water", "Every drop on Earth has been round this loop millions of times. Let's follow one.", "focus", "fade", [
      el("drop", "box", "A drop in the sea", { icon: "💧", animation: "highlight", color: "teal" }),
      el("sun", "box", "The sun", { icon: "☀️", enterAtSeconds: 1.4, color: "yellow" }),
      el("sky", "box", "The sky", { icon: "☁️", enterAtSeconds: 2, color: "ink" }),
      el("land", "box", "The land", { icon: "⛰️", enterAtSeconds: 2.6, color: "coral" }),
    ]),
    scene(2, "s2", "Evaporation", "The sun warms the sea. Our drop turns into invisible vapour and floats up.", "steps", "slide", [
      el("sea", "box", "Warm sea", { icon: "🌊", color: "teal" }),
      el("vapour", "box", "Vapour rises", { icon: "♨️", enterAtSeconds: 1.6, color: "yellow" }),
      el("a1", "arrow", "heat", { fromId: "sea", toId: "vapour", enterAtSeconds: 0.8, animation: "draw", color: "coral" }),
    ]),
    scene(3, "s3", "Condensation", "High up the air is cold. The vapour cools and becomes tiny droplets: a cloud.", "before_after", "wipe", [
      el("vap", "box", "Vapour", { icon: "♨️", color: "yellow" }),
      el("cloud", "box", "Cloud", { icon: "☁️", enterAtSeconds: 1.8, animation: "highlight", color: "ink" }),
      el("a2", "arrow", "cools", { fromId: "vap", toId: "cloud", enterAtSeconds: 1, animation: "draw", color: "teal" }),
    ]),
    scene(4, "s4", "Precipitation", "Droplets bump together and grow heavy. Down they fall as rain, or snow if it's cold.", "compare", "zoom", [
      el("rain", "box", "Rain", { icon: "🌧️", color: "teal" }),
      el("snow", "box", "Snow", { icon: "❄️", enterAtSeconds: 1.2, color: "ink" }),
      el("hill", "box", "Lands on hills", { icon: "⛰️", enterAtSeconds: 2.2, color: "coral" }),
      el("field", "box", "and fields", { icon: "🌾", enterAtSeconds: 2.8, color: "yellow" }),
      el("f1", "arrow", "falls", { fromId: "rain", toId: "hill", enterAtSeconds: 3.4, animation: "draw", color: "teal" }),
      el("f2", "arrow", "falls", { fromId: "snow", toId: "field", enterAtSeconds: 3.8, animation: "draw", color: "teal" }),
    ]),
    scene(5, "s5", "Collection, then again", "Rivers carry the water back to the sea, and the whole cycle starts again.", "cycle", "fade", [
      el("c1", "box", "Evaporation", { icon: "☀️", color: "yellow" }),
      el("c2", "box", "Condensation", { icon: "☁️", enterAtSeconds: 0.9, color: "ink" }),
      el("c3", "box", "Precipitation", { icon: "🌧️", enterAtSeconds: 1.8, color: "teal" }),
      el("c4", "box", "Collection", { icon: "🌊", enterAtSeconds: 2.7, color: "coral" }),
      el("r1", "arrow", "", { fromId: "c1", toId: "c2", enterAtSeconds: 0.6, animation: "draw", color: "coral" }),
      el("r2", "arrow", "", { fromId: "c2", toId: "c3", enterAtSeconds: 1.5, animation: "draw", color: "coral" }),
      el("r3", "arrow", "", { fromId: "c3", toId: "c4", enterAtSeconds: 2.4, animation: "draw", color: "coral" }),
      el("r4", "arrow", "", { fromId: "c4", toId: "c1", enterAtSeconds: 3.3, animation: "draw", color: "coral" }),
    ], 7),
  ],
  videoArtifactId: "",
};
