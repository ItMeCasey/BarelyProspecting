"""One-off: inserts the viewer tour (kept in the session scratchpad) into the 26.3 ShotTour, after the helpers marker."""
import sys

block = open(sys.argv[1], encoding='utf-8').read()
p = 'version-26.3/src/dev/java/com/itmecasey/barelyprospecting/dev/ShotTour.java'
s = open(p, encoding='utf-8').read()
if 'planViewers' not in s:
    dispatch = "        if (\"compat\".equals(System.getenv(\"BARELYPROSPECTING_SHOTS\"))) {\n            planCompat();\n            return;\n        }\n"
    assert dispatch in s
    s = s.replace(dispatch, dispatch + "        if (\"viewers\".equals(System.getenv(\"BARELYPROSPECTING_SHOTS\"))) {\n            planViewers();\n            return;\n        }\n", 1)
    s = s.replace("    // ---- helpers ----\n", "    // ---- helpers ----\n" + block, 1)
    open(p, 'w', encoding='utf-8').write(s)
print('ok')
