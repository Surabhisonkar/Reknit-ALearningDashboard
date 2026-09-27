// import { Button } from "../shared/ui/Buttons";
import { Pill } from "../shared/ui/Pill";

// TODO(Surabhi): swap the role/bio line and the Reknit placeholders
// once you've settled on a product name - everything else on this page
// (tech stack, architecture highlights) reflects what's actually built.
const techStack = [
  "React 19 + Vite",
  "Spring Boot 3.5.9 / Java 21",
  "MySQL 8 + Flyway",
  "AWS Cognito, SQS, S3",
  "Redis (Bucket4j rate limiting)",
  "Claude + Gemini + OpenAI (adapter pattern)",
];

// TODO(Surabhi): this is a placeholder - swap in your real projects.
// Learning Dashboard's entry below is accurate to what's actually built;
// the other two are structural placeholders so the grid has real shape
// to work with rather than being empty.
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
  // {
  //   title: "[Project Name]",
  //   tagline: "[One or two sentences on what it does and the problem it solves.]",
  //   tags: ["[Tag]", "[Tag]"],
  //   tone: "yellow",
  // },
];

// TODO(Surabhi): add/remove skills freely - this only lists what's
// demonstrably used in Learning Dashboard's own codebase, not your full
// range.
const skillGroups = [
  { label: "Frontend", tone: "coral", skills: ["React", "Vite", "Tailwind CSS", "Cognito PKCE auth"] },
  { label: "Backend", tone: "teal", skills: ["Java", "Spring Boot", "REST APIs", "Resilience4j"] },
  { label: "Data & Cloud", tone: "yellow", skills: ["MySQL", "Redis", "AWS (Cognito, SQS, S3)", "Flyway"] },
  { label: "AI / ML", tone: "coral", skills: ["LLM provider integration", "RAG", "Prompt engineering"] },
];

function AboutPage() {
  return (
    <main className="about-page page-width">
      <div className="about-hero" >
        <Pill tone="coral">ABOUT THE BUILDER</Pill>
        <h1>Surabhi Sonkar</h1>
        <p className="role">Software Engineer / Builder of Reknit</p>
        <p style={{ textAlign: "justify" }}>
          <br /><b style={{fontSize:"19px"}}>I built Reknit because I wanted a better way to revisit what I've learned.</b>
          <br />Ideas can get buried under pages of notes. I wanted something that would make coming back to an old idea feel less like going through notes 
          and more like picking up a thought where I left it.
        </p>
        {/* Display buttons for CREATE and WORKSPACE */}
        {/* <div className="about-actions">
          <Button to="/create">Work with Reknit</Button>
          <Button secondary to="/workspace">
            Open workspace
          </Button>
        </div> */}
      </div>

      <section className="about-section">
        <div className="section-heading">
          <Pill tone="teal">WHY I BUILT Reknit</Pill>
          <h2>Some ideas deserve more than a page of notes.</h2>
          <p>
            <b>Reknit is built around a simple idea: learning shouldn't stop when you close your notes. 
            It should be easy to come back to what you've learned, see it from a different perspective, 
            and build on what you already know.</b>
          </p>
        </div>
        <div className="principles">
          <div>
            <Pill tone="coral">01
            &nbsp;&nbsp;&nbsp;
            <h4> Make it visual</h4></Pill>
            <p>Revisit topics with the context you need. 
            Refresh your understanding, reconnect related concepts, and keep moving forward.</p>
          </div>
          <div>
            <Pill tone="teal">02
            &nbsp;&nbsp;&nbsp;
            <h4>Make it easier to revisit</h4></Pill>
            <p>Come back to what you've learned without starting from scratch. 
                Pick up where you left off, refresh your understanding, and keep building on it.</p>
          </div>
        </div>
      </section>

      <section className="about-section">
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
      </section>

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
          {techStack.map((stack, index) => (
            <div className="stack-card" key={stack}>
              <Pill tone={index % 3 === 0 ? "coral" : index % 3 === 1 ? "yellow" : "teal"}>
                {String(index + 1).padStart(2, "0")}
              </Pill>
              <strong>{stack}</strong>
              <span>Designed to keep the concept layer fast and flexible.</span>
            </div>
          ))}
        </div>
      </section>

      <section className="architecture-band">
        <Pill tone="coral">ARCHITECTURE HIGHLIGHTS</Pill>
        <div className="architecture-grid">
          <div>
            <h3>Adapter Pattern for AI Providers</h3>
            <p>Text generation and image generation are swappable independently, per provider.</p>
          </div>
          <div>
            <h3>Recall-Ready Concepts</h3>
            <p>Notes become small, revisit-able experiences instead of dead files.</p>
          </div>
          <div>
            <h3>Personalized RAG</h3>
            <p>New concepts are grounded in what you've already learned, not generated in isolation.</p>
          </div>
        </div>
      </section>
    </main>
  );
}

export default AboutPage;
