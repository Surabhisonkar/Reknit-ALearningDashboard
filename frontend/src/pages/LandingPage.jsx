import { Button } from "../shared/ui/Buttons";
import { Icon } from "../shared/ui/Icon";
import { Pill } from "../shared/ui/Pill";

function FeatureCard({ tone, icon, title, text, link }) {
  return (
    <article className={`feature-card feature-${tone}`}>
      <div className="feature-icon">{icon}</div>
      <h3>{title}</h3>
      <p>{text}</p>
      <a href="/workspace">
        {link} <span>{"->"}</span>
      </a>
    </article>
  );
}

function LandingPage() {
  return (
    <main className="landing-page">
      <section className="landing-hero page-width">
        <div className="eyebrow">
          <span className="status-dot" /> VISUAL LEARNING TOOL
        </div>
        <h1>Turn your notes into visual concepts that actually stick.</h1>
        <p>
          Reknit automatically converts raw text into clean diagrams, dynamic mind maps, and
          micro-animations. Revisit ideas by looking, never re-reading.
        </p>
        <div className="hero-actions">
          <Button to="/create">
            <Icon>+</Icon> New concept
          </Button>
          <Button to="/library" secondary>
            <Icon>o</Icon> Browse library
          </Button>
        </div>
      </section>

      <section className="showcase page-width tactile-panel">
        <div className="raw-note">
          <div className="note-heading">
            <Pill tone="yellow">RAW TEXT INPUT</Pill>
            <span className="note-pin" />
          </div>
          <h3>How Dopamine Loops Work</h3>
          <ol>
            <li>Trigger creates anticipation</li>
            <li>Cue prompts immediate action</li>
            <li>Reward delivers release loop</li>
          </ol>
          <small>
            Imported from Obsidian <span>18 words</span>
          </small>
        </div>
        <div className="transform-arrow">
          <strong>+</strong>
          <span>AI VISUAL ENGINE</span>
        </div>
        <div className="mindmap-card">
          <div className="mindmap-top">
            <Pill tone="teal">AUTO-GENERATED</Pill>
            <Pill tone="coral">3 nodes</Pill>
          </div>
          <div className="mindmap-lines">
            <span className="node node-one">Dopamine Cycle</span>
            <span className="node node-two">Trigger</span>
            <span className="node node-three">Reward</span>
            <span className="node node-four">Cue</span>
          </div>
          <small>
            Visual map ready <span>12:04 AM</span>
          </small>
        </div>
      </section>

      <section className="feature-section page-width">
        <div className="section-heading">
          <Pill tone="teal">BUILT FOR MINDS THAT THINK IN PICTURES</Pill>
          <h2>See the idea. Remember the idea.</h2>
          <p>
            No endless bullet points. Reknit turns your notes into visual cues that make
            recall feel natural.
          </p>
        </div>
        <div className="feature-grid">
          <FeatureCard
            tone="coral"
            icon="#"
            title="Visual Mind Maps"
            text="Instant spatial diagrams organize your thoughts so connections become obvious."
            link="Explore map structure"
          />
          <FeatureCard
            tone="teal"
            icon="o"
            title="Micro-Animations"
            text="Step-by-step concepts play at the pace your brain prefers."
            link="See a sample animation"
          />
          <FeatureCard
            tone="yellow"
            icon="?"
            title="10-Second Recall"
            text="Revisit tough ideas with tiny prompts that make retrieval effortless."
            link="Try recall mode"
          />
        </div>
      </section>

      <section className="callout page-width">
        <div>
          <Pill tone="coral">IN DEVELOPMENT</Pill>
          <h2>Stop forcing linear note-taking on a non-linear brain.</h2>
          <p>Export directly to Arc, Notion, or wherever your ideas already live.</p>
        </div>
        <Button to="/workspace">
          Start visualizing <Icon>{"->"}</Icon>
        </Button>
      </section>
    </main>
  );
}

export default LandingPage;
