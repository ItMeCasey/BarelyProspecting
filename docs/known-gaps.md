# Known gaps

The user-facing list is the wiki draft (`wiki/barelyprospecting/pages/known-gaps.md` in the site folder). Short version:

- **Integrations** (code in `version-<mc>/src/main` for BEI and SMWTI, `version-<mc>/src/{jade,jei,rei,emi}` where the mod has a build): seen in a real client are BEI and SMWTI on Forge 26.3 and Jade and JEI on NeoForge 26.3. REI and EMI, and every other build, compile only. n/a (no build of the other mod): Jade on Forge 26.x and Forge 1.21.1; JEI on Forge 26.x and NeoForge 1.20.1; REI on Forge 26.x, Forge 1.21.1 and NeoForge 1.20.1; EMI everywhere except NeoForge 1.21.1 and Forge 1.20.1.
- **Real clients:** Forge 26.3, NeoForge 26.3 and NeoForge 1.21.1 ran the mechanics tour (`BARELYPROSPECTING_SHOTS=1`, frames in `docs/frames`); `BARELYPROSPECTING_SHOTS=compat` is the viewer tour (26.3 only). Every other build is covered by the headless self test (72 checks) and CI.
- The jars of 1.20.1 hold SRG names (`verify_jars.py`: 10 ok), but they were not loaded in a real Forge 47.4.28 or NeoForge 47.1.106 install.
- The progress "ring" is drawn with particles around the block, not as a screen overlay.
- Balance is a first pass and was never played through.
- Multiplayer with real connections is untested.
- The CurseForge project id is 1799887.
