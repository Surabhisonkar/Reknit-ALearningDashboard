import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";

import { useNarration } from "../../../shared/narration";
import { Button, Modal, SvgIcon } from "../../../shared/ui";
import { cx } from "../../../shared/utils/classNames.js";
import { MAX_QUESTION_CHARS, useConceptChat } from "../hooks/useConceptChat.js";

/**
 * "Ask the AI" as a bottom sheet over the card: a normal chat. Once there's
 * an answer, the user decides - save a condensed note on the concept, turn
 * the chat into a new visual (via Capture), or discard it. Closing with an
 * unsaved chat asks first.
 */
export default function AskSheet({ accessToken, concept, onClose }) {
  const chat = useConceptChat(accessToken, concept);
  const narration = useNarration();
  const navigate = useNavigate();
  const [draft, setDraft] = useState("");
  const [confirmingDiscard, setConfirmingDiscard] = useState(false);
  const listRef = useRef(null);

  useEffect(() => {
    const list = listRef.current;
    if (list) list.scrollTop = list.scrollHeight;
  }, [chat.messages.length, chat.status]);

  const unsaved = chat.messages.length > 0 && chat.status !== "saved";

  function requestClose() {
    if (unsaved) setConfirmingDiscard(true);
    else onClose();
  }

  function submit(event) {
    event.preventDefault();
    chat.send(draft);
    setDraft("");
  }

  function makeVisual() {
    navigate("/create", { state: { prefill: chat.toCapturePrefill() } });
  }

  const busy = chat.status === "thinking" || chat.status === "saving";

  return (
    <Modal title={`Ask about “${concept.title}”`} onClose={requestClose} variant="sheet">
      <div className="chat-log" ref={listRef} aria-live="polite">
        {chat.messages.length === 0 && <p className="chat-empty">Ask anything about this concept.</p>}
        {chat.messages.map((message, i) => (
          <div key={i} className={cx("chat-bubble", message.role === "user" ? "chat-bubble-user" : "chat-bubble-ai")}>
            <p>{message.text}</p>
            {message.role === "assistant" && narration.supported && (
              <button
                type="button"
                className="chat-read-aloud"
                aria-label="Read this answer aloud"
                title="Read aloud"
                onClick={() => narration.speak(message.text)}
              >
                <SvgIcon name="speaker" size={16} />
              </button>
            )}
          </div>
        ))}
        {chat.status === "thinking" && (
          <div className="chat-bubble chat-bubble-ai chat-thinking" aria-label="The AI is thinking">
            <span />
            <span />
            <span />
          </div>
        )}
      </div>

      {chat.error && (
        <p className="form-error" role="alert">
          {chat.error}
        </p>
      )}
      {chat.status === "saved" && <p className="chat-saved">Saved to this concept's Notes.</p>}

      {confirmingDiscard ? (
        <div className="chat-confirm">
          <p>Discard this chat? It isn't saved anywhere.</p>
          <div className="modal-actions">
            <Button secondary small onClick={() => setConfirmingDiscard(false)} autoFocus>
              Keep chatting
            </Button>
            <Button small className="button-danger" onClick={onClose}>
              Discard
            </Button>
          </div>
        </div>
      ) : (
        <>
          <form className="chat-input" onSubmit={submit}>
            <label className="visually-hidden" htmlFor="chat-question">
              Your question
            </label>
            <input
              id="chat-question"
              className="modal-input"
              value={draft}
              onChange={(event) => setDraft(event.target.value)}
              placeholder="Ask a question"
              maxLength={MAX_QUESTION_CHARS}
              autoComplete="off"
              disabled={chat.status === "saving"}
              autoFocus
            />
            <button type="submit" className="spark-icon-button spark-icon-button-accent" aria-label="Send" title="Send" disabled={!draft.trim() || busy}>
              <SvgIcon name="send" />
            </button>
          </form>
          {chat.hasAnswer && chat.status !== "saved" && (
            <div className="chat-actions">
              <Button secondary small onClick={chat.saveAsNote} disabled={busy}>
                <SvgIcon name="note" size={16} /> Save as note
              </Button>
              <Button secondary small onClick={makeVisual} disabled={busy}>
                <SvgIcon name="image" size={16} /> Make a visual
              </Button>
              <Button secondary small onClick={() => setConfirmingDiscard(true)} disabled={busy}>
                <SvgIcon name="trash" size={16} /> Discard
              </Button>
            </div>
          )}
        </>
      )}
    </Modal>
  );
}
