"""One-off: records the real Forge 47.4.28 and NeoForge 47.1.106 jar loads in the status file, the notes and the wiki draft."""
p = 'PROJECT_STATUS.md'
s = open(p, encoding='utf-8').read()
old = "- [x] Forge and NeoForge 1.20.1: self test, jars hold SRG names (verify_jars: 10 ok); not loaded in a real Forge 47.4.28 or NeoForge 47.1.106 install (w:3)"
new = ("- [x] Forge and NeoForge 1.20.1: self test, jars hold SRG names (verify_jars: 10 ok); the jars were loaded in a real Forge 47.4.28 and a real NeoForge 47.1.106 install "
       "(our own game folders, real libraries, through launch-isolated.ps1) and ran the whole mechanics tour, 22 frames each (docs/frames/real-forge-47.4.28_*, real-neoforge-47.1.106_*) (w:3)")
assert old in s
open(p, 'w', encoding='utf-8').write(s.replace(old, new))

p = 'docs/known-gaps.md'
s = open(p, encoding='utf-8').read()
s = s.replace("- The jars of 1.20.1 hold SRG names (`verify_jars.py`: 10 ok), but they were not loaded in a real Forge 47.4.28 or NeoForge 47.1.106 install.",
              "- The 1.20.1 jars hold SRG names (`verify_jars.py`: 10 ok) and were loaded in a real Forge 47.4.28 and a real NeoForge 47.1.106 install (mechanics tour, frames `real-forge-47.4.28_*` and `real-neoforge-47.1.106_*`).")
open(p, 'w', encoding='utf-8').write(s)

W = r"G:\Other computers\My Computer\Projects\ItMeCasey\website\d-minimal\wiki\barelyprospecting\pages\versions.md"
t = open(W, encoding='utf-8').read()
t = t.replace("The 1.20.1 jars contain the game's real (SRG) names.",
              "The 1.20.1 jars contain the game's real (SRG) names, and both were loaded in a real Forge 47.4.28 and a real NeoForge 47.1.106 install and ran the whole tour.")
t = t.replace("| 1.20.1 | yes | yes | Forge 72, NeoForge 71 | not run |", "| 1.20.1 | yes | yes | Forge 72, NeoForge 71 | both, in real Forge 47.4.28 and NeoForge 47.1.106 installs |")
open(W, 'w', encoding='utf-8', newline='\n').write(t)
print('ok')
