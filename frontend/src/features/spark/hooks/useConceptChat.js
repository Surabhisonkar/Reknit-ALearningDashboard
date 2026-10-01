import { useCallback, useState } from "react";

import { askConcept, saveChatAsNote } from "../../../api/conceptChatApi.js";
import { pollJob } from "../../../api/jobsApi.js";

/** Capture accepts at most this much text (VisualizeRequest), so "Make a visual" trims to fit. */
export const MAX_VISUAL_INPUT_CHARS = 6000;
/** The server's question limit (ConceptAskRequest). */
export const MAX_QUESTION_CHARS = 1000;
/**
 * How much recent history each request carries. The model only sees the last
 * 12 messages (ChatHistoryPolicy), so sending more would just grow requests;
 * the server rejects over 200.
 */
const HISTORY_SENT = 24;
const MAX_TOPIC_CHARS = 120;

/**
 * A short back-and-forth with the AI about one concept. Nothing is kept on
 * the concept unless the user saves the chat as a note (condensed by the AI)
 * or turns it into a new concept via Capture.
 */
export function useConceptChat(accessToken, concept) {
  const [messages, setMessages] = useState([]); // [{ role: "user" | "assistant", text }]
  const [status, setStatus] = useState("idle"); // idle | thinking | error | saving | saved
  const [error, setError] = useState(null);

  const send = useCallback(
    async (rawQuestion) => {
      const question = rawQuestion.trim();
      if (!question || status === "thinking" || status === "saving") return;
      const history = messages;
      setMessages([...history, { role: "user", text: question }]);
      setStatus("thinking");
      setError(null);
      try {
        const job = await askConcept(accessToken, concept.id, { history: history.slice(-HISTORY_SENT), question });
        const done = await pollJob(accessToken, job.id);
        setMessages((prev) => [...prev, { role: "assistant", text: done.resultPayload?.answer ?? "" }]);
        setStatus("idle");
      } catch (err) {
        setError(err?.message ?? "The AI couldn't answer just now. Try again.");
        setStatus("error");
      }
    },
    [accessToken, concept.id, messages, status],
  );

  const saveAsNote = useCallback(async () => {
    setStatus("saving");
    setError(null);
    try {
      const job = await saveChatAsNote(accessToken, concept.id, { history: messages.slice(-HISTORY_SENT) });
      await pollJob(accessToken, job.id);
      setStatus("saved");
    } catch (err) {
      setError(err?.message ?? "The note wasn't saved. Try again.");
      setStatus("error");
    }
  }, [accessToken, concept.id, messages]);

  /** Capture's pre-fill for "Make a visual": the latest exchanges that fit, oldest first. */
  const toCapturePrefill = useCallback(() => {
    const lines = messages.map((m) => `${m.role === "user" ? "Q" : "A"}: ${m.text}`);
    const kept = [];
    let length = 0;
    for (let i = lines.length - 1; i >= 0; i--) {
      if (length + lines[i].length + 2 > MAX_VISUAL_INPUT_CHARS) break;
      kept.unshift(lines[i]);
      length += lines[i].length + 2;
    }
    const firstQuestion = messages.find((m) => m.role === "user")?.text ?? "";
    const title = `${concept.title}: ${firstQuestion}`.slice(0, MAX_TOPIC_CHARS);
    return { title, description: kept.join("\n\n") };
  }, [concept.title, messages]);

  const hasAnswer = messages.some((m) => m.role === "assistant");

  return { messages, status, error, hasAnswer, send, saveAsNote, toCapturePrefill };
}
