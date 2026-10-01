# Design: Spark feed + Ask the AI + Notes (Phase 8)

Status: **implemented** on `feature/phase-8-spark` (2026-09-29). The UI was locked on a mock first (step 1), then the backend (step 2), then wired and checked against the real contract (step 3). Diagrams below are as built.

## Decisions (user, 2026-09-28/29)

| Question | Decision |
|---|---|
| What a card shows on reveal | Every visual type; the reveal is always animated per type (animation plays, mind map grows, diagram draws, image fades) |
| Feed mix | Games 20%, animations 60%, mind maps 15%, images + diagrams together 5% ("still"). Concept shares applied by the backend (`app.spark.mix`), games by the frontend (`feedMix.js`) |
| Session length | Endless scrolling. *This deliberately changes the vision's "short sessions with a clear end".* |
| Build order | Whole UI locked on a mock first, then real data |
| Voice-over | Foundation now: a swappable `Narrator` (browser voice today). **Real AI voice moves to the last phase** |
| Mini-games | Keep both, restyled, as placeholders; a `GameRegistry` + `GamePicker` strategy so new games are one file + one line. Each game shows its own Skip/Continue; `ownsGestures` opts a drag-based game out of feed swipes |
| Deep dive | Icon-only: open in Library, ask the AI |
| Ask the AI | Normal chat in a bottom sheet; not saved unless the user chooses "Save as note" (AI-condensed, shown under Notes on Concept Detail) or "Make a visual" (opens Capture pre-filled; a new concept, linked by Related Concepts) |
| Chat limits | No cap on questions; a shared "ask" rate limit of 20/min; the prompt uses the last 12 messages / 8,000 chars |
| Due for review | Visible mode that says "coming soon" until Phase 9 |
| Reveal on return | Teaser again every time a card is left (recall practice) |
| Swipes vs scrolling | The revealed visual scrolls by finger and ignores swipes; swipe on the card's header/footer (or anywhere on a teaser) |
| Weighting code | A small `SparkFeedSampler` + weights in `application.yml` |
| Prompt safety | Chat text has `<` and `>` neutralized, so a message can't close the prompt's tags |
| Migration | `V4__concept_notes.sql` (the parked lifecycle migration moves to V5) |

## ER

```mermaid
erDiagram
    users ||--o{ concept_notes : "owns (CASCADE)"
    concepts ||--o{ concept_notes : "has (CASCADE)"
    concept_notes {
        char36 id PK "V4"
        char36 concept_id FK
        char36 user_id FK
        text content "AI-condensed chat, a few lines"
        varchar16 source "chat"
        datetime6 created_at
    }
```

## Backend classes

```mermaid
classDiagram
    direction LR
    namespace concept {
        class ConceptNote {
            <<entity>>
            -id, conceptId, userId: UUID
            -content: String
            -source: String "chat"
        }
        class ConceptNoteRepository { <<interface>> }
        class ConceptNoteService {
            +addNote(conceptId, userId, content, source) ConceptNote
            +listForConcept(conceptId, userId) List
            +deleteNote(noteId, conceptId, userId)
        }
        class ConceptService {
            +randomSparkFeed(userId, folderName, excludeIds, limit) "every type, via the sampler"
            +requireOwnedConcept(conceptId, userId) Concept "not transactional"
        }
        class SparkFeedSampler {
            +sample(pool, limit) List "bucket by weight, then a concept"
        }
    }
    namespace config {
        class SparkMixProperties {
            <<app.spark.mix>>
            weights: animation 60, mind_map 15, still 5
            bucketOf: image and diagram to still
        }
    }
    namespace generation {
        class JobType {
            <<enumeration>>
            ASK_CONCEPT
            CHAT_TO_NOTE
        }
        class ChatJobCodec {
            +toInputJson(conceptId, history, question) String
            +readInput(json) ChatInput
            +answerResultJson(answer) String
            +noteResultJson(noteId, content) String
        }
        class AskConceptJobHandler
        class ChatToNoteJobHandler
        class ConceptChatPipeline { +run(context, history, question) Result }
        class ChatNotePipeline { +run(context, history) Result }
        class ChatHistoryPolicy { +trim(history) List "last 12, 8000 chars" }
        class ChatReplyParser { +readField(raw, field, maxLength) String }
        class ConceptChatPromptBuilder
        class ChatNotePromptBuilder
        class PromptText { +neutralizeTags(text) String$ }
        class GenerationJobService {
            +submitAskJob(userId, conceptId, history, question)
            +submitChatToNoteJob(userId, conceptId, history)
        }
    }
    class ConceptChatController { <<web>> }
    class ConceptNoteController { <<web>> }

    ConceptService --> SparkFeedSampler
    SparkFeedSampler --> SparkMixProperties
    ConceptNoteService --> ConceptNoteRepository
    ConceptNoteService --> ConceptService : ownership
    AskConceptJobHandler --> ConceptService : ownership, non-transactional
    AskConceptJobHandler --> ConceptChatPipeline
    AskConceptJobHandler --> ChatJobCodec
    ChatToNoteJobHandler --> ConceptService
    ChatToNoteJobHandler --> ChatNotePipeline
    ChatToNoteJobHandler --> ConceptNoteService : saves the note
    ChatToNoteJobHandler --> ChatJobCodec
    ConceptChatPipeline --> ChatHistoryPolicy
    ConceptChatPipeline --> ChatReplyParser
    ConceptChatPipeline ..> ConceptChatPromptBuilder
    ChatNotePipeline --> ChatHistoryPolicy
    ChatNotePipeline --> ChatReplyParser
    ChatNotePipeline ..> ChatNotePromptBuilder
    ConceptChatPromptBuilder ..> PromptText
    ChatNotePromptBuilder ..> PromptText
    GenerationJobService --> ChatJobCodec
    ConceptChatController --> GenerationJobService
    ConceptChatController --> ConceptService : 404 before enqueueing
    ConceptNoteController --> ConceptNoteService
```

The api process never calls an AI provider: both chat routes enqueue a worker job and return 202. Ownership is checked before enqueueing (404 at once for a concept that isn't yours), and again in the worker through the non-transactional `requireOwnedConcept`, so a concept deleted mid-chat fails its job cleanly instead of hitting the rollback-only trap (handoff §4.1). `ConceptNoteService` is deliberately not `@Transactional` for the same reason.

## HTTP contract

| Route | Result |
|---|---|
| `GET /api/concepts/spark-feed?folder=&excludeIds=&limit=` | Unchanged contract; now every visual type, in the configured mix |
| `POST /api/concepts/{id}/ask` `{history: [{role, text}], question}` | 202 job; its `resultPayload` is `{kind: "ANSWER", answer}`. 400: blank or >1,000-char question, a role other than user/assistant, >200 messages. 404: not your concept. 429: rate limit |
| `POST /api/concepts/{id}/notes/from-chat` `{history}` | 202 job; `resultPayload` `{kind: "NOTE", noteId, content}`. Same 400/404/429 rules; history must not be empty |
| `GET /api/concepts/{id}/notes` | `[{id, content, source, createdAt}]`, newest first; 404 not your concept |
| `DELETE /api/concepts/{id}/notes/{noteId}` | 204; 404 if the note or concept isn't yours |

## Frontend

```mermaid
classDiagram
    direction LR
    class SparkPage { <<page, NarratorProvider>> }
    class SparkSession { <<one card on stage>> }
    class useSparkSession { +current, next(), previous(), reveal(key) "teaser again on every move" }
    class sparkModes { <<registry>> shuffle, folder, due "coming soon" }
    class SparkFeedSource { <<interface>> +nextPage(excludeIds, limit) }
    class composeFeed { <<pure>> "games ~20%, never first, never two in a row" }
    class feedMix { <<config>> game 20, animation 60, mind_map 15, still 5 }
    class GameRegistry { +register(game) +all() +byId(id) }
    class GamePicker { <<strategy>> +next(previousId) }
    class RandomNoRepeatPicker
    class SparkConceptCard { <<teaser then revealed>> }
    class RevealStage { <<finger-scrolls, ignores swipes>> }
    class revealEffects { <<registry by type>> }
    class useSwipeGestures { <<pointer, wheel>> }
    class useRelatedStack
    class RelatedCard
    class DeepDiveMenu { <<icon-only>> }
    class AskSheet { <<bottom sheet>> }
    class useConceptChat
    class Narrator { <<interface>> +speak() +stop() +isSupported() }
    class BrowserSpeechNarrator
    class SilentNarrator
    class useNarration { <<shared/narration>> }
    class ConceptNotesPanel { <<Concept Detail>> }

    SparkPage --> SparkSession
    SparkPage ..> sparkModes
    sparkModes ..> SparkFeedSource : creates
    SparkSession --> useSparkSession
    SparkSession --> useSwipeGestures
    SparkSession --> useRelatedStack
    SparkSession --> SparkConceptCard
    SparkSession --> RelatedCard
    RelatedCard --> SparkConceptCard
    useSparkSession --> composeFeed
    composeFeed --> feedMix
    composeFeed --> GamePicker
    GamePicker <|.. RandomNoRepeatPicker
    RandomNoRepeatPicker --> GameRegistry
    SparkConceptCard --> RevealStage
    RevealStage --> revealEffects
    SparkConceptCard --> useNarration
    useNarration --> Narrator
    Narrator <|.. BrowserSpeechNarrator
    Narrator <|.. SilentNarrator
    SparkConceptCard --> DeepDiveMenu
    DeepDiveMenu --> AskSheet
    AskSheet --> useConceptChat
```

## Known limits (accepted)

- A visual taller than the card is scrolled by finger; "Open in Library" shows it whole.
- "Related concepts" links a chat-made visual to its origin only if their texts are close enough (the Phase 6 distance cut-off); there's no explicit link table yet (fits Phase 12).
- The browser voice sounds robotic and varies by device; the AI voice is the last phase.
- Each chat question is stored in `generation_jobs` like every job (audit trail); nothing is attached to the concept unless saved.
