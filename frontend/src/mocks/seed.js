/**
 * The library a first-time demo visitor sees. Shapes match what the real
 * backend stores (see generation.model.*Payload), so the same renderers
 * draw them unchanged.
 */

function daysAgo(days) {
  return new Date(Date.now() - days * 86_400_000).toISOString();
}

const FOLDERS = [
  { id: "f-networking", name: "Networking", color: "sky", createdAt: daysAgo(30) },
  { id: "f-system-design", name: "System design", color: "violet", createdAt: daysAgo(28) },
  { id: "f-biology", name: "Biology", color: "green", createdAt: daysAgo(21) },
  { id: "f-anatomy", name: "Anatomy", color: "coral", createdAt: daysAgo(14) },
  { id: "f-javascript", name: "JavaScript", color: "yellow", createdAt: daysAgo(10) },
];

const ARTIFACTS = {
  "a-photo-sun": { title: "Sunlight reaches the leaf", hue: 45, motif: "sun" },
  "a-photo-leaf": { title: "Chloroplasts capture light", hue: 120, motif: "leaf" },
  "a-photo-glucose": { title: "Glucose is built", hue: 30, motif: "glucose" },
  "a-monarch": { title: "Monarch butterfly", hue: 28, motif: "butterfly" },
  "a-event-loop": { title: "The event loop", hue: 210, motif: "loop" },
};

const tcpV1 = {
  type: "diagram",
  version: 1,
  elements: [
    { id: "client", elementType: "actor", label: "Client", x: 40, y: 40, width: 160, height: 60 },
    { id: "server", elementType: "actor", label: "Server", x: 600, y: 40, width: 160, height: 60 },
    { id: "syn", elementType: "process", label: "SYN", x: 320, y: 160, width: 160, height: 60 },
    { id: "synack", elementType: "process", label: "SYN-ACK", x: 320, y: 280, width: 160, height: 60 },
    { id: "ack", elementType: "process", label: "ACK", x: 320, y: 400, width: 160, height: 60 },
    { id: "open", elementType: "terminator", label: "Connected", x: 320, y: 520, width: 160, height: 60 },
  ],
  connections: [
    { id: "c1", sourceId: "client", targetId: "syn", label: "" },
    { id: "c2", sourceId: "syn", targetId: "server", label: "" },
    { id: "c3", sourceId: "server", targetId: "synack", label: "" },
    { id: "c4", sourceId: "synack", targetId: "client", label: "" },
    { id: "c5", sourceId: "client", targetId: "ack", label: "" },
    { id: "c6", sourceId: "ack", targetId: "open", label: "" },
  ],
};

const tcpV2 = {
  type: "diagram",
  version: 1,
  elements: [
    { id: "client", elementType: "actor", label: "Client", x: 40, y: 40, width: 180, height: 60 },
    { id: "server", elementType: "actor", label: "Server", x: 580, y: 40, width: 180, height: 60 },
    { id: "syn", elementType: "process", label: "SYN  seq=x", x: 290, y: 150, width: 220, height: 60 },
    { id: "synack", elementType: "process", label: "SYN-ACK  ack=x+1", x: 290, y: 270, width: 220, height: 60 },
    { id: "ack", elementType: "process", label: "ACK  ack=y+1", x: 290, y: 390, width: 220, height: 60 },
    { id: "check", elementType: "decision", label: "Numbers match?", x: 40, y: 510, width: 200, height: 60 },
    { id: "open", elementType: "terminator", label: "Established", x: 560, y: 510, width: 200, height: 60 },
  ],
  connections: [
    { id: "c1", sourceId: "client", targetId: "syn", label: "1" },
    { id: "c2", sourceId: "syn", targetId: "server", label: "" },
    { id: "c3", sourceId: "server", targetId: "synack", label: "2" },
    { id: "c4", sourceId: "synack", targetId: "client", label: "" },
    { id: "c5", sourceId: "client", targetId: "ack", label: "3" },
    { id: "c6", sourceId: "ack", targetId: "check", label: "" },
    { id: "c7", sourceId: "check", targetId: "open", label: "yes" },
  ],
};

const CONCEPTS = [
  {
    id: "c-tcp-handshake",
    folderId: "f-networking",
    sourceText:
      "The client sends a SYN with its starting sequence number. The server answers with SYN-ACK, acknowledging the client's number and sending its own. The client replies with ACK. Both sides now agree on sequence numbers and the connection is established.",
    versions: [
      { title: "TCP three-way handshake", summary: "How a client and server agree on sequence numbers before any data flows: SYN, SYN-ACK, ACK.", visualization: tcpV1, createdAt: daysAgo(29) },
      { title: "TCP three-way handshake", summary: "How a client and server agree on sequence numbers before any data flows: SYN, SYN-ACK, ACK.", visualization: tcpV2, createdAt: daysAgo(6) },
    ],
  },
  {
    id: "c-cache-aside",
    folderId: "f-system-design",
    sourceText:
      "The application asks the cache for a key. On a hit it returns the cached value. On a miss it reads the row from the database. It writes the row into the cache with a TTL. It returns the value to the caller.",
    versions: [
      {
        title: "Cache-aside pattern",
        summary: "The application checks the cache first and loads from the database on a miss, then populates the cache with a TTL.",
        createdAt: daysAgo(27),
        visualization: {
          type: "diagram",
          version: 1,
          elements: [
            { id: "app", elementType: "actor", label: "App", x: 40, y: 260, width: 160, height: 60 },
            { id: "check", elementType: "decision", label: "In cache?", x: 300, y: 260, width: 180, height: 60 },
            { id: "cache", elementType: "data_store", label: "Redis cache", x: 600, y: 60, width: 180, height: 60 },
            { id: "db", elementType: "data_store", label: "Database", x: 600, y: 460, width: 180, height: 60 },
            { id: "fill", elementType: "process", label: "Set with TTL", x: 600, y: 260, width: 180, height: 60 },
            { id: "ret", elementType: "terminator", label: "Return value", x: 300, y: 500, width: 180, height: 60 },
          ],
          connections: [
            { id: "c1", sourceId: "app", targetId: "check", label: "read key" },
            { id: "c2", sourceId: "check", targetId: "cache", label: "hit" },
            { id: "c3", sourceId: "check", targetId: "db", label: "miss" },
            { id: "c4", sourceId: "db", targetId: "fill", label: "" },
            { id: "c5", sourceId: "fill", targetId: "ret", label: "" },
          ],
        },
      },
    ],
  },
  {
    id: "c-cap-theorem",
    folderId: "f-system-design",
    sourceText:
      "A distributed data store can only guarantee two of consistency, availability and partition tolerance at once. Partitions happen in real networks, so the practical choice is between consistency and availability during a partition.",
    versions: [
      {
        title: "CAP theorem",
        summary: "During a network partition a distributed database must choose between consistency and availability.",
        createdAt: daysAgo(20),
        visualization: {
          type: "mind_map",
          version: 1,
          rootLabel: "CAP theorem",
          nodes: [
            { id: "root", label: "CAP theorem", detail: "Pick two of three when the network splits." },
            { id: "c", label: "Consistency", detail: "Every read sees the latest write." },
            { id: "a", label: "Availability", detail: "Every request gets a non-error response." },
            { id: "p", label: "Partition tolerance", detail: "The system keeps working when messages are lost." },
            { id: "cp", label: "CP systems", detail: "e.g. HBase, ZooKeeper - refuse requests rather than serve stale data." },
            { id: "ap", label: "AP systems", detail: "e.g. Cassandra, DynamoDB - stay up and reconcile later." },
          ],
          edges: [
            { sourceId: "root", targetId: "c", relationshipLabel: "" },
            { sourceId: "root", targetId: "a", relationshipLabel: "" },
            { sourceId: "root", targetId: "p", relationshipLabel: "" },
            { sourceId: "c", targetId: "cp", relationshipLabel: "favoured by" },
            { sourceId: "a", targetId: "ap", relationshipLabel: "favoured by" },
          ],
          layoutHints: { orientation: "radial", rootNodeId: "root" },
          citations: [],
        },
      },
    ],
  },
  {
    id: "c-photosynthesis",
    folderId: "f-biology",
    sourceText:
      "Sunlight reaches the leaf. Chlorophyll in the chloroplasts absorbs the light energy. Water is split, releasing oxygen. The Calvin cycle uses carbon dioxide to build glucose. The plant stores the glucose as energy.",
    versions: [
      {
        title: "Photosynthesis",
        summary: "Plants turn light, water and carbon dioxide into glucose and oxygen, in two linked stages.",
        createdAt: daysAgo(18),
        visualization: {
          type: "animation",
          version: 1,
          scenes: [
            { id: "s1", order: 1, title: "Light arrives", narration: "Sunlight lands on the leaf. That light is the energy source for everything that follows.", durationSeconds: 5, transitionToNext: "fade", assetArtifactIds: ["a-photo-sun"] },
            { id: "s2", order: 2, title: "Chlorophyll absorbs it", narration: "Chlorophyll inside the chloroplasts absorbs red and blue light and reflects green, which is why leaves look green.", durationSeconds: 6, transitionToNext: "slide", assetArtifactIds: ["a-photo-leaf"] },
            { id: "s3", order: 3, title: "Water is split", narration: "The captured energy splits water molecules. Oxygen is released as a by-product, and the energy is stored in ATP and NADPH.", durationSeconds: 6, transitionToNext: "fade", assetArtifactIds: [] },
            { id: "s4", order: 4, title: "The Calvin cycle", narration: "ATP and NADPH power the Calvin cycle, which fixes carbon dioxide from the air into glucose.", durationSeconds: 6, transitionToNext: "fade", assetArtifactIds: ["a-photo-glucose"] },
            { id: "s5", order: 5, title: "Energy stored", narration: "The plant uses glucose for energy and growth, or stores it as starch for later.", durationSeconds: 5, transitionToNext: "none", assetArtifactIds: [] },
          ],
        },
      },
    ],
  },
  {
    id: "c-monarch",
    folderId: "f-biology",
    sourceText: "The monarch butterfly has orange wings with black veins and white spots, and migrates thousands of kilometres each autumn.",
    versions: [
      {
        title: "Monarch butterfly",
        summary: "Orange wings with black veins and white-spotted borders; famous for its multi-generation migration to Mexico.",
        createdAt: daysAgo(15),
        visualization: {
          type: "image",
          version: 1,
          imagePrompt: "A monarch butterfly with open wings, flat illustration",
          artifactId: "a-monarch",
          altText: "Illustration of a monarch butterfly with orange wings, black veins and white spots.",
        },
      },
    ],
  },
  {
    id: "c-back-muscles",
    folderId: "f-anatomy",
    sourceText:
      "The erector spinae run along the spine and keep it upright. The latissimus dorsi pull the arms down and back. The rhomboids and middle trapezius squeeze the shoulder blades together. Weak back muscles let the upper body round forward.",
    versions: [
      {
        title: "Back muscles",
        summary: "The muscles that keep the spine upright and the shoulder blades back: erector spinae, lats, rhomboids and trapezius.",
        createdAt: daysAgo(12),
        visualization: {
          type: "mind_map",
          version: 1,
          rootLabel: "Back muscles",
          nodes: [
            { id: "root", label: "Back muscles", detail: "Keep the spine upright and the shoulders back." },
            { id: "es", label: "Erector spinae", detail: "Run along the spine and hold it upright." },
            { id: "lat", label: "Latissimus dorsi", detail: "Pull the arms down and back." },
            { id: "rh", label: "Rhomboids", detail: "Squeeze the shoulder blades together." },
            { id: "tr", label: "Middle trapezius", detail: "Works with the rhomboids to retract the shoulder blades." },
            { id: "weak", label: "When weak", detail: "The upper body rounds forward - poor posture." },
          ],
          edges: [
            { sourceId: "root", targetId: "es", relationshipLabel: "" },
            { sourceId: "root", targetId: "lat", relationshipLabel: "" },
            { sourceId: "root", targetId: "rh", relationshipLabel: "" },
            { sourceId: "root", targetId: "tr", relationshipLabel: "" },
            { sourceId: "rh", targetId: "tr", relationshipLabel: "pair with" },
            { sourceId: "root", targetId: "weak", relationshipLabel: "" },
          ],
          layoutHints: { orientation: "radial", rootNodeId: "root" },
          citations: [],
        },
      },
    ],
  },
  {
    id: "c-posture",
    folderId: "f-anatomy",
    sourceText:
      "Good posture stacks the ears, shoulders and hips in a line. Slouching rounds the upper back and pushes the head forward. That strains the neck and lower back. Holding posture depends on back muscles that keep the spine upright and the shoulder blades back.",
    versions: [
      {
        title: "Posture",
        summary: "Ears, shoulders and hips stacked in a line; held by back and core muscles that keep the spine upright.",
        createdAt: daysAgo(11),
        visualization: {
          type: "mind_map",
          version: 1,
          rootLabel: "Posture",
          nodes: [
            { id: "root", label: "Posture", detail: "How the body is stacked while sitting or standing." },
            { id: "line", label: "Stacked line", detail: "Ears over shoulders over hips." },
            { id: "slouch", label: "Slouching", detail: "Rounds the upper back and pushes the head forward." },
            { id: "strain", label: "Strain", detail: "Neck and lower back take the load." },
            { id: "muscles", label: "Back muscles", detail: "Keep the spine upright and the shoulder blades back." },
          ],
          edges: [
            { sourceId: "root", targetId: "line", relationshipLabel: "" },
            { sourceId: "root", targetId: "slouch", relationshipLabel: "" },
            { sourceId: "slouch", targetId: "strain", relationshipLabel: "causes" },
            { sourceId: "root", targetId: "muscles", relationshipLabel: "held by" },
          ],
          layoutHints: { orientation: "radial", rootNodeId: "root" },
          citations: [],
        },
      },
    ],
  },
  {
    id: "c-event-loop",
    folderId: "f-javascript",
    sourceText:
      "JavaScript runs one call stack. Async work like timers and fetch is handed to the browser. Finished callbacks wait in the task queue, and promise callbacks wait in the microtask queue. When the stack is empty the event loop runs every microtask, then the next task.",
    versions: [
      {
        title: "The JavaScript event loop",
        summary: "One call stack, a task queue and a microtask queue: how single-threaded JavaScript handles async work.",
        createdAt: daysAgo(8),
        visualization: {
          type: "animation",
          version: 1,
          scenes: [
            { id: "s1", order: 1, title: "One call stack", narration: "JavaScript runs on a single thread with one call stack. Each function call is pushed on and popped off when it returns.", durationSeconds: 6, transitionToNext: "fade", assetArtifactIds: ["a-event-loop"] },
            { id: "s2", order: 2, title: "Hand off async work", narration: "setTimeout, fetch and DOM events are handed to the browser, so the stack is free to keep running.", durationSeconds: 5, transitionToNext: "slide", assetArtifactIds: [] },
            { id: "s3", order: 3, title: "Two queues", narration: "Finished callbacks wait in the task queue. Promise callbacks wait in the microtask queue, which has priority.", durationSeconds: 6, transitionToNext: "fade", assetArtifactIds: [] },
            { id: "s4", order: 4, title: "The loop turns", narration: "When the stack is empty, the event loop drains every microtask, then takes the next task. That's why a resolved promise runs before a zero-delay timeout.", durationSeconds: 7, transitionToNext: "none", assetArtifactIds: [] },
          ],
        },
      },
    ],
  },
];

const NOTES = [
  { id: "n-tcp-1", conceptId: "c-tcp-handshake", source: "chat", createdAt: daysAgo(5), content: "Why three steps and not two: each side has to prove it received the other's starting sequence number, and the middle SYN-ACK does two jobs at once." },
  { id: "n-tcp-2", conceptId: "c-tcp-handshake", source: "chat", createdAt: daysAgo(4), content: "A SYN flood abuses step 2: the server holds half-open connections waiting for ACKs that never come. SYN cookies avoid storing that state." },
];

/** A fresh copy every call, so resetting the demo never shares objects with the old state. */
export function createSeedState() {
  return structuredClone({
    folders: FOLDERS,
    artifacts: ARTIFACTS,
    concepts: CONCEPTS.map((c) => ({
      id: c.id,
      folderId: c.folderId,
      sourceText: c.sourceText,
      versions: c.versions.map((v, i) => ({ ...v, version: i + 1, visualizationType: v.visualization.type })),
      createdAt: c.versions[0].createdAt,
      updatedAt: c.versions.at(-1).createdAt,
    })),
    notes: NOTES,
  });
}
