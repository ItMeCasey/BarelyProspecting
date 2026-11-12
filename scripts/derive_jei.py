"""Derives the JEI plugin of the other versions from the 26.3 one. Run from the project folder."""
import os

NAME = 'src/jei/java/com/itmecasey/barelyprospecting/compat/jei/ProspectJeiPlugin.java'
s26 = open('version-26.3/' + NAME, encoding='utf-8').read()


def write(version, text):
    path = f'version-{version}/{NAME}'
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, 'w', encoding='utf-8').write(text)


for v in ('26.2', '26.1.2'):
    write(v, s26)

old = s26.replace('import net.minecraft.resources.Identifier;', 'import net.minecraft.resources.ResourceLocation;').replace('Identifier getPluginUid', 'ResourceLocation getPluginUid')
old = old.replace('private static final Identifier UID', 'private static final ResourceLocation UID')
old = old.replace('import mezz.jei.api.recipe.types.IRecipeType;\n', 'import mezz.jei.api.recipe.RecipeType;\n')
old = old.replace('IRecipeType.create(ProspectMod.id(kind.path()), ViewerData.Page.class)', 'RecipeType.create(ProspectMod.MODID, kind.path(), ViewerData.Page.class)')
old = old.replace('IRecipeType<', 'RecipeType<')
old = old.replace('registration.addCraftingStation(', 'registration.addRecipeCatalysts(')
old = old.replace('.setStandardSlotBackground().add(stack)', '.setStandardSlotBackground().addItemStack(stack)')
reg = '''        @Override
        public Identifier getIdentifier(ViewerData.Page page) {
            return ProspectMod.id(kind.path() + "/" + page.id());
        }
'''
assert reg in old
v121 = old.replace(reg, reg.replace('Identifier getIdentifier', 'ResourceLocation getRegistryName'))
write('1.21.1', v121)
v120 = old.replace('\n' + reg, '')
write('1.20.1', v120)
print('ok')
