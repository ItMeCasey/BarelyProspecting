"""Derives the Jade plugin of the other versions from the 26.3 one. Run from the project folder."""
import os
import shutil

SRC = 'version-26.3/src/jade/java/com/itmecasey/barelyprospecting/compat/jade/ProspectJadePlugin.java'
s26 = open(SRC, encoding='utf-8').read()


def write(version, text):
    path = f'version-{version}/src/jade/java/com/itmecasey/barelyprospecting/compat/jade/ProspectJadePlugin.java'
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, 'w', encoding='utf-8').write(text)


for v in ('26.2', '26.1.2'):
    write(v, s26)

# 1.21.1: ResourceLocation, and components go through NBT with the level's registry access.
s121 = s26.replace('import net.minecraft.resources.Identifier;', 'import net.minecraft.resources.ResourceLocation;').replace('Identifier', 'ResourceLocation')
s121 = s121.replace('import net.minecraft.nbt.CompoundTag;', 'import net.minecraft.nbt.CompoundTag;\nimport net.minecraft.nbt.NbtOps;\nimport net.minecraft.nbt.Tag;')
s121 = s121.replace('data.store(TAG, ComponentSerialization.CODEC.listOf(), LookInfo.lines(entity));',
                    'ComponentSerialization.CODEC.listOf()\n                        .encodeStart(accessor.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE), LookInfo.lines(entity))\n                        .result().ifPresent(lines -> data.put(TAG, lines));')
s121 = s121.replace('List<Component> lines = accessor.getServerData().read(TAG, ComponentSerialization.CODEC.listOf()).orElse(List.of());',
                    'Tag saved = accessor.getServerData().get(TAG);\n            List<Component> lines = saved == null ? List.of() : ComponentSerialization.CODEC.listOf()\n                    .parse(accessor.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE), saved)\n                    .result().orElse(List.of());')
write('1.21.1', s121)

# 1.20.1: lines travel as JSON strings, and server data is asked for by block entity class.
s120 = s26.replace('import net.minecraft.resources.Identifier;', 'import net.minecraft.resources.ResourceLocation;').replace('Identifier', 'ResourceLocation')
s120 = s120.replace('import net.minecraft.network.chat.ComponentSerialization;\n', '')
s120 = s120.replace('import net.minecraft.nbt.CompoundTag;', 'import net.minecraft.nbt.CompoundTag;\nimport net.minecraft.nbt.ListTag;\nimport net.minecraft.nbt.StringTag;\nimport net.minecraft.nbt.Tag;')
s120 = s120.replace('import com.itmecasey.barelyprospecting.block.BasinBlock;', 'import com.itmecasey.barelyprospecting.block.BasinBlock;\nimport com.itmecasey.barelyprospecting.block.BasinBlockEntity;\nimport com.itmecasey.barelyprospecting.block.CrucibleBlockEntity;\nimport com.itmecasey.barelyprospecting.block.LureBlockEntity;')
s120 = s120.replace('    @Override\n    public void register(IWailaCommonRegistration registration) {\n        for (Class<? extends Block> block : MACHINES) {\n            registration.registerBlockDataProvider(ServerData.INSTANCE, block);\n        }\n    }',
                    '    private static final List<Class<? extends BlockEntity>> MACHINE_ENTITIES = List.of(BasinBlockEntity.class, CrucibleBlockEntity.class, LureBlockEntity.class);\n\n    @Override\n    public void register(IWailaCommonRegistration registration) {\n        for (Class<? extends BlockEntity> entity : MACHINE_ENTITIES) {\n            registration.registerBlockDataProvider(ServerData.INSTANCE, entity);\n        }\n    }')
s120 = s120.replace('data.store(TAG, ComponentSerialization.CODEC.listOf(), LookInfo.lines(entity));',
                    'ListTag lines = new ListTag();\n                for (Component line : LookInfo.lines(entity)) {\n                    lines.add(StringTag.valueOf(Component.Serializer.toJson(line)));\n                }\n                data.put(TAG, lines);')
s120 = s120.replace('List<Component> lines = accessor.getServerData().read(TAG, ComponentSerialization.CODEC.listOf()).orElse(List.of());\n            lines.forEach(tooltip::add);',
                    'ListTag lines = accessor.getServerData().getList(TAG, Tag.TAG_STRING);\n            for (int i = 0; i < lines.size(); i++) {\n                Component line = Component.Serializer.fromJson(lines.getString(i));\n                if (line != null) {\n                    tooltip.add(line);\n                }\n            }')
write('1.20.1', s120)
print('ok')
