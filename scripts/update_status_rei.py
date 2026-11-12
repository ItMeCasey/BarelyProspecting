"""One-off: marks REI and EMI as seen in real clients in the status file, the known-gaps notes and the wiki draft."""
import re

p = 'PROJECT_STATUS.md'
s = open(p, encoding='utf-8').read()
s = re.sub(r"- \[~\] REI on NeoForge 26\.x.*", "- [x] REI on NeoForge 26.3, 26.2, 26.1.2, NeoForge 1.21.1 and Forge 1.20.1, EMI on NeoForge 1.21.1 and Forge 1.20.1: all five categories opened in real clients with chance tooltips (REI: NeoForge 26.3, NeoForge 1.21.1, Forge 1.20.1; EMI: NeoForge 1.21.1, Forge 1.20.1; frames in docs/frames); 26.2 and 26.1.2 share the 26.3 code and compile; n/a: no REI build for Forge 26.x, Forge 1.21.1 or NeoForge 1.20.1, no EMI build for 26.x, Forge 1.21.1 or NeoForge 1.20.1 (w:3)", s)
s = s.replace("three builds ran a real-client tour", "three builds ran a real-client tour and every viewer was opened in a real client")
open(p, 'w', encoding='utf-8').write(s)

p = 'docs/known-gaps.md'
s = open(p, encoding='utf-8').read()
s = s.replace("seen in a real client are BEI and SMWTI on Forge 26.3 and Jade and JEI on NeoForge 26.3. REI and EMI, and every other build, compile only.",
              "seen in a real client are BEI and SMWTI on Forge 26.3, Jade and JEI on NeoForge 26.3, REI on NeoForge 26.3, NeoForge 1.21.1 and Forge 1.20.1, and EMI on NeoForge 1.21.1 and Forge 1.20.1. Every other build of them compiles only.")
open(p, 'w', encoding='utf-8').write(s)

W = r"G:\Other computers\My Computer\Projects\ItMeCasey\website\d-minimal\wiki\barelyprospecting\pages"
for name, old, new in (("compatibility.md", "BEI and SMWTI on Forge 26.3, Jade and JEI on NeoForge 26.3. The rest compiles and is not yet seen.",
                        "BEI and SMWTI on Forge 26.3, Jade and JEI on NeoForge 26.3, REI on NeoForge 26.3, NeoForge 1.21.1 and Forge 1.20.1, EMI on NeoForge 1.21.1 and Forge 1.20.1. The rest compiles and is not yet seen."),
                       ("known-gaps.md", "REI and EMI, and every other build of the integrations, compile and are not seen yet.",
                        "REI and EMI were also opened in real clients (REI: NeoForge 26.3, NeoForge 1.21.1, Forge 1.20.1; EMI: NeoForge 1.21.1, Forge 1.20.1). Every other build of the integrations compiles and is not seen yet.")):
    q = W + "\\" + name
    t = open(q, encoding='utf-8').read().replace(old, new)
    open(q, 'w', encoding='utf-8', newline='\n').write(t)
print('ok')
