"""Builds banners.html from the shared studio style (copied from Barely Pipes' banners.html head and pixel font) plus the
Barely Prospecting scenes, which draw the mod's own textures. Run once; edit banners.html afterwards if you like.

python branding/source/build_banners_html.py
"""
from pathlib import Path

HERE = Path(__file__).resolve().parent
SRC = Path(r"C:\Users\casey\Desktop\BarelyPipes\branding\source\banners.html").read_text(encoding="utf-8")
head = SRC[: SRC.index("// ---------- helpers")]
head = head.replace("Renders the Barely Pipes banners", "Renders the Barely Prospecting banners")

body = r'''// ---------- helpers ----------
const TEX = "../../common/src/main/resources/assets/barelyprospecting/textures/";
function half(c) { c.style.width = c.width / 2 + "px"; c.style.height = c.height / 2 + "px"; return c; }
function el(tag, attrs = {}, ...kids) {
  const e = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs)) k === "style" ? (e.style.cssText = v) : e.setAttribute(k, v);
  for (const k of kids) e.append(k);
  return e;
}
function iconImg(px) { return el("img", {class: "icon", src: "../icon/icon_512.png", width: px, height: px}); }
function tex(path, px, extra = "") {
  return el("img", {src: TEX + path + ".png", width: px, height: px, style: `image-rendering:pixelated;${extra}`});
}
function pills(list, style = "") {
  const m = el("div", {class: "meta", style});
  for (const [t, on] of list) m.append(el("span", {class: "pill" + (on ? " on" : "")}, t));
  return m;
}
// One step of the chain: a picture, a name and what it does.
function step(img, title, line) {
  return el("div", {class: "panel", style: "min-width:0;width:340px;gap:10px;align-items:center;text-align:center"},
    img, el("div", {class: "name", style: "color:#ffe28c"}, title), el("div", {class: "line"}, line));
}
function arrow() { return el("div", {style: "font-size:44px;color:#f2b33d;align-self:center"}, "\u25B6"); }

const ASSETS = {
  banner: [800, 150, (r) => {
    r.style.cssText += "display:flex;align-items:center;gap:24px;padding:0 28px";
    r.append(iconImg(104));
    r.append(el("div", {style: "display:flex;flex-direction:column;align-items:flex-start;gap:11px;flex:1"},
      half(pixelText("BarelyProspecting", 8)),
      el("div", {class: "tag", style: "font-size:16px"}, "Skyblock by hand. ", el("b", {}, "Pan, crush, soak, fire and feed.")),
      el("div", {class: "meta", style: "gap:6px"},
        el("span", {class: "pill on", style: "font-size:11px;padding:3px 9px"}, "Forge"),
        el("span", {class: "pill on", style: "font-size:11px;padding:3px 9px"}, "NeoForge"),
        el("span", {class: "pill", style: "font-size:11px;padding:3px 9px"}, "Void island preset"),
        el("span", {class: "pill", style: "font-size:11px;padding:3px 9px"}, "Five pan tiers"))));
    r.append(el("div", {style: "display:flex;gap:6px;align-items:center"}, tex("item/wooden_panning_tray", 64), tex("item/pick_hammer", 64)));
  }],
  social: [1280, 640, (r) => {
    r.style.cssText += "display:flex;flex-direction:column;align-items:center;justify-content:center;gap:24px;text-align:center";
    r.append(iconImg(190));
    r.append(pixelText("BarelyProspecting", 9));
    r.append(el("div", {class: "tag"}, "A hands-on skyblock progression for Forge and NeoForge."));
    r.append(pills([["Forge", 1], ["NeoForge", 1], ["Panning"], ["Crucible"], ["Void island preset"]]));
  }],
  features: [1920, 1080, (r) => {
    r.style.cssText += "display:flex;flex-direction:column;align-items:center;justify-content:center;padding:40px 60px;gap:44px";
    r.append(el("div", {style: "display:flex;align-items:center;gap:30px"}, iconImg(110), pixelText("BarelyProspecting", 7)));
    r.append(el("div", {class: "tag", style: "font-size:34px;text-align:center"}, "Every step is a hand on a block, and every result feeds the next step."));
    r.append(el("div", {style: "display:flex;gap:18px;align-items:stretch"},
      step(tex("block/silt_0", 120), "Hammer", "Dirt breaks into silt, stone into rubble, grit, fine grit."), arrow(),
      step(tex("item/copper_panning_tray", 120), "Pan", "Hold a tray on a block until the ring closes."), arrow(),
      step(tex("block/lure_2", 120), "Soak, fire, feed", "A basin, a crucible and a lure you fill by hand."),));
    r.append(pills([["Forge", 1], ["NeoForge", 1], ["Five pan tiers"], ["Nether and End silt"], ["MIT license"]]));
  }],
  gallery: [1920, 1080, (r) => {
    r.style.cssText += "display:flex;flex-direction:column;align-items:center;justify-content:center;gap:44px";
    r.append(el("div", {style: "display:flex;align-items:center;gap:30px"}, iconImg(80), pixelText("Five pan tiers", 8)));
    const row = el("div", {style: "display:flex;gap:24px"});
    [["wooden", "Silt and the first grit: flint, seeds, saplings, coal dust, copper."],
     ["copper", "Iron flakes, redstone and a quicker ring."],
     ["iron", "Gold flakes and lapis; two rolls a pan."],
     ["gold", "Diamond shards from grit and Nether finds."],
     ["diamond", "Three rolls a pan, netherite scrap and End silt."]].forEach(([t, v]) => row.append(
      el("div", {class: "panel", style: "min-width:0;width:340px;gap:12px;align-items:center;text-align:center"},
        tex("item/" + t + "_panning_tray", 110), el("div", {class: "name", style: "color:#ffe28c"}, t[0].toUpperCase() + t.slice(1)), el("div", {class: "line"}, v))));
    r.append(row);
    const mats = el("div", {style: "display:flex;gap:14px"});
    ["silt", "grit", "fine_grit", "nether_silt", "end_silt"].forEach((m) => mats.append(tex("block/" + m + "_0", 96)));
    r.append(el("div", {class: "tag", style: "font-size:26px"}, "Each material wears down in four pans."), mats);
  }],
};
for (const [k, label] of Object.entries({features: "Features", how: "How it works", tiers: "Pan tiers", compat: "Compatibility", faq: "FAQ"})) {
  ASSETS["h_" + k] = [800, 46, (r) => {
    r.style.cssText += "display:flex;align-items:center;gap:14px;padding:0 16px;border-top:2px solid var(--gold)";
    r.append(iconImg(30));
    r.append(half(pixelText(label, 5)));
  }];
}

const name = location.hash.slice(1) || "banner";
const [w, h, build] = ASSETS[name];
const root = document.getElementById("root");
root.className = "stage";
root.style.cssText = `width:${w}px;height:${h}px`;
build(root);
window.ASSET_NAMES = Object.keys(ASSETS);
</script>
</body>
</html>
'''
(HERE / "banners.html").write_text(head + body, encoding="utf-8")
print("wrote banners.html")
