"""One-off helper used to derive version-1.20.1 from version-1.21.1 (kept so the port can be repeated). Run from the project folder."""
import os
import re
import shutil

SRC = 'version-1.21.1/src'
DST = 'version-1.20.1/src'
B = DST + '/main/java/com/itmecasey/barelyprospecting/'

for root, _, files in os.walk(SRC):
    for f in files:
        if f.endswith('.java'):
            p = os.path.join(root, f)
            q = os.path.join(DST, os.path.relpath(p, SRC))
            os.makedirs(os.path.dirname(q), exist_ok=True)
            shutil.copy(p, q)


def edit(rel, fn):
    p = rel if rel.startswith('version') else B + rel
    s = open(p, encoding='utf-8').read()
    s = fn(s)
    open(p, 'w', encoding='utf-8').write(s)


edit('ProspectMod.java', lambda s: s.replace('ResourceLocation.fromNamespaceAndPath(MODID, path)', 'new ResourceLocation(MODID, path)'))


def be(s):
    s = s.replace('protected void saveAdditional(CompoundTag output, HolderLookup.Provider registries) {\n        super.saveAdditional(output, registries);',
                  'protected void saveAdditional(CompoundTag output) {\n        super.saveAdditional(output);')
    s = s.replace('protected void loadAdditional(CompoundTag input, HolderLookup.Provider registries) {\n        super.loadAdditional(input, registries);',
                  'public void load(CompoundTag input) {\n        super.load(input);')
    s = s.replace('public CompoundTag getUpdateTag(HolderLookup.Provider registries) {\n        return saveWithoutMetadata(registries);',
                  'public CompoundTag getUpdateTag() {\n        return saveWithoutMetadata();')
    s = s.replace('import net.minecraft.core.HolderLookup;\n', '')
    return s


def block(s):
    s = re.sub(r'    public static final MapCodec<\w+> CODEC = simpleCodec\(\w+::new\);\n\n    @Override\n    protected MapCodec<\? extends BaseEntityBlock> codec\(\) \{\n        return CODEC;\n    \}\n\n', '', s)
    s = s.replace('import com.mojang.serialization.MapCodec;\n', '').replace('import net.minecraft.world.ItemInteractionResult;\n', '')
    s = s.replace('@Override\n    protected ItemInteractionResult useItemOn(', 'private InteractionResult useItemOn(')
    s = s.replace('@Override\n    protected InteractionResult useWithoutItem(', 'private InteractionResult useWithoutItem(')
    s = s.replace('private ItemInteractionResult useBucket(', 'private InteractionResult useBucket(')
    s = s.replace('ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION', 'InteractionResult.PASS').replace('ItemInteractionResult.SUCCESS', 'InteractionResult.SUCCESS')
    s = s.replace('InteractionResult.TRY_WITH_EMPTY_HAND', 'InteractionResult.PASS')
    for name in ('getRenderShape', 'getShape', 'isRandomlyTicking', 'randomTick'):
        s = re.sub(r'protected (\S+) ' + name, r'public \1 ' + name, s)
    use = ('    /** One entry point in this version: a held item goes to the item rules, an empty hand to the empty-hand rules. */\n'
           '    @Override\n'
           '    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {\n'
           '        ItemStack stack = player.getItemInHand(hand);\n'
           '        if (stack.isEmpty()) {\n'
           '            return useWithoutItem(state, level, pos, player, hit);\n'
           '        }\n'
           '        return useItemOn(stack, state, level, pos, player, hand, hit);\n'
           '    }\n\n')
    i = s.index('    private InteractionResult useItemOn(')
    # keep any javadoc that precedes: insert before the javadoc if present
    return s[:i] + use + s[i:]


for n in ('Basin', 'Crucible', 'Lure'):
    edit(f'block/{n}BlockEntity.java', be)
    edit(f'block/{n}Block.java', block)


def tray(s):
    return s.replace('public int getUseDuration(ItemStack stack, LivingEntity entity) {', 'public int getUseDuration(ItemStack stack) {')


edit('item/TrayItem.java', tray)
edit('item/HammerItem.java', lambda s: s.replace('hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(context.getHand()));',
                                                  'hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(context.getHand()));'))
edit('server/Pan.java', lambda s: s.replace('tray.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));',
                                             'tray.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(hand));'))


def selftest(s):
    s = s.replace('new ServerPlayer(server, level, new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name),\n                ClientInformation.createDefault())',
                  'new ServerPlayer(server, level, new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name))')
    s = s.replace('import net.minecraft.server.level.ClientInformation;\n', '')
    s = s.replace('ResourceLocation.parse("barelyprospecting:coal_dust")', 'new ResourceLocation("barelyprospecting:coal_dust")')
    # loot tables
    s = re.sub(r'var table = server\.reloadableRegistries\(\)\.getLootTable\(ResourceKey\.create\(Registries\.LOOT_TABLE, ProspectMod\.id\("blocks/" \+ name\)\)\);',
               'var table = server.getLootData().getLootTable(ProspectMod.id("blocks/" + name));', s)
    s = s.replace('server.registryAccess().lookupOrThrow(Registries.WORLD_PRESET)\n                .get(ResourceKey.create(Registries.WORLD_PRESET, ProspectMod.id("void_island"))).isPresent());',
                  'server.registryAccess().registryOrThrow(Registries.WORLD_PRESET)\n                .containsKey(ProspectMod.id("void_island")));')
    i = s.index('boolean coalTag =')
    j = s.index('check("coal dust counts')
    s = s[:i] + 'boolean coalTag = BuiltInRegistries.ITEM.getHolderOrThrow(ResourceKey.create(Registries.ITEM, ProspectMod.id("coal_dust")))\n                .is(net.minecraft.tags.ItemTags.COALS);\n        ' + s[j:]
    s = s.replace('ProspectMod.id("recipe/"', 'ProspectMod.id("recipes/"')
    # block interaction helper
    s = re.sub(r'level\.getBlockState\(p\)\.useItemOn\((\w+), level, steve, hand\(\), hit\(p\)\);', r'useItem(level, steve, \1, p);', s)
    s = s.replace('level.getBlockState(p).useWithoutItem(level, steve, hit(p));', 'level.getBlockState(p).use(level, steve, hand(), hit(p));')
    s = s.replace('    private static BlockHitResult hit(BlockPos pos) {',
                  '    private static void useItem(ServerLevel level, ServerPlayer player, ItemStack stack, BlockPos pos) {\n        player.setItemInHand(hand(), stack);\n        level.getBlockState(pos).use(level, player, hand(), hit(pos));\n    }\n\n    private static BlockHitResult hit(BlockPos pos) {')
    return s


edit(DST + '/dev/java/com/itmecasey/barelyprospecting/dev/SelfTest.java', selftest)
print('derived version-1.20.1')
