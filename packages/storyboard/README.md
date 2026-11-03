# @reknit/storyboard

The Remotion composition for animation storyboards, shared by:

- `frontend/`, which plays it live with `@remotion/player`, and
- `animation-renderer/`, which renders it to the MP4 the backend stores in S3.

Keeping one copy means the video and the in-app preview always match.

| Export | What it is |
|---|---|
| `StoryboardVideo` | The composition. Props: `{ scenes, assetUrls }` |
| `mapAnimation(payload)` | Turns the backend's animation payload into `{ scenes, ... }` for `StoryboardVideo`. Also at `@reknit/storyboard/mapAnimation`, which pulls in no Remotion code |
| `storyboardTiming(scenes)` | Scene start frames and total length |
| `LAYOUTS`, `layoutElements(scene)` | Scene layouts (`steps`, `cycle`, `compare`, `before_after`, `focus`); a scene that names one has its elements placed for it instead of using their own x/y |
| `FPS`, `WIDTH`, `HEIGHT`, `TRANSITION_FRAMES` | Video settings |

Beyond the backend's storyboard fields, the composition reads two optional ones: `layout` on a scene and `icon` (an emoji drawn above the text) on a box or circle. Arrows with `text` get a label, and the `highlight` animation pops an element in with a pulsing ring.

Both apps depend on it as `"@reknit/storyboard": "file:../packages/storyboard"`. It lists `react` and `remotion` as peer dependencies and installs nothing itself, so each app supplies its own single copy. Code here may import only `react`, `remotion` and files in this package.

After changing a file here there is nothing to rebuild: both apps read the source directly.
