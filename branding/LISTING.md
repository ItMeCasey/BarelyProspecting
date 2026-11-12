# Barely Prospecting listing kit: CurseForge

Everything needed to fill in the CurseForge project form. Files are in `branding/icon/` and `branding/banners/`.

| Field | CurseForge |
|---|---|
| Project ID | 1799887 (set in `.github/scripts/publish_curseforge.py`) |
| Name | Barely Prospecting |
| Slug / URL | `barelyprospecting` |
| Mod id | `barelyprospecting` |
| Icon / logo | `icon/icon_512.png` (square, at least 400x400) |
| Class | Mods |
| Main category | Adventure and RPG, or the closest "skyblock" or "questing and progression" category the form offers |
| Loaders | Forge and NeoForge |
| Game versions | Minecraft 1.20.1, 1.21.1, 26.1.2, 26.2 and 26.3, ten jars, one per loader and Minecraft version |
| Environment | Client and Server (the upload script sends both) |
| License | MIT |
| Source / Issues | https://github.com/ItMeCasey/BarelyProspecting and `/issues` |
| Wiki | https://itmecasey.com/wiki/barelyprospecting/ |

### One-line description

> A hands-on skyblock for Forge and NeoForge: pan silt, crush rubble, soak, fire a crucible and feed a lure from a void island to the End.

### Gallery

| File | Use |
|---|---|
| `banners/features.png` (1920x1080) | First gallery image |
| `banners/gallery.png` (1920x1080) | The five pan tiers, second gallery image |
| `docs/frames/forge-26.3_*.png` | Real client screenshots (island, blocks, the panning ring, basin, crucible, lure) |
| `banners/social.png` (1280x640) | GitHub social preview |
| `banners/banner.png` (800x150, saved at 2x) | Top of the description |
| `banners/h_*.png` (800x46, saved at 2x) | Section headers (features, how, tiers, compat, faq) |

Rebuild: `python scripts/make_assets.py` (textures), `python branding/source/make_icon.py`, `python branding/source/build_banners_html.py`, `python branding/source/render_banners.py` (needs Pillow and Google Chrome).

The description is in [`curseforge/DESCRIPTION.md`](../curseforge/DESCRIPTION.md); paste it with the description type set to **Markdown**. Its images load from GitHub, so `branding/` has to be pushed first. It has no install or testing section on purpose.
