"""One-off: inserts the compatibility tour (kept in the session scratchpad) into the 26.3 ShotTour. Kept for reference."""
import sys

block = open(sys.argv[1], encoding='utf-8').read()
p = 'version-26.3/src/dev/java/com/itmecasey/barelyprospecting/dev/ShotTour.java'
s = open(p, encoding='utf-8').read()
if 'planCompat' not in s:
    s = s.replace("    private static void plan() {\n        then(0, () -> {",
                  "    private static void plan() {\n        if (\"compat\".equals(System.getenv(\"BARELYPROSPECTING_SHOTS\"))) {\n            planCompat();\n            return;\n        }\n        then(0, () -> {", 1)
    s = s.replace("\n    // ---- helpers ----\n", block, 1)
    open(p, 'w', encoding='utf-8').write(s)
print('ok')
