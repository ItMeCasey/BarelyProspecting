"""Derives the REI plugin of the other versions from the 26.3 one. Run from the project folder."""
import os
import re

NAME = 'src/rei/java/com/itmecasey/barelyprospecting/compat/rei/ProspectReiPlugin.java'
s26 = open('version-26.3/' + NAME, encoding='utf-8').read()


def write(version, text):
    path = f'version-{version}/{NAME}'
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, 'w', encoding='utf-8').write(text)


for v in ('26.2', '26.1.2'):
    write(v, s26)

old = s26.replace('import net.minecraft.resources.Identifier;', 'import net.minecraft.resources.ResourceLocation;').replace('CategoryIdentifier', '@@CAT@@').replace('Identifier', 'ResourceLocation').replace('@@CAT@@', 'CategoryIdentifier')
old = old.replace('import com.mojang.serialization.MapCodec;\n', '').replace('import me.shedaniel.rei.api.common.display.DisplaySerializer;\n', '').replace('import net.minecraft.network.codec.StreamCodec;\n', '')
# drop the serializer constant and getter
old = re.sub(r'        /\*\* These displays never travel.*?false\);\n\n', '', old, flags=re.S)
old = re.sub(r'\n        @Override\n        public DisplaySerializer<\? extends PageDisplay> getSerializer\(\) \{\n            return SERIALIZER;\n        \}\n', '', old)
write('1.21.1', old)
write('1.20.1', old)
print('ok')
