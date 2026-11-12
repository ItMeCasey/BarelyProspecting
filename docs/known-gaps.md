# Known gaps

The user-facing list is the wiki draft (`wiki/barelyprospecting/pages/known-gaps.md` in the site folder). Short version:

- **Not built:** recipe viewer integrations (BEI, REI, JEI, EMI) and in-world info (SMWTI, Jade). Per the playbook they apply on every version where the other mod has a build; none of those builds has been looked up with the `curseforge-lookup` skill yet.
- **Real clients:** only Forge 26.3 ran the screenshot tour (`BARELYPROSPECTING_SHOTS`, frames in `docs/frames`). The other nine builds are covered by the headless self test (72 checks) and, in CI, the unit tests. The older client calls for 26.1.2 are already in its tour; 1.21.1 and 1.20.1 have an empty tour on purpose.
- The jars of 1.20.1 hold SRG names (`verify_jars.py`: 10 ok), but they were not loaded in a real Forge 47.4.28 or NeoForge 47.1.106 install.
- The progress "ring" is drawn with particles around the block, not as a screen overlay.
- Balance is a first pass and was never played through.
- Multiplayer with real connections is untested.
- The CurseForge project id is 1799887.
