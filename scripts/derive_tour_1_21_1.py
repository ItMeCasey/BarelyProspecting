"""Derives the 1.21.1 screenshot tour (the standard mechanics tour only) from the 26.3 one. Run from the project folder."""
import re

s = open('version-26.3/src/dev/java/com/itmecasey/barelyprospecting/dev/ShotTour.java', encoding='utf-8').read()
# the compatibility tour uses 26.x-only client calls; keep only the mechanics tour
s = re.sub(r'        if \("compat"\.equals\(System\.getenv\("BARELYPROSPECTING_SHOTS"\)\)\) \{\n            planCompat\(\);\n            return;\n        \}\n', '', s)
a = s.index('    // ---- the compatibility tour')
b = s.index('    // ---- helpers ----')
s = s[:a] + s[b:]
s = s.replace('minecraft.gui.overlay()', 'minecraft.getOverlay()')
s = s.replace('minecraft.gui.screen()', 'minecraft.screen').replace('Minecraft.getInstance().gui.screen()', 'Minecraft.getInstance().screen')
s = s.replace('Minecraft.getInstance().gui.setScreen(', 'Minecraft.getInstance().setScreen(')
s = s.replace('minecraft.gui.hud.getChat()', 'minecraft.gui.getChat()').replace('minecraft.gui.toastManager()', 'minecraft.getToasts()')
s = s.replace('minecraft.gameRenderer.mainRenderTarget(), 1,', 'minecraft.getMainRenderTarget(),')
s = s.replace('m.player.getInventory().setSelectedSlot(8)', 'm.player.getInventory().selected = 8').replace('player.getInventory().setSelectedSlot(slot)', 'player.getInventory().selected = slot')
s = s.replace('player.getInventory().getSelectedSlot()', 'player.getInventory().selected')
s = s.replace('player.snapTo(', 'player.moveTo(')
s = s.replace('''        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE,
                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);''',
              '''        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                new net.minecraft.world.level.GameRules(), WorldDataConfiguration.DEFAULT);''')
s = s.replace('registries -> registries.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(preset).value().createWorldDimensions()',
              'access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(preset).value().createWorldDimensions()')
open('version-1.21.1/src/dev/java/com/itmecasey/barelyprospecting/dev/ShotTour.java', 'w', encoding='utf-8').write(s)
print('ok')
# the 1.20.1 tour is the same file with the older world-creation call
t = open('version-1.21.1/src/dev/java/com/itmecasey/barelyprospecting/dev/ShotTour.java', encoding='utf-8').read()
t = t.replace('.createWorldDimensions(), new TitleScreen());', '.createWorldDimensions());')
import os
os.makedirs('version-1.20.1/src/dev/java/com/itmecasey/barelyprospecting/dev', exist_ok=True)
open('version-1.20.1/src/dev/java/com/itmecasey/barelyprospecting/dev/ShotTour.java', 'w', encoding='utf-8').write(t)
print('1.20.1 tour written')
