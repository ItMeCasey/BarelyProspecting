"""One-off: records the integrations in the wiki draft, the description, the known-gaps notes and the status file."""
import re

W = r"G:\Other computers\My Computer\Projects\ItMeCasey\website\d-minimal\wiki\barelyprospecting\pages"


def read(p):
    return open(p, encoding='utf-8').read()


def write(p, s):
    open(p, 'w', encoding='utf-8', newline='\n').write(s)


write(W + r"\compatibility.md", """---
title: Other mods
summary: Recipe viewers and in-world info mods, and which builds each one has.
---
Barely Prospecting needs no other mod. Every integration below is optional and loads only when the other mod is present.

## What they show

- **Recipe viewers (BEI, JEI, REI, EMI):** five categories, **Panning**, **Hammering**, **Soaking**, **Melting and quenching** and **Lure**. A panning page shows one material with every drop, and the tooltip of a drop gives its chance for each of the five trays. The trays, the hammer, the basin, the crucible and the lure are the workstations.
- **In-world info (SMWTI, Jade):** a basin shows its fluid, what is soaking and the finished result; a crucible shows the fire, the lava and the rubble waiting; a lure shows its meals and the hatch timer; silt, grit and the other pannable blocks show the pans they have left, the tray they need and what they can yield. SMWTI adds progress bars.

## Which builds have what

A viewer or overlay can only be supported on a build where that mod exists. "n/a" means there is no build of the other mod for that loader and Minecraft version.

| Build | BEI | SMWTI | Jade | JEI | REI | EMI |
|---|---|---|---|---|---|---|
| NeoForge 26.3 | yes | yes | yes | yes | yes | n/a |
| NeoForge 26.2 | yes | yes | yes | yes | yes | n/a |
| NeoForge 26.1.2 | yes | yes | yes | yes | yes | n/a |
| NeoForge 1.21.1 | yes | yes | yes | yes | yes | yes |
| NeoForge 1.20.1 | yes | yes | yes | n/a | n/a | n/a |
| Forge 26.3, 26.2, 26.1.2 | yes | yes | n/a | n/a | n/a | n/a |
| Forge 1.21.1 | yes | yes | n/a | yes | n/a | n/a |
| Forge 1.20.1 | yes | yes | yes | yes | yes | yes |

"yes" means the integration is built into that jar. What was seen in a real client is on [[known-gaps]]: BEI and SMWTI on Forge 26.3, Jade and JEI on NeoForge 26.3. The rest compiles and is not yet seen.
""")

s = read(W + r"\known-gaps.md")
s = s.replace("- **No recipe viewer or in-world info integration yet** (BEI, REI, JEI, EMI, SMWTI, Jade). See [[compatibility]].",
              "- **Integrations seen in a real client:** BEI and SMWTI on Forge 26.3, Jade and JEI on NeoForge 26.3. REI and EMI, and every other build of the integrations, compile and are not seen yet. See [[compatibility]].")
s = s.replace("- **Real clients:** only Forge 26.3 has run the screenshot tour.", "- **Real clients:** Forge 26.3, NeoForge 26.3 and NeoForge 1.21.1 ran the screenshot tour.")
write(W + r"\known-gaps.md", s)
write(W + r"\faq.md", read(W + r"\faq.md").replace("**Is there a recipe viewer page?** Not yet. See [[compatibility]].",
                                                   "**Is there a recipe viewer page?** Yes, for BEI, JEI, REI and EMI where they exist for your version. See [[compatibility]]."))
write(W + r"\home.md", read(W + r"\home.md").replace("Optional: Recipe viewers (planned), in-world info mods (planned)", "Optional: BEI, JEI, REI, EMI, SMWTI, Jade"))
s = read(W + r"\versions.md")
s = s.replace("| Forge: the whole tour, see below |", "| Forge and NeoForge: the whole tour, see below |")
s = s.replace("| 1.21.1 | yes | yes | NeoForge 72, Forge 71 | not run |", "| 1.21.1 | yes | yes | NeoForge 72, Forge 71 | NeoForge: the whole tour |")
write(W + r"\versions.md", s)

p = 'curseforge/DESCRIPTION.md'
s = read(p)
s = re.sub(r"Needs no other mod\. Recipe viewer pages.*?not in this release\.",
           "Needs no other mod. Optional extras: panning, hammering, soaking, melting and lure pages in **BEI, JEI, REI and EMI**, and look-at info on basins, crucibles, lures and silt in **SMWTI and Jade**, on every build where that mod exists. The wiki lists which.", s, flags=re.S)
open(p, 'w', encoding='utf-8').write(s)

p = 'docs/known-gaps.md'
s = read(p)
a = s.index('- **Not built:**')
b = s.index('- The jars of 1.20.1')
s = s[:a] + """- **Integrations** (code in `version-<mc>/src/main` for BEI and SMWTI, `version-<mc>/src/{jade,jei,rei,emi}` where the mod has a build): seen in a real client are BEI and SMWTI on Forge 26.3 and Jade and JEI on NeoForge 26.3. REI and EMI, and every other build, compile only. n/a (no build of the other mod): Jade on Forge 26.x and Forge 1.21.1; JEI on Forge 26.x and NeoForge 1.20.1; REI on Forge 26.x, Forge 1.21.1 and NeoForge 1.20.1; EMI everywhere except NeoForge 1.21.1 and Forge 1.20.1.
- **Real clients:** Forge 26.3, NeoForge 26.3 and NeoForge 1.21.1 ran the mechanics tour (`BARELYPROSPECTING_SHOTS=1`, frames in `docs/frames`); `BARELYPROSPECTING_SHOTS=compat` is the viewer tour (26.3 only). Every other build is covered by the headless self test (72 checks) and CI.
""" + s[b:]
open(p, 'w', encoding='utf-8').write(s)

p = 'PROJECT_STATUS.md'
s = read(p)
s = re.sub(r"Summary: .*", "Summary: Barely Prospecting is built for all ten targets with the full progression, the void-island preset and BEI, JEI, REI, EMI, SMWTI and Jade integrations; the self test passes on all of them and three builds ran a real-client tour.", s, count=1)
s = s.replace("- [ ] Real-client tour on NeoForge 26.3 and one older build (w:2)", "- [x] Real-client mechanics tour on NeoForge 26.3 and NeoForge 1.21.1 (22 frames each, DONE) (w:2)")
a = s.index('## Compatibility')
b = s.index('## Documentation and listing')
s = s[:a] + """## Compatibility
- [x] BEI recipe pages (five categories) and SMWTI look-at lines with progress bars on all ten builds; seen in a real client on Forge 26.3 (frames in docs/frames); compile-checked on the other nine (w:3)
- [x] Jade on NeoForge 26.3, 26.2, 26.1.2, 1.21.1 and both 1.20.1 builds, seen in a real client on NeoForge 26.3; n/a: no Jade build for Forge 26.x or Forge 1.21.1 (w:3)
- [x] JEI on NeoForge 26.3, 26.2, 26.1.2, both 1.21.1 builds and Forge 1.20.1, all five categories seen in a real client on NeoForge 26.3; n/a: no JEI build for Forge 26.x or NeoForge 1.20.1 (w:3)
- [~] REI on NeoForge 26.x, NeoForge 1.21.1 and Forge 1.20.1 and EMI on NeoForge 1.21.1 and Forge 1.20.1: written and compile-checked, not opened in a real client; n/a: no REI or EMI build for the other builds (EMI has none for 26.x) (w:3)
- [x] Dependencies wired for all ten builds (API jars from the ItMeCasey Maven with libs/ fallback, other mods from CurseForge) and the ten new CurseForge files mirrored to the private Maven (w:2)

""" + s[b:]
open(p, 'w', encoding='utf-8').write(s)
print('ok')
