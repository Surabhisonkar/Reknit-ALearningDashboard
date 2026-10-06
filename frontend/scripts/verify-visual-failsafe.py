"""
Browser check for the visual failsafe, against a mocked API (no backend needed).

  pip install playwright && playwright install chromium
  npm run dev                      # in another terminal, port 5173
  python scripts/verify-visual-failsafe.py ./failsafe-screenshots

Covers: the Capture type picker, React Flow mind map / flowchart (drag, reset,
phone layout), the coded animation, and every image fallback path. Exits
non-zero if any check fails. Requests to Google Fonts may be blocked in a
sandbox; those console errors are ignored.
"""
import os
import json, base64, sys, time
from playwright.sync_api import sync_playwright

SHOTS = sys.argv[1] if len(sys.argv) > 1 else "failsafe-screenshots"
os.makedirs(SHOTS, exist_ok=True)
API = "http://localhost:8080"
PNG = base64.b64decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==")

TWIN = {"type": "diagram", "version": 1, "elements": [
    {"id": "a", "elementType": "terminator", "label": "Sunlight", "accessibilityLabel": "Sunlight"},
    {"id": "b", "elementType": "process", "label": "Leaf absorbs light", "accessibilityLabel": "x"},
    {"id": "c", "elementType": "decision", "label": "Enough water?", "accessibilityLabel": "x"},
    {"id": "d", "elementType": "process", "label": "Make glucose", "accessibilityLabel": "x"},
    {"id": "e", "elementType": "note", "label": "Stomata close", "accessibilityLabel": "x"}],
    "connections": [{"id": "1", "sourceId": "a", "targetId": "b", "label": ""}, {"id": "2", "sourceId": "b", "targetId": "c", "label": ""},
                    {"id": "3", "sourceId": "c", "targetId": "d", "label": "yes"}, {"id": "4", "sourceId": "c", "targetId": "e", "label": "no"}]}

MIND = {"type": "mind_map", "version": 1, "rootLabel": "The Zeigarnik effect",
        "nodes": [{"id": "root", "label": "The Zeigarnik effect", "detail": "Unfinished tasks stay on your mind."},
                  {"id": "g1", "label": "Why it happens", "detail": ""}, {"id": "g2", "label": "How to use it", "detail": ""},
                  {"id": "n1", "label": "The brain keeps open loops active", "detail": ""},
                  {"id": "n2", "label": "Finishing releases the tension", "detail": ""},
                  {"id": "n3", "label": "Start a task before a break", "detail": ""},
                  {"id": "n4", "label": "If you feel stuck", "detail": "If you feel stuck, write the next step down"}],
        "edges": [{"sourceId": "root", "targetId": "g1"}, {"sourceId": "root", "targetId": "g2"}, {"sourceId": "g1", "targetId": "n1"},
                  {"sourceId": "g1", "targetId": "n2"}, {"sourceId": "g2", "targetId": "n3"}, {"sourceId": "g2", "targetId": "n4"},
                  {"sourceId": "n2", "targetId": "n3", "relationshipLabel": "leads to"}],
        "layoutHints": {"orientation": "radial", "rootNodeId": "root"},
        "citations": [{"nodeId": "n4", "sourceText": "If you feel stuck, write the next step down"}]}
MIND_FLAT = {"type": "mind_map", "version": 1, "rootLabel": "Cells", "nodes": [{"id": "root", "label": "Cells", "detail": ""}] +
             [{"id": f"n{i}", "label": f"Fact number {i} about the cell", "detail": ""} for i in range(1, 15)],
             "edges": [], "layoutHints": {"orientation": "radial", "rootNodeId": "root"}, "citations": []}
CHAIN = {"type": "diagram", "version": 1,
         "elements": [{"id": "start", "elementType": "terminator", "label": "Making tea", "accessibilityLabel": "x"}] +
                     [{"id": f"e{i}", "elementType": "decision" if i == 4 else "process", "label": f"Step {i}: do the thing", "x": 310, "y": 20 + i * 110, "accessibilityLabel": "x"} for i in range(1, 10)],
         "connections": [{"id": "c0", "sourceId": "start", "targetId": "e1"}] + [{"id": f"c{i}", "sourceId": f"e{i}", "targetId": f"e{i+1}"} for i in range(1, 9)]}
def scenes(artifact=None):
    return {"type": "animation", "version": 1, "scenes": [
        {"id": "s1", "order": 0, "title": "Boil the water", "narration": "Boil the water", "durationSeconds": 2, "transitionToNext": "fade", "assetArtifactIds": [artifact] if artifact else []},
        {"id": "s2", "order": 1, "title": "Add the tea leaves", "narration": "Two grams per cup is plenty.", "durationSeconds": 2, "transitionToNext": "fade", "assetArtifactIds": []},
        {"id": "s3", "order": 2, "title": "Wait three minutes", "narration": "Wait three minutes.", "durationSeconds": 30, "transitionToNext": "slide", "assetArtifactIds": []}]}
def image(artifact, twin=True):
    v = {"type": "image", "version": 1, "imagePrompt": "leaf", "artifactId": artifact, "altText": "A leaf in sunlight"}
    if twin: v["fallbackDiagram"] = TWIN
    return v

state = {"viz": MIND, "posts": [], "n": 0}
errors = []
results = []
def ok(cond, what):
    results.append((bool(cond), what))
    print(("PASS " if cond else "FAIL ") + what)

def handle(route):
    req = route.request; url = req.url; path = url[len(API):]
    if req.method == "OPTIONS":
        return route.fulfill(status=204, headers={"Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "*", "Access-Control-Allow-Methods": "*"})
    cors = {"Access-Control-Allow-Origin": "*"}
    def js(body, status=200): route.fulfill(status=status, headers=cors, content_type="application/json", body=json.dumps(body))
    if path == "/api/jobs/visualize" and req.method == "POST":
        state["posts"].append(json.loads(req.post_data)); state["n"] += 1
        return js({"id": f"job-{state['n']}", "status": "PENDING"}, 202)
    if path.startswith("/api/jobs/"):
        v = state["viz"]
        return js({"id": path.split("/")[-1], "status": "COMPLETED", "resultPayload": {"kind": "DRAFT", "title": "Draft title", "summary": "A one line summary.", "suggestedFolder": "", "visualizationType": v["type"], "visualization": v, "artifactIds": []}})
    if path == "/api/artifacts/good": return js({"downloadUrl": "https://img.test/ok.png"})
    if path == "/api/artifacts/brokenimg": return js({"downloadUrl": "https://img.test/broken.png"})
    if path == "/api/artifacts/nourl": return js({"error": "Artifact not found"}, 404)
    return js({})

def visualize(page, viz, choice=None):
    state["viz"] = viz
    page.goto("http://localhost:5173/create"); page.wait_for_selector("textarea")
    page.fill("input", "Topic"); page.fill("textarea", "Some notes that are long enough.")
    if choice: page.get_by_role("radio", name=choice).check()
    page.get_by_role("button", name="Visualize this").click()
    page.wait_for_selector(".draft-preview")

with sync_playwright() as p:
    browser = p.chromium.launch()
    ctx = browser.new_context(viewport={"width": 1280, "height": 1000})
    ctx.add_init_script("sessionStorage.setItem('auth_session', JSON.stringify({accessToken:'t', idToken:'a.' + btoa(JSON.stringify({name:'Test', email:'t@t.t'})) + '.c', refreshToken:'r', expiresAtMs: Date.now() + 3600e3}))")
    ctx.route(API + "/**", handle)
    ctx.route("https://img.test/ok.png", lambda r: r.fulfill(status=200, content_type="image/png", body=PNG))
    ctx.route("https://img.test/broken.png", lambda r: r.fulfill(status=404, body="nope"))
    page = ctx.new_page()
    page.on("console", lambda m: errors.append(m.text) if m.type == "error" else None)
    page.on("pageerror", lambda e: errors.append("PAGEERROR " + str(e)))

    # 1. type picker default + mind map
    visualize(page, MIND)
    ok(state["posts"][-1]["preferredVisualizationType"] == "auto", "default type sent is auto")
    page.wait_for_selector(".mindmap-node")
    page.wait_for_timeout(600)
    ok(page.locator(".mindmap-node").count() == 7, "mind map renders 7 nodes")
    ok(page.locator(".react-flow__edge").count() == 7, "mind map renders 6 branches + 1 cross link")
    boxes = [page.locator(".mindmap-node").nth(i).bounding_box() for i in range(7)]
    def overlap(a, b): return not (a["x"] + a["width"] <= b["x"] or b["x"] + b["width"] <= a["x"] or a["y"] + a["height"] <= b["y"] or b["y"] + b["height"] <= a["y"])
    ok(not any(overlap(boxes[i], boxes[j]) for i in range(7) for j in range(i + 1, 7)), "mind map nodes do not overlap")
    page.locator(".draft-preview").screenshot(path=f"{SHOTS}/1-mindmap.png")
    page.get_by_text("If you feel stuck", exact=True).click()
    ok(page.locator(".mindmap-inspector").inner_text().count("write the next step down") == 1, "inspector shows detail once (no duplicate citation)")
    ok(page.locator(".graph-reset").count() == 0, "no reset button before dragging")
    b = page.get_by_text("Start a task before a break", exact=True).bounding_box()
    page.mouse.move(b["x"] + 20, b["y"] + 20); page.mouse.down(); page.mouse.move(b["x"] + 120, b["y"] + 90, steps=8); page.mouse.up()
    b2 = page.get_by_text("Start a task before a break", exact=True).bounding_box()
    ok(abs(b2["x"] - b["x"]) > 40, "node can be dragged")
    ok(page.locator(".graph-reset").count() == 1, "reset button appears after drag")
    page.locator(".graph-reset").click(); page.wait_for_timeout(500)
    ok(page.locator(".graph-reset").count() == 0, "reset button hides after reset")
    b3 = page.get_by_text("Start a task before a break", exact=True).bounding_box()
    ok(abs(b3["x"] - b["x"]) < 3 and abs(b3["y"] - b["y"]) < 3, "reset restores the original position")
    # page scroll not trapped by canvas
    y0 = page.evaluate("window.scrollY"); c = page.locator(".graph-canvas").bounding_box()
    page.mouse.move(c["x"] + c["width"] / 2, c["y"] + 100); page.mouse.wheel(0, 300); page.wait_for_timeout(300)
    ok(page.evaluate("window.scrollY") > y0, "wheel over the canvas still scrolls the page")

    # regenerate keeps the chosen type
    visualize(page, scenes(), "Animation")
    ok(state["posts"][-1]["preferredVisualizationType"] == "animation", "chosen type 'animation' is sent")
    page.get_by_role("button", name="Regenerate").click(); page.wait_for_timeout(2500)
    ok(len(state["posts"]) >= 3 and state["posts"][-1]["preferredVisualizationType"] == "animation", "regenerate re-sends the chosen type")

    # 2. animation, coded motion
    visualize(page, scenes(), "Animation")
    page.wait_for_selector(".scene-motion")
    ok(page.locator(".scene-motion-number").text_content() == "1", "coded scene shows step 1")
    ok(page.locator(".scene-narration").count() == 0, "narration identical to title is not repeated")
    page.wait_for_timeout(900); page.locator(".draft-preview").screenshot(path=f"{SHOTS}/2-animation-step1.png")
    page.wait_for_timeout(1500)
    ok(page.locator(".scene-motion-number").text_content() == "2", "autoplay advances to step 2")
    ok(page.locator(".scene-narration").inner_text() == "Two grams per cup is plenty.", "distinct narration is shown")
    page.wait_for_timeout(2100)
    ok(page.locator(".scene-motion-number").text_content() == "3", "autoplay advances to step 3")
    page.wait_for_timeout(900); page.locator(".draft-preview").screenshot(path=f"{SHOTS}/3-animation-step3.png")
    page.get_by_role("button", name="Pause").click()
    ok(page.locator(".scene-motion-ring-fill").evaluate("e => getComputedStyle(e).animationPlayState") == "paused", "pausing pauses the timer ring")

    # scene picture OK / broken
    visualize(page, scenes("good"), "Animation"); page.wait_for_selector(".scene-image")
    ok(page.locator(".scene-motion").count() == 0, "scene with a working picture shows the picture")
    visualize(page, scenes("brokenimg"), "Animation"); page.wait_for_selector(".scene-motion")
    ok(True, "scene whose picture fails to load falls back to coded motion")
    visualize(page, scenes("nourl"), "Animation"); page.wait_for_selector(".scene-motion")
    ok(True, "scene whose picture URL can't be fetched falls back to coded motion")

    # 3. image + toggle
    visualize(page, image("good"), "Image / diagram")
    ok(state["posts"][-1]["preferredVisualizationType"] == "image", "chosen type 'image' is sent")
    page.wait_for_selector("img.image-renderer")
    ok(page.locator(".diagram-node").count() == 0, "image shown first, not the diagram")
    page.get_by_role("button", name="Show as diagram").click(); page.wait_for_selector(".diagram-node"); page.wait_for_timeout(600)
    ok(page.locator(".diagram-node").count() == 5, "toggle shows the 5-element diagram twin")
    ok(page.locator(".react-flow__edge").count() == 4, "diagram twin has 4 arrows")
    page.locator(".draft-preview").screenshot(path=f"{SHOTS}/4-image-as-diagram.png")
    page.get_by_role("button", name="Show image").click(); page.wait_for_selector("img.image-renderer")
    ok(True, "toggle back shows the image")
    visualize(page, image("good", twin=False), "Image / diagram"); page.wait_for_selector("img.image-renderer")
    ok(page.get_by_role("button", name="Show as diagram").count() == 0, "no toggle when an image has no diagram twin (old concepts)")
    visualize(page, image("brokenimg"), "Image / diagram"); page.wait_for_selector(".diagram-node")
    ok(page.locator("img.image-renderer").count() == 0, "image that fails to load -> diagram automatically")
    visualize(page, image("nourl"), "Image / diagram"); page.wait_for_selector(".diagram-node")
    ok(True, "image whose URL can't be fetched -> diagram automatically")
    visualize(page, image("nourl", twin=False), "Image / diagram"); page.wait_for_selector(".image-renderer-error")
    ok(True, "no twin + no image -> message, as before")

    # 4. diagrams
    visualize(page, CHAIN, "Image / diagram"); page.wait_for_selector(".diagram-node"); page.wait_for_timeout(600)
    ok(page.locator(".diagram-node").count() == 10, "10-step chain renders")
    boxes = [page.locator(".diagram-node").nth(i).bounding_box() for i in range(10)]
    ok(not any(overlap(boxes[i], boxes[j]) for i in range(10) for j in range(i + 1, 10)), "chain nodes do not overlap")
    ok(len({round(b["y"]) for b in boxes}) == 3, "long chain is folded into 3 rows")
    page.locator(".draft-preview").screenshot(path=f"{SHOTS}/5-chain.png")
    visualize(page, MIND_FLAT, "Mind map"); page.wait_for_selector(".mindmap-node"); page.wait_for_timeout(600)
    ok(state["posts"][-1]["preferredVisualizationType"] == "mind_map", "chosen type 'mind_map' is sent")
    boxes = [page.locator(".mindmap-node").nth(i).bounding_box() for i in range(15)]
    ok(not any(overlap(boxes[i], boxes[j]) for i in range(15) for j in range(i + 1, 15)), "15-node edge-less mind map does not overlap")
    ok(page.locator(".react-flow__edge").count() == 14, "edge-less mind map still connects every node to the root")
    page.locator(".draft-preview").screenshot(path=f"{SHOTS}/6-mindmap-15.png")
    page.locator(".generate-actions").screenshot(path=f"{SHOTS}/7-type-picker.png")

    # 5. mobile
    m = browser.new_context(viewport={"width": 390, "height": 844}, is_mobile=True, has_touch=True)
    m.add_init_script("sessionStorage.setItem('auth_session', JSON.stringify({accessToken:'t', idToken:'a.b.c', refreshToken:'r', expiresAtMs: Date.now() + 3600e3}))")
    m.route(API + "/**", handle)
    mp = m.new_page(); mp.on("pageerror", lambda e: errors.append("PAGEERROR " + str(e)))
    visualize(mp, MIND); mp.wait_for_selector(".mindmap-node"); mp.wait_for_timeout(600)
    ok(mp.evaluate("document.documentElement.scrollWidth <= window.innerWidth"), "no horizontal page scroll on mobile")
    mp.locator(".generate-actions").screenshot(path=f"{SHOTS}/8-mobile-picker.png")
    mp.locator(".draft-preview").screenshot(path=f"{SHOTS}/9-mobile-mindmap.png")
    mb = [mp.locator(".mindmap-node").nth(i).bounding_box() for i in range(7)]
    ok(not any(overlap(mb[i], mb[j]) for i in range(7) for j in range(i + 1, 7)), "mobile outline nodes do not overlap")
    ok(min(b["width"] for b in mb) > 110, "mobile outline nodes stay readable (%d px wide)" % min(b["width"] for b in mb))
    ok(mp.locator(".react-flow__edge").count() == 6, "mobile outline draws the 6 branches, no cross links")
    visualize(mp, CHAIN, "Image / diagram"); mp.wait_for_selector(".diagram-node"); mp.wait_for_timeout(600)
    mb = [mp.locator(".diagram-node").nth(i).bounding_box() for i in range(10)]
    ok(not any(overlap(mb[i], mb[j]) for i in range(10) for j in range(i + 1, 10)), "mobile chain nodes do not overlap")
    mp.locator(".draft-preview").screenshot(path=f"{SHOTS}/11-mobile-chain.png")
    visualize(mp, scenes(), "Animation"); mp.wait_for_selector(".scene-motion"); mp.wait_for_timeout(900)
    mp.locator(".draft-preview").screenshot(path=f"{SHOTS}/10-mobile-animation.png")

    real = [e for e in errors if "img.test/broken" not in e and "404" not in e and "ERR_TUNNEL_CONNECTION_FAILED" not in e]
    ok(not real, "no console/page errors" + (": " + " | ".join(real[:5]) if real else ""))
    browser.close()

failed = [w for c, w in results if not c]
print(f"\n{len(results) - len(failed)}/{len(results)} passed")
sys.exit(1 if failed else 0)
