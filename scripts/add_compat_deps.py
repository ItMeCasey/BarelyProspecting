"""Appends the optional-integration dependencies to every module's build.gradle (compile-only; nothing of them ships).

Our own API jars (BEI, SMWTI) come from the private ItMeCasey Maven when the login is set and from libs/ otherwise. The other mods
come from CurseForge through the same Maven mirror (cursemaven as the fallback). -PdevMods=jei,jade,rei,emi puts the named mods into
the dev client (-PdevMods=all for every one that exists for the build). Run from the project folder; safe to run twice.
"""
import os

MARK = '// --- Optional integrations'

BEI = '2026.11.7'
SMWTI = {'26.3': '2026.11.3', '26.2': '2026.11.3'}

# per module: mod key -> curse.maven coordinate (None means no build exists: n/a)
JADE_1201 = 'curse.maven:jade-324717:8479276'
MODS = {
    'neoforge-26.3': {'jade': 'jade-324717:9042207', 'jei': 'jei-238222:9453343', 'rei': 'roughly-enough-items-310111:9090699'},
    'neoforge-26.2': {'jade': 'jade-324717:8755115', 'jei': 'jei-238222:9352384', 'rei': 'roughly-enough-items-310111:8271756'},
    'neoforge-26.1.2': {'jade': 'jade-324717:9156414', 'jei': 'jei-238222:9384706', 'rei': 'roughly-enough-items-310111:8271478'},
    'neoforge-1.21.1': {'jade': 'jade-324717:8591319', 'jei': 'jei-238222:9393327', 'rei': 'roughly-enough-items-310111:6199140',
                        'emi': 'emi-580555:8081408'},
    'forge-1.21.1': {'jei': 'jei-238222:9393325'},
    'forge-1.20.1': {'jade': 'jade-324717:8479276', 'jei': 'jei-238222:9394360', 'rei': 'roughly-enough-items-310111:5846923',
                     'emi': 'emi-580555:8081375'},
    'neoforge-1.20.1': {'jade': 'jade-324717:8479276'},
    'forge-26.3': {}, 'forge-26.2': {}, 'forge-26.1.2': {},
}


ARCH = {'neoforge-26.3': 'architectury-api-419699:9111315', 'neoforge-26.2': 'architectury-api-419699:9111233', 'neoforge-26.1.2': 'architectury-api-419699:9111154', 'neoforge-1.21.1': 'architectury-api-419699:8492726', 'forge-1.20.1': 'architectury-api-419699:5137938'}
CLOTH = {'neoforge-26.3': 'me.shedaniel.cloth:cloth-config-neoforge:26.3.159', 'neoforge-26.2': 'cloth-config-348521:8269700', 'neoforge-26.1.2': 'cloth-config-348521:7817583', 'neoforge-1.21.1': 'cloth-config-348521:5729127', 'forge-1.20.1': 'cloth-config-348521:5729105'}
NO_SRC = ('architectury', 'cloth')


def block(module):
    loader, mc = module.split('-', 1)
    smwti = SMWTI.get(mc, '2026.11.2')
    fg6 = module in ('forge-1.20.1', 'forge-1.21.1')
    legacy = module == 'neoforge-1.20.1'
    compile_only = 'modCompileOnly' if legacy else 'compileOnly'
    runtime = 'modLocalRuntime' if legacy else ('runtimeOnly' if fg6 else 'localRuntime')

    def wrap(coord):
        c = f'"{coord}"' if coord.startswith('me.shedaniel') else f'"curse.maven:{coord}"'
        return f'fg.deobf({c})' if fg6 else c

    lines = [MARK + ' (BEI, SMWTI, Jade, JEI, REI, EMI): compile-only, loaded by the other mod when it is installed ---',
             'def imcUser = System.getenv("IMC_MAVEN_USER") ?: project.findProperty("imcMavenUser")',
             'def imcPass = System.getenv("IMC_MAVEN_PASSWORD") ?: project.findProperty("imcMavenPassword")',
             'repositories {',
             '    if (imcUser && imcPass) {',
             '        maven {',
             "            name = 'ItMeCasey Maven'",
             "            url = 'https://itmecasey.com/maven'",
             '            credentials { username = imcUser; password = imcPass }',
             '        }',
             '    }',
             "    maven { name = 'Cursemaven (fallback)'; url = 'https://cursemaven.com'; content { includeGroup 'curse.maven' } }",
             "    maven { name = 'Shedaniel'; url = 'https://maven.shedaniel.me'; content { includeGroup 'me.shedaniel.cloth' } }",
             '}']
    if not fg6 and not legacy:
        lines += ['configurations {', '    localRuntime', '    runtimeClasspath.extendsFrom localRuntime', '}']
    lines += ['def devMods = (project.findProperty("devMods") ?: "").toString().split(",").collect { it.trim() }',
              'dependencies {',
              '    if (imcUser && imcPass) {',
              f'        compileOnly "com.itmecasey:bei-api:{mc}-{loader}-{BEI}"',
              f'        compileOnly "com.itmecasey:smwti-api:{mc}-{loader}-{smwti}"',
              '    } else {',
              f"        compileOnly files('libs/bei-api-{mc}-{loader}-{BEI}.jar', 'libs/smwti-api-{mc}-{loader}-{smwti}.jar')",
              '    }']
    for key, coord in MODS[module].items():
        lines.append(f'    {compile_only} {wrap(coord)}')
        lines.append(f'    if (devMods.contains("{key}") || devMods.contains("all")) {runtime} {wrap(coord)}')
    if 'rei' in MODS[module]:
        lines.append('    compileOnly "me.shedaniel.cloth:basic-math:0.6.1"')
        for name, table in (('architectury', ARCH), ('cloth', CLOTH)):
            if module in table:
                if name == 'architectury':
                    lines.append(f'    {compile_only} {wrap(table[module])}')
                lines.append(f'    if (devMods.contains("rei") || devMods.contains("all")) {runtime} {wrap(table[module])}')
    if not fg6 and not legacy:
        lines.append('    if (devMods.contains("bei") || devMods.contains("smwti") || devMods.contains("all")) {')
        lines.append('        fileTree("devlibs").matching { include "*.jar" }.each { f -> if ((devMods.contains("bei") && f.name.startsWith("bei-")) || (devMods.contains("smwti") && f.name.startsWith("smwti-")) || devMods.contains("all")) { runtimeOnly files(f) } }')
        lines.append('    }')
    lines.append('}')
    for key in MODS[module]:
        if key in NO_SRC:
            continue
        lines.append(f"sourceSets.main.java.srcDir '../version-{mc}/src/{key}/java'")
    return '\n'.join(lines) + '\n'


for module in MODS:
    path = f'{module}/build.gradle'
    s = open(path, encoding='utf-8').read()
    if MARK in s:
        s = s[:s.index(MARK)].rstrip() + '\n'
    s = s.rstrip() + '\n\n' + block(module)
    open(path, 'w', encoding='utf-8', newline='\n').write(s)
print('done')
