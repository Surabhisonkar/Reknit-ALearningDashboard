import { useConceptDetails } from "../hooks/useConceptDetails.js";
import SparkConceptCard from "./SparkConceptCard.jsx";

/** A card in the related sub-stack: its full visual is fetched only once revealed. */
export default function RelatedCard({ accessToken, related, revealed, onReveal, onDeeper, onAsk, hasDeeper }) {
  const { concept, status } = useConceptDetails(accessToken, related.id, revealed);
  const shown = concept ?? { id: related.id, title: related.title, summary: related.summary };

  return (
    <SparkConceptCard
      concept={shown}
      revealed={revealed}
      loading={status === "loading"}
      onReveal={onReveal}
      onRelated={hasDeeper ? onDeeper : undefined}
      relatedLabel="Next related concept"
      onAsk={() => onAsk(shown)}
    />
  );
}
