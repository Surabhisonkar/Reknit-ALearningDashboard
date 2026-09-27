# Verifying "Related concepts" (Phase F)

**Backend:** not compiled in the assistant's sandbox (Maven Central is blocked there). Every file parses, and a dependency-free type analysis found no errors in calls between the project's own classes. Run `mvn test`: `RetrievalServiceRelatedToConceptTest` is new.

**No migration, no new provider call.** The endpoint reads embeddings that `INDEX_CONCEPT` jobs already write.

## Step 1–2: endpoint
```bash
curl -s -H "Authorization: Bearer $TOKEN" $API/api/concepts/<id>/related | jq
# [ { "id", "title", "summary", "distance" } ]  nearest first, possibly []
curl -s -o /dev/null -w '%{http_code}\n' -H "Authorization: Bearer $TOKEN" $API/api/concepts/<someone-elses-id>/related   # 404
```
Watch the api log while calling it: there should be **no** Gemini request (the endpoint uses stored vectors).

## Step 5: the product check (the one that matters)
1. Save the five concepts in `learning-dashboard-frontend/scripts/related-concepts-fixtures.md` (three related body topics in different folders, and two unrelated controls).
2. Wait about 10s for the `INDEX_CONCEPT` jobs (worker log).
3. `node scripts/verify-related-concepts.mjs --token <token>` from the frontend folder.
4. Read the output:
   - **RESULT: product claim met** means Back muscles ↔ Posture link both ways and the controls stay out.
   - The script prints a **suggested `RELATED_MAX_DISTANCE`** computed from your real Gemini distances. The shipped `0.35` is an informed guess, and this replaces it with a measurement. Set it in `set-env.ps1` and restart the api.
   - If it reports that the embeddings **don't separate** the topics, a threshold can't fix that. The next lever is embedding with `taskType: SEMANTIC_SIMILARITY` (Gemini supports it). That changes every vector, though, so it needs a re-index of existing concepts. It's a deliberate follow-up, not something to flip silently.
5. Open Back muscles in the app. "Connects to" should show Posture (and probably Core stability). Open Sourdough, and there should be no section at all.
