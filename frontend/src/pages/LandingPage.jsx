import { useState } from "react";
import { Link } from "react-router-dom";

import { ShowcaseSection } from "../features/showcase";
import { Button } from "../shared/ui/Buttons";
import { Icon } from "../shared/ui/Icon";
import { Pill } from "../shared/ui/Pill";

/** A feature card links either to a page or to one of the showcase tabs on this page. */
function FeatureCard({ tone, icon, title, text, link, to, onShow }) {
  return (
    <article className={`feature-card feature-${tone}`}>
      <div className="feature-icon">{icon}</div>
      <h3>{title}</h3>
      <p>{text}</p>
      {to ? (
        <Link to={to}>{link}</Link>
      ) : (
        <a href="#showcase" onClick={onShow}>
          {link}
        </a>
      )}
    </article>
  );
}

function LandingPage() {
  const [showcaseType, setShowcaseType] = useState("diagram");

  function showInShowcase(type) {
    return (event) => {
      event.preventDefault();
      setShowcaseType(type);
      document.getElementById("showcase")?.scrollIntoView({ behavior: "smooth", block: "start" });
    };
  }

  return (
    <main className="landing-page">
      <section className="landing-hero page-width">
        <div className="eyebrow">
          <span className="status-dot" /> VISUAL LEARNING TOOL
        </div>
        <h1>Turn your notes into visual concepts that actually stick.</h1>
        <p>
          Paste your notes and pick how to see them: a clean diagram, a labelled illustration, a
          mind map or a short animation. Revisit ideas by looking, never re-reading.
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
            text="Branches you can fold, rename, drag and rearrange, laid out for you automatically."
            link="Explore the mind map"
            onShow={showInShowcase("mind_map")}
          />
          <FeatureCard
            tone="teal"
            icon="o"
            title="Micro-Animations"
            text="Step-by-step concepts play at the pace your brain prefers."
            link="Watch a sample animation"
            onShow={showInShowcase("animation")}
          />
          <FeatureCard
            tone="yellow"
            icon="?"
            title="10-Second Recall"
            text="Revisit tough ideas with tiny prompts that make retrieval effortless."
            link="Try recall mode"
            to="/spark"
          />
        </div>
      </section>

      <ShowcaseSection activeType={showcaseType} onSelect={setShowcaseType} />
    </main>
  );
}

export default LandingPage;
