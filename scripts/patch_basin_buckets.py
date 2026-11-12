"""Lets a basin take a vanilla bucket of water or lava (the way into a basin before any clay bucket exists). Applies the same edit to
every version's BasinBlock and self test. Run from the project folder."""
import re

VERSIONS = ['26.3', '26.2', '26.1.2', '1.21.1', '1.20.1']
for v in VERSIONS:
    p = f'version-{v}/src/main/java/com/itmecasey/barelyprospecting/block/BasinBlock.java'
    s = open(p, encoding='utf-8').read()
    if 'WATER_BUCKET' in s:
        continue
    result = 'ItemInteractionResult' if v == '1.21.1' else 'InteractionResult'
    snippet = f'''        if (stack.is(net.minecraft.world.item.Items.WATER_BUCKET) || stack.is(net.minecraft.world.item.Items.LAVA_BUCKET)) {{
            if (level.isClientSide()) {{
                return {result}.SUCCESS;
            }}
            boolean lava = stack.is(net.minecraft.world.item.Items.LAVA_BUCKET);
            if (basin.fill(lava ? Recipes.Fluid.LAVA : Recipes.Fluid.WATER, BasinState.CAPACITY) > 0) {{
                be.changed();
                level.playSound(null, pos, lava ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.8F, 1.0F);
                if (!player.getAbilities().instabuild) {{
                    player.setItemInHand(hand, new ItemStack(net.minecraft.world.item.Items.BUCKET));
                }}
            }} else {{
                Compat.bar(player, "message.barelyprospecting.basin_full");
            }}
            return {result}.SUCCESS;
        }}
'''
    s = s.replace('        BasinState basin = be.basin();\n', '        BasinState basin = be.basin();\n' + snippet, 1)
    open(p, 'w', encoding='utf-8').write(s)
    t = f'version-{v}/src/dev/java/com/itmecasey/barelyprospecting/dev/SelfTest.java'
    s = open(t, encoding='utf-8').read()
    marker = '        // Soak silt.\n'
    check = '''        // A vanilla bucket of water fills it too, and comes back as an empty bucket.
        be.basin().drain();
        ItemStack vanilla = new ItemStack(Items.WATER_BUCKET);
        steve.setItemInHand(hand(), vanilla);
        level.getBlockState(p).use(level, steve, hand(), hit(p));
        check("a vanilla bucket of water fills the basin", be.basin().levels() == 4 && steve.getItemInHand(hand()).is(Items.BUCKET));
'''
    if v in ('26.3', '26.2', '26.1.2', '1.21.1'):
        check = check.replace('level.getBlockState(p).use(level, steve, hand(), hit(p));',
                              'level.getBlockState(p).useItemOn(vanilla, level, steve, hand(), hit(p));')
    if 'vanilla bucket of water' not in s:
        s = s.replace(marker, check + marker, 1)
    open(t, 'w', encoding='utf-8').write(s)
print('patched')
