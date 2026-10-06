# Verify: visual failsafe

Run `mvn test` first. New test classes: `ConceptTextOutlinerTest`, `FallbackVisualBuildersTest`, `FallbackVisualizationServiceTest`, `VisualAssetGeneratorTest`, `VisualizePipelineTest`.

Then, with the api and worker running and the frontend on `npm run dev`:

| # | Do this | Expect |
|---|---|---|
| 1 | Normal run: Capture, choose **Mind map**, Visualize. | A React Flow mind map. Nodes drag; **Reset layout** appears after a drag. |
| 2 | Choose **Image / diagram**, Visualize. | The AI image, with a **Show as diagram** button that switches to a flowchart and back. |
| 3 | Set `$env:GEMINI_VISUAL_MODEL = "does-not-exist"`, restart the worker, repeat step 2. | A flowchart instead of an image. Worker log: `image generation failed (...), using the AI-written flowchart instead.` |
| 4 | Set `$env:GEMINI_API_KEY = "invalid"` (leave the paid keys unset), restart the worker. Visualize with **Mind map**, then **Animation**, then **Image / diagram**. | A mind map, a coded animation and a flowchart built from your own text. Worker log: `no AI provider produced a usable visualization (...), building one from the concept text by rules.` Each can be saved. |
| 5 | Same broken key, **Let AI choose**, with notes written as a numbered list. | An animation. With plain facts instead: a mind map. |
| 6 | Restore the real key and model. | Back to AI output. |

If a job still ends FAILED, the worker log has an ERROR line starting `Job <id>:` with the stack trace; paste it.

Startup log to check once: `Only one text generation provider is usable` is expected while only `GEMINI_API_KEY` is set.
