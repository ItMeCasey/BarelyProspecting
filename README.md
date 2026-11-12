# Barely Prospecting

A skyblock progression mod for Forge and NeoForge. You start on a tiny island in the void and refine everything by hand: hammer rubble, pan silt, crush grit and pan it again, soak, fire and feed. Every step is a manual action on a block, and each result feeds the next step.

- **Versions:** Minecraft 26.3, 26.2, 26.1.2, 1.21.1 and 1.20.1, on Forge and NeoForge (ten jars). Version `2026.11.1`.
- **Design:** [SPEC.md](SPEC.md), the build notes in [docs/DESIGN.md](docs/DESIGN.md).
- **Wiki:** https://itmecasey.com/wiki/barelyprospecting/
- **Status:** [PROJECT_STATUS.md](PROJECT_STATUS.md) and [docs/known-gaps.md](docs/known-gaps.md).

## Install

Put the jar for your Minecraft version and loader in `mods`, on the server and on every client. Pick the **Void Island** world type for the starter island.

## Build

Each Minecraft version and loader is its own Gradle build (`forge-26.3`, `neoforge-1.21.1`, and so on); shared rules live in `common/`, the game code in `version-<mc>/`.

```
cd forge-26.3
./gradlew build                          # jar and unit tests
./gradlew runGameTestServer -PdevTools   # headless self test, prints SELFTEST DONE failures=0
```

Textures, models, recipes and the language file are generated: `python scripts/make_assets.py` (needs Pillow). The wiki drop-table page is generated from the code with `python scripts/wiki_tables.py`.

## License

MIT, see [LICENSE](LICENSE).
