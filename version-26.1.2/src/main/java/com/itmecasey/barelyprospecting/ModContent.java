package com.itmecasey.barelyprospecting;

import com.itmecasey.barelyprospecting.block.BasinBlock;
import com.itmecasey.barelyprospecting.block.BasinBlockEntity;
import com.itmecasey.barelyprospecting.block.CrucibleBlock;
import com.itmecasey.barelyprospecting.block.CrucibleBlockEntity;
import com.itmecasey.barelyprospecting.block.LureBlock;
import com.itmecasey.barelyprospecting.block.LureBlockEntity;
import com.itmecasey.barelyprospecting.block.PannableBlock;
import com.itmecasey.barelyprospecting.core.DropTables;
import com.itmecasey.barelyprospecting.core.Tier;
import com.itmecasey.barelyprospecting.item.ClayBucketItem;
import com.itmecasey.barelyprospecting.item.HammerItem;
import com.itmecasey.barelyprospecting.item.TrayItem;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Every block, item and block entity type the mod adds, built once and handed to the loader's registry. */
public final class ModContent {
    /** All blocks and all items (block items included) by path, in registration order. */
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();

    // --- Phase 0 and 1: scrounge and silt ---
    public static final Item TWIG = item("twig", new Item(itemProps("twig")));
    public static final Item PEBBLE = item("pebble", new Item(itemProps("pebble")));
    public static final Item COAL_DUST = item("coal_dust", new Item(itemProps("coal_dust")));
    public static final Item PICK_HAMMER = item("pick_hammer", new HammerItem(itemProps("pick_hammer").stacksTo(1).durability(250)));

    public static final PannableBlock SILT = pannable("silt", MapColor.DIRT, SoundType.GRAVEL);
    public static final Block RUBBLE = block("rubble", new Block(blockProps("rubble").mapColor(MapColor.STONE).strength(1.5F, 6.0F).sound(SoundType.STONE)));
    public static final PannableBlock GRIT = pannable("grit", MapColor.STONE, SoundType.GRAVEL);
    public static final PannableBlock FINE_GRIT = pannable("fine_grit", MapColor.SAND, SoundType.SAND);

    public static final Item WOODEN_TRAY = tray(Tier.WOOD);

    // --- Phase 2: water and stone ---
    public static final Block BASIN = block("basin", new BasinBlock(blockProps("basin").mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final Item UNFIRED_CLAY_BUCKET = item("unfired_clay_bucket", new Item(itemProps("unfired_clay_bucket").stacksTo(1)));
    public static final Item CLAY_BUCKET = item("clay_bucket", new ClayBucketItem(ClayBucketItem.Kind.EMPTY, itemProps("clay_bucket").stacksTo(1)));
    public static final Item CLAY_BUCKET_WATER = item("clay_bucket_water", new ClayBucketItem(ClayBucketItem.Kind.WATER, itemProps("clay_bucket_water").stacksTo(1)));
    public static final Item CLAY_BUCKET_LAVA = item("clay_bucket_lava", new ClayBucketItem(ClayBucketItem.Kind.LAVA, itemProps("clay_bucket_lava").stacksTo(1)));
    public static final Block UNFIRED_CRUCIBLE = block("unfired_crucible", new Block(blockProps("unfired_crucible").mapColor(MapColor.CLAY).strength(0.8F).sound(SoundType.GRAVEL).noOcclusion()));
    public static final Block CRUCIBLE = block("crucible", new CrucibleBlock(blockProps("crucible").mapColor(MapColor.TERRACOTTA_ORANGE).strength(1.5F, 6.0F).sound(SoundType.STONE).noOcclusion()
            .lightLevel(state -> state.getValue(CrucibleBlock.LIT) ? 7 : 0)));

    // --- Phase 3: metals ---
    public static final Item COPPER_FLAKE = item("copper_flake", new Item(itemProps("copper_flake")));
    public static final Item IRON_FLAKE = item("iron_flake", new Item(itemProps("iron_flake")));
    public static final Item GOLD_FLAKE = item("gold_flake", new Item(itemProps("gold_flake")));
    public static final Item DIAMOND_SHARD = item("diamond_shard", new Item(itemProps("diamond_shard")));
    public static final Item COPPER_TRAY = tray(Tier.COPPER);
    public static final Item IRON_TRAY = tray(Tier.IRON);
    public static final Item GOLD_TRAY = tray(Tier.GOLD);
    public static final Item DIAMOND_TRAY = tray(Tier.DIAMOND);

    // --- Phase 4: life ---
    public static final Block LURE = block("lure", new LureBlock(blockProps("lure").mapColor(MapColor.PLANT).strength(0.8F).sound(SoundType.GRASS).noOcclusion()));

    // --- Phase 5: beyond ---
    public static final PannableBlock NETHER_SILT = pannable("nether_silt", MapColor.NETHER, SoundType.SOUL_SAND);
    public static final PannableBlock END_SILT = pannable("end_silt", MapColor.SAND, SoundType.SAND);

    public static final BlockEntityType<BasinBlockEntity> BASIN_BE = new BlockEntityType<>(BasinBlockEntity::new, Set.<Block>of(BASIN));
    public static final BlockEntityType<CrucibleBlockEntity> CRUCIBLE_BE = new BlockEntityType<>(CrucibleBlockEntity::new, Set.<Block>of(CRUCIBLE));
    public static final BlockEntityType<LureBlockEntity> LURE_BE = new BlockEntityType<>(LureBlockEntity::new, Set.<Block>of(LURE));

    private ModContent() {
    }

    private static BlockBehaviour.Properties blockProps(String name) {
        return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, ProspectMod.id(name)));
    }

    public static Item.Properties itemProps(String name) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, ProspectMod.id(name)));
    }

    private static <T extends Item> T item(String name, T item) {
        ITEMS.put(name, item);
        return item;
    }

    /** Registers a block and its block item (named like the block). */
    private static <T extends Block> T block(String name, T block) {
        BLOCKS.put(name, block);
        ITEMS.put(name, new BlockItem(block, itemProps(name).useBlockDescriptionPrefix()));
        return block;
    }

    private static PannableBlock pannable(String name, MapColor color, SoundType sound) {
        return block(name, new PannableBlock(name, blockProps(name).mapColor(color).strength(0.6F).sound(sound)));
    }

    private static Item tray(Tier tier) {
        return item(tier.itemPath(), new TrayItem(tier, itemProps(tier.itemPath()).stacksTo(1).durability(tier.durability())));
    }

    /** The pannable block of a material name, or null. */
    public static PannableBlock pannableBlock(String material) {
        Block block = BLOCKS.get(material);
        return block instanceof PannableBlock p && DropTables.isPannable(material) ? p : null;
    }

    /** The creative tab. */
    public static CreativeModeTab creativeTab() {
        return CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + ProspectMod.MODID))
                .icon(() -> new ItemStack(WOODEN_TRAY))
                .displayItems((params, output) -> ITEMS.values().forEach(output::accept))
                .build();
    }
}
