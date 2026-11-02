import { hueFor, motifFor } from "./artwork.js";

/**
 * What the worker's model calls return, faked in the browser. Output is
 * built from the visitor's own topic and notes, in exactly the payload
 * shapes the real pipeline validates, so the renderers can't tell the
 * difference. Nothing here pretends to be a model: the demo banner says
 * responses are simulated.
 */

const STOP_WORDS = new Set(
  "a an and are as at be but by can do does for from has have how in into is it its of on or so that the their then there these this to was what when where which while who why will with you your".split(" ")
);

export function keywords(text) {
  return (text ?? "")
    .toLowerCase()
    .split(/[^a-z0-9]+/)
    .filter((w) => w.length > 2 && !STOP_WORDS.has(w));
}

function sentences(text) {
  return (text ?? "")
    .split(/(?<=[.!?])\s+|\n+/)
    .map((s) => s.replace(/^[-*\d.)\s]+/, "").trim())
    // Drop headings ("The core idea:") and the demo's own disclaimer when an explanation is fed back in.
    .filter((s) => s.length > 8 && !s.endsWith(":") && !/demo runs without/i.test(s));
}

function titleCase(text) {
  return text.replace(/\s+/g, " ").trim().replace(/\b\w/g, (ch) => ch.toUpperCase());
}

/** First line, or the first few words, as a title. */
export function titleFrom(text) {
  const firstLine = (text ?? "").split("\n").find((l) => l.trim()) ?? "Untitled concept";
  const words = firstLine.replace(/[.:!?]+$/, "").trim().split(/\s+/);
  return titleCase(words.length <= 7 ? words.join(" ") : words.slice(0, 6).join(" "));
}

/** A node label from a sentence: its opening words up to the first comma, without leading articles or trailing filler. */
function shortLabel(sentence) {
  const words = sentence.split(",")[0].replace(/[.!?]+$/, "").split(/\s+/);
  while (words.length > 1 && /^(the|a|an)$/i.test(words[0])) words.shift();
  const picked = words.slice(0, 4);
  while (picked.length > 1 && STOP_WORDS.has(picked.at(-1).toLowerCase())) picked.pop();
  const label = picked.join(" ");
  return label.charAt(0).toUpperCase() + label.slice(1);
}

/** The facets a concept is broken into: the visitor's own sentences, or generic study prompts when there are too few. */
function facets(topic, text) {
  const own = sentences(text).filter((s) => s.toLowerCase() !== topic.toLowerCase());
  if (own.length >= 3) return own.slice(0, 6).map((s) => ({ label: shortLabel(s), detail: s }));
  return [
    { label: "Core idea", detail: `The one-sentence version of ${topic}.` },
    { label: "Why it matters", detail: `The problem ${topic} solves, and what goes wrong without it.` },
    { label: "How it works", detail: `The moving parts of ${topic} and how they interact.` },
    { label: "Example", detail: `A concrete case where ${topic} shows up.` },
    { label: "Pitfalls", detail: `Common misconceptions about ${topic}.` },
  ];
}

export function explain(topic, userNotes) {
  const title = titleCase(topic);
  const own = sentences(userNotes);
  return {
    version: 1,
    suggestedTitle: title,
    overview: `${title}, broken into the core idea, an analogy and a worked example. This demo runs without a live model, so the explanation is assembled from your topic${own.length ? " and notes" : ""}.`,
    sections: [
      {
        heading: "The core idea",
        type: "concept",
        body: "",
        bullets: own.length
          ? own.slice(0, 4)
          : [`What ${title} is, in one sentence`, "The key parts and how they connect", "When it applies, and when it doesn't"],
      },
      {
        heading: "An analogy",
        type: "analogy",
        body: `Think of ${title} like learning a route through a new city: at first every turn has to be spelled out, then landmarks start to connect, and eventually you find shortcuts on your own.`,
        bullets: [],
      },
      {
        heading: "A worked example",
        type: "example",
        body: `Explain ${title} to a friend in three sentences and sketch it as you go. The gaps you notice while drawing are exactly what the visual below is meant to fill.`,
        bullets: [],
      },
    ],
  };
}

function pickType(text, preferred) {
  if (preferred && preferred !== "auto") return preferred;
  const t = text.toLowerCase();
  const stepWords = t.match(/\b(step|then|first|next|after|finally|if|otherwise|repeat|until|request)\b/g) ?? [];
  if (stepWords.length >= 3) return "diagram";
  if (/\b(stage|phase|timeline|history|cycle|over time)\b/.test(t)) return "animation";
  if (/\b(looks like|picture|species|animal|landmark|object)\b/.test(t)) return "image";
  return "mind_map";
}

function mindMap(title, items, variant) {
  const nodes = [{ id: "root", label: title, detail: "" }, ...items.map((f, i) => ({ id: `n${i}`, label: f.label, detail: f.detail }))];
  return {
    type: "mind_map",
    version: 1,
    rootLabel: title,
    nodes,
    edges: items.map((_, i) => ({ sourceId: "root", targetId: `n${i}`, relationshipLabel: "" })),
    layoutHints: { orientation: variant % 2 ? "horizontal" : "radial", rootNodeId: "root" },
    citations: [],
  };
}

function diagram(items) {
  const steps = items.slice(0, 6);
  // Two columns, snaking down, so even six steps fit the 800x600 canvas.
  const elements = steps.map((f, i) => ({
    id: `e${i}`,
    elementType: i === 0 || i === steps.length - 1 ? "terminator" : "process",
    label: f.label,
    x: i % 2 === 0 ? 60 : 480,
    y: 40 + i * 90,
    width: 260,
    height: 60,
    style: "",
    accessibilityLabel: f.detail,
  }));
  return {
    type: "diagram",
    version: 1,
    elements,
    connections: elements.slice(1).map((el, i) => ({ id: `c${i}`, sourceId: elements[i].id, targetId: el.id, label: String(i + 1), style: "" })),
  };
}

function animation(title, items, newArtifact) {
  const scenes = items.slice(0, 5).map((f, i) => ({
    id: `s${i}`,
    order: i + 1,
    title: f.label,
    narration: f.detail,
    durationSeconds: Math.min(8, Math.max(4, Math.round(f.detail.split(/\s+/).length / 3))),
    transitionToNext: i === items.length - 1 ? "none" : i % 2 ? "slide" : "fade",
    needsVisualAsset: i === 0,
    visualPrompt: "",
    assetArtifactIds: i === 0 ? [newArtifact(title)] : [],
  }));
  return { type: "animation", version: 1, scenes };
}

/**
 * One Visualize run. `newArtifact(title)` registers a generated image and
 * returns its id - the demo's version of uploading to S3.
 */
export function visualize(conceptText, preferredType, newArtifact, variant = 0) {
  const title = titleFrom(conceptText);
  const items = facets(title, conceptText);
  // Regenerating shuffles the order a little, so a new version looks different.
  const ordered = variant ? [...items.slice(variant % items.length), ...items.slice(0, variant % items.length)] : items;
  const type = pickType(conceptText, preferredType);
  const summary = sentences(conceptText).find((s) => s.toLowerCase() !== title.toLowerCase()) ?? `A visual overview of ${title}.`;

  let visualization;
  if (type === "diagram") visualization = diagram(ordered);
  else if (type === "animation") visualization = animation(title, ordered, newArtifact);
  else if (type === "image") {
    visualization = { type: "image", version: 1, imagePrompt: `${title}, flat illustration`, artifactId: newArtifact(title), altText: `Illustration of ${title}.` };
  } else visualization = mindMap(title, ordered, variant);

  return { title, summary, visualizationType: type, visualization };
}

export function artifactFor(title) {
  return { title, hue: hueFor(title), motif: motifFor(title) };
}

export function answer(concept, question) {
  return `Good question. ${concept.summary} Looking at "${question.trim()}" in the context of ${concept.title}: start from the visual above and trace which part your question touches, then ask what would change if that part were missing.\n\n(In this demo answers come from a template. With the real backend, this question, the last 12 messages and the concept are sent to Claude or Gemini.)`;
}

export function condenseChat(concept, history) {
  const questions = history.filter((m) => m.role === "user").map((m) => m.text.trim());
  const asked = questions.length ? questions.at(-1) : "a follow-up question";
  return `Asked about ${concept.title}: "${asked}". Takeaway: ${concept.summary}`;
}
