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

/** Splits a facet's sentence into a few short sub-points, for the mind map's second level. */
function subPoints(detail) {
  return detail
    .split(/,|;| and | so | because | which /i)
    .map((part) => shortLabel(part.trim()))
    .filter((label) => label.length > 2)
    .slice(0, 2);
}

function mindMap(title, items) {
  const nodes = [{ id: "root", label: title, detail: "", parentId: null }];
  items.forEach((f, i) => {
    nodes.push({ id: `n${i}`, label: f.label, detail: f.detail, parentId: "root" });
    subPoints(f.detail)
      .filter((label) => label.toLowerCase() !== f.label.toLowerCase())
      .forEach((label, j) => nodes.push({ id: `n${i}-${j}`, label, detail: "", parentId: `n${i}` }));
  });
  return {
    type: "mind_map",
    version: 2,
    rootLabel: title,
    nodes,
    edges: items.length > 2 ? [{ sourceId: "n0", targetId: `n${items.length - 1}`, relationshipLabel: "relates to" }] : [],
    layoutHints: { orientation: "horizontal", rootNodeId: "root" },
    citations: [],
  };
}

/** Mermaid labels can't contain a bare double quote. */
const mermaidText = (text) => text.replace(/"/g, "'");

function diagram(title, items) {
  const steps = items.slice(0, 6);
  const lines = ["flowchart TD", `  start(["${mermaidText(title)}"])`];
  steps.forEach((f, i) => lines.push(`  s${i}["${mermaidText(f.label)}"]`));
  lines.push("  start --> s0");
  steps.slice(1).forEach((_, i) => lines.push(`  s${i} -->|"then"| s${i + 1}`));
  return {
    type: "diagram",
    version: 2,
    syntax: "mermaid",
    source: lines.join("\n"),
    caption: `${title}, step by step.`,
  };
}

const ELEMENT_COLORS = ["teal", "coral", "yellow"];
const SCENE_EMOJI = ["💡", "🔍", "⚙️", "🧩", "✅", "📈", "🔁", "🎯"];

const box = (id, text, icon, enterAtSeconds, color, animation = "pop") =>
  ({ id, kind: "box", text, icon, enterAtSeconds, animation, color, fromId: "", toId: "" });
const arrow = (id, fromId, toId, text, enterAtSeconds) =>
  ({ id, kind: "arrow", text, enterAtSeconds, animation: "draw", color: "coral", fromId, toId });

/**
 * A storyboard the way the prompt asks for one: every scene names a layout
 * (so nothing is positioned by hand), boxes carry icons, arrows have labels
 * and each scene's key element uses "highlight". An intro shows the topic
 * and its parts, one scene per step follows, and a recap ties the steps
 * together - as a cycle when the notes describe one.
 */
function animation(title, items, newArtifact, conceptText) {
  const steps = items.slice(0, 5);
  if (steps.length < 2) steps.push({ label: "Why it matters", detail: `What ${title} changes, in one line.` });
  const looping = /\b(cycle|loop|repeat|again|circular)\b/i.test(conceptText);

  const intro = {
    title,
    narration: `What ${title} is about, in ${steps.length + 2} short scenes.`,
    layout: "focus",
    elements: [
      box("topic", title, "💡", 0, "coral", "highlight"),
      ...steps.slice(0, 4).map((step, i) => box(`p${i}`, step.label, SCENE_EMOJI[(i + 1) % SCENE_EMOJI.length], 1 + i * 0.5, ELEMENT_COLORS[i % 3])),
    ],
  };
  const middle = steps.map((step, i) => {
    // What the step leads to: the first part of its sentence that isn't the label itself.
    const detail = subPoints(step.detail).find((point) => point.toLowerCase() !== step.label.toLowerCase());
    const icon = SCENE_EMOJI[(i + 1) % SCENE_EMOJI.length];
    return {
      title: step.label,
      narration: step.detail,
      layout: "steps",
      elements: detail
        ? [box("main", step.label, icon, 0.3, ELEMENT_COLORS[i % 3]), box("result", detail, "", 1.6, ELEMENT_COLORS[(i + 1) % 3], "highlight"), arrow("link", "main", "result", "leads to", 1)]
        : [box("main", step.label, icon, 0.3, ELEMENT_COLORS[i % 3], "highlight")],
    };
  });
  const recapSteps = steps.slice(0, looping ? 5 : 4);
  const recap = {
    title: looping ? "And round again" : "All together",
    narration: looping ? `Each step leads to the next, and ${title} starts over.` : `The key steps of ${title}, in order.`,
    layout: looping ? "cycle" : "steps",
    elements: [
      ...recapSteps.map((step, i) => box(`r${i}`, step.label, SCENE_EMOJI[(i + 1) % SCENE_EMOJI.length], i * 0.6, ELEMENT_COLORS[i % 3])),
      ...recapSteps.slice(0, -1).map((_, i) => arrow(`ra${i}`, `r${i}`, `r${i + 1}`, "", 0.4 + i * 0.6)),
      ...(looping ? [arrow("back", `r${recapSteps.length - 1}`, "r0", "", recapSteps.length * 0.6)] : []),
    ],
  };

  const beats = [intro, ...middle, recap];
  const scenes = beats.map((beat, i) => {
    const longestEntry = Math.max(...beat.elements.map((el) => el.enterAtSeconds));
    const duration = Math.min(10, Math.max(5, Math.ceil(longestEntry + 2.5), Math.round(beat.narration.split(/\s+/).length / 2.5)));
    return {
      id: `s${i}`,
      order: i + 1,
      title: beat.title,
      narration: beat.narration,
      durationSeconds: duration,
      transitionToNext: i === beats.length - 1 ? "none" : ["fade", "slide", "wipe", "zoom"][i % 4],
      layout: beat.layout,
      visualElements: beat.elements,
      needsVisualAsset: false,
      visualPrompt: "",
      assetArtifactIds: [],
    };
  });
  return { type: "animation", version: 2, scenes, videoArtifactId: "" };
}

function illustration(title, items, newArtifact) {
  // The demo's artwork is abstract, so labels are spread around the centre rather than placed on real parts.
  const spots = [[0.3, 0.32], [0.7, 0.3], [0.72, 0.68], [0.28, 0.7], [0.5, 0.5]];
  const labels = items.slice(0, 4).map((f, i) => ({
    id: `l${i}`,
    text: f.label,
    explanation: f.detail,
    x: spots[i][0],
    y: spots[i][1],
  }));
  return {
    type: "image",
    version: 2,
    imagePrompt: `A clean educational illustration of ${title}, plain light background, no text or labels.`,
    artifactId: newArtifact(title),
    altText: `Illustration of ${title}.`,
    labels,
  };
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
  if (type === "diagram") visualization = diagram(title, ordered);
  else if (type === "animation") visualization = animation(title, ordered, newArtifact, conceptText);
  else if (type === "image") visualization = illustration(title, ordered, newArtifact);
  else visualization = mindMap(title, ordered);

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
