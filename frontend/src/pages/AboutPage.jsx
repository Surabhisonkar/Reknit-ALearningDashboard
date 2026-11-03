import { Pill } from "../shared/ui/Pill";

// The photo is kept out of git (see .gitignore), so it's picked up only when
// the file exists; without it the hero shows initials and the build still works.
const photos = import.meta.glob("../assets/about/surabhi.jpg", { eager: true, import: "default" });
const photo = Object.values(photos)[0] ?? null;

// Matches what the code actually uses - check backend/pom.xml and
// frontend/package.json before changing a version here.
const techStack = [
  { name: "React 19, Vite 8, Tailwind CSS 4", detail: "The app itself: pages, the Create flow and every visual renderer." },
  { name: "Spring Boot 4.1 on Java 21", detail: "One codebase run as two processes: the API and a background worker." },
  { name: "MySQL 8 with Flyway", detail: "Concepts, version history, folders and notes, with versioned schema migrations." },
  { name: "AWS Cognito, SQS and S3", detail: "Sign-in, the queue for AI work, and storage for generated images and videos." },
  { name: "Redis with Bucket4j", detail: "Per-user rate limits that hold across every server." },
  { name: "Gemini, Claude, OpenAI, Qwen-Image", detail: "Gemini by default, Claude and OpenAI as optional fallbacks, Qwen-Image for illustrations." },
  { name: "Mermaid, React Flow and ELK.js", detail: "Diagrams drawn as real SVG, and mind maps laid out automatically that you can rearrange." },
  { name: "Remotion", detail: "Animation storyboards rendered to MP4 by a small Node service, or played live in the browser." },
];

const architecture = [
  {
    title: "AI work never blocks a request",
    text: "The API queues each generation on SQS and returns straight away; a separate worker calls the model while the browser checks back.",
  },
  {
    title: "Swappable providers with fallback",
    text: "Each task has an ordered list of providers, wrapped in retries and circuit breakers. A provider without a key simply drops out.",
  },
  {
    title: "AI output is checked before it's kept",
    text: "Every answer is validated against its schema (a real tree, safe Mermaid code, a sensible storyboard) and content-safety rules.",
  },
  {
    title: "Versions, not overwrites",
    text: "Regenerating a concept or saving your own edit adds a version; earlier ones stay one click away.",
  },
  {
    title: "Related concepts from your own library",
    text: "Embeddings live in MySQL and are ranked by cosine similarity, so new ideas link back to what you already saved.",
  },
  {
    title: "One animation, two outputs",
    text: "The same Remotion scene code plays in the browser and renders the MP4, so the video always matches the preview.",
  },
];

// Projects section is commented out below; uncomment both to show it again.
/*
const projects = [
  {
    title: "Reknit- Learning Dashboard",
    tagline: "A visual learning workspace that uses AI to turn concepts into mind maps, diagrams, animations, and images, making them easier to understand and revisit.",
    tags: ["Spring Boot", "React", "AWS", "RAG"],
    tone: "coral",
  },
  {
    title: "ML Algorithm Visualizer",
    tagline: "An interactive workspace to explore ML algorithms, experiment with parameters, and see how they affect the results.",
    tags: ["Python", "Streamlit", "NumPy", "Plotly", "Machine Learning"],
    tone: "teal",
  },
];
*/

// Only what Reknit's own codebase demonstrates.
const skillGroups = [
  { label: "Frontend", tone: "coral", skills: ["React", "Vite", "Tailwind CSS", "React Flow", "Mermaid", "Remotion", "Cognito PKCE auth"] },
  { label: "Backend", tone: "teal", skills: ["Java", "Spring Boot", "REST APIs", "Async workers", "Resilience4j"] },
  { label: "Data & Cloud", tone: "yellow", skills: ["MySQL", "Flyway", "Redis", "AWS (Cognito, SQS, S3)", "Docker", "GitHub Actions"] },
  { label: "AI", tone: "coral", skills: ["LLM provider integration", "Structured output validation", "RAG", "Prompt engineering"] },
];

const TONES = ["coral", "yellow", "teal"];

function AboutPage() {
  return (
    <main className="about-page page-width">
      <section className="about-hero">
        <div className="about-portrait">
          {photo ? <img src={photo} alt="Surabhi Sonkar" /> : <span aria-hidden="true">SS</span>}
        </div>
        <div className="about-intro">
          <Pill tone="coral">ABOUT THE BUILDER</Pill>
          <h1>Surabhi Sonkar</h1>
          <p className="role">Software Engineer, builder of Reknit</p>
          <p className="about-lead">I built Reknit because I wanted a better way to revisit what I&apos;ve learned.</p>
          <p>
            Ideas get buried under pages of notes. I wanted coming back to an old idea to feel less like digging
            through notes and more like picking up a thought where I left it.
          </p>
        </div>
      </section>

      <section className="about-section">
        <div className="section-heading">
          <Pill tone="teal">WHY I BUILT REKNIT</Pill>
          <h2>Some ideas deserve more than a page of notes.</h2>
          <p>
            Learning shouldn&apos;t stop when you close your notes. It should be easy to come back to what you&apos;ve
            learned, see it from a different angle, and build on what you already know.
          </p>
        </div>
        <div className="principles">
          <div>
            <Pill tone="coral">01</Pill>
            <h3>Make it visual</h3>
            <p>A diagram, an illustration, a mind map or a short animation: pick the form that makes the idea click.</p>
          </div>
          <div>
            <Pill tone="teal">02</Pill>
            <h3>Make it easy to revisit</h3>
            <p>Come back without starting from scratch: pick up where you left off, refresh it, and keep building on it.</p>
          </div>
        </div>
      </section>

      {/* <section className="about-section">
        <div className="section-heading">
          <Pill tone="coral">PROJECTS</Pill>
          <h2>A few things I've built.</h2>
        </div>
        <div className="projects-grid">
          {projects.map((project) => (
            <div className="project-card" key={project.title}>
              <Pill tone={project.tone}>{project.title}</Pill>
              <p>{project.tagline}</p>
              <div className="project-tags">
                {project.tags.map((tag) => (
                  <span className="project-tag" key={tag}>
                    {tag}
                  </span>
                ))}
              </div>
            </div>
          ))}
        </div>
      </section> */}

      <section className="about-section">
        <div className="section-heading">
          <Pill tone="yellow">SKILLS</Pill>
          <h2>What I bring to a build.</h2>
        </div>
        <div className="skills-grid">
          {skillGroups.map((group) => (
            <div className="skills-group" key={group.label}>
              <Pill tone={group.tone}>{group.label}</Pill>
              <div className="skills-pills">
                {group.skills.map((skill) => (
                  <span className="skill-pill" key={skill}>
                    {skill}
                  </span>
                ))}
              </div>
            </div>
          ))}
        </div>
      </section>

      <section className="about-section">
        <div className="section-heading">
          <Pill tone="teal">TECH STACK</Pill>
          <h2>Small surface. Thoughtful machinery.</h2>
        </div>
        <div className="stack-grid">
          {techStack.map((item, index) => (
            <div className="stack-card" key={item.name}>
              <Pill tone={TONES[index % 3]}>{String(index + 1).padStart(2, "0")}</Pill>
              <strong>{item.name}</strong>
              <span>{item.detail}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="about-section architecture-band">
        <div className="section-heading">
          <Pill tone="coral">ARCHITECTURE HIGHLIGHTS</Pill>
          <h2>How it holds together.</h2>
        </div>
        <div className="architecture-grid">
          {architecture.map((item) => (
            <div key={item.title}>
              <h3>{item.title}</h3>
              <p>{item.text}</p>
            </div>
          ))}
        </div>
      </section>
    </main>
  );
}

export default AboutPage;
