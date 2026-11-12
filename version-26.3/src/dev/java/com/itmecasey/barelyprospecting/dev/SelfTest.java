package com.itmecasey.barelyprospecting.dev;

import com.itmecasey.barelyprospecting.ModContent;
import com.itmecasey.barelyprospecting.ProspectMod;
import com.itmecasey.barelyprospecting.block.BasinBlock;
import com.itmecasey.barelyprospecting.block.BasinBlockEntity;
import com.itmecasey.barelyprospecting.block.CrucibleBlock;
import com.itmecasey.barelyprospecting.block.CrucibleBlockEntity;
import com.itmecasey.barelyprospecting.block.LureBlockEntity;
import com.itmecasey.barelyprospecting.block.PannableBlock;
import com.itmecasey.barelyprospecting.core.Recipes;
import com.itmecasey.barelyprospecting.core.Tier;
import com.itmecasey.barelyprospecting.server.HandDrops;
import com.itmecasey.barelyprospecting.server.Pan;
import com.itmecasey.barelyprospecting.server.StarterIsland;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Headless check of every hands-on step on a real server. Runs once when the server has started if the system property
 * {@code barelyprospecting.selftest} is set and prints {@code SELFTEST PASS/FAIL} lines ending with
 * {@code SELFTEST DONE failures=N}. Players are bare {@link ServerPlayer} objects without a connection; every action goes through
 * the same entry points the game uses (item use, block interaction, the panning tick).
 */
public final class SelfTest {
    private static final Logger LOGGER = LoggerFactory.getLogger("BarelyProspecting");

    private static int failures;
    private static MinecraftServer server;

    private SelfTest() {
    }

    public static boolean requested() {
        return System.getenv("BARELYPROSPECTING_SELFTEST") != null || System.getProperty("barelyprospecting.selftest") != null;
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            LOGGER.info("SELFTEST PASS {}", name);
        } else {
            failures++;
            LOGGER.error("SELFTEST FAIL {}", name);
        }
    }

    private static ServerPlayer player(ServerLevel level, String name) {
        ServerPlayer player = new ServerPlayer(server, level, new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name),
                ClientInformation.createDefault());
        player.snapTo(0.5, 64, 0.5, 0.0F, 0.0F);
        return player;
    }

    private static int count(ServerPlayer player, net.minecraft.world.item.Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** Sums the items of one kind lying on the ground around a position and removes them. */
    private static int collect(ServerLevel level, BlockPos pos, net.minecraft.world.item.Item item, boolean any) {
        int total = 0;
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3.0))) {
            if (any || entity.getItem().is(item)) {
                total += entity.getItem().getCount();
            }
            entity.discard();
        }
        return total;
    }

    private static boolean is(ServerLevel level, BlockPos pos, Block block) {
        return level.getBlockState(pos).is(block);
    }

    /** Builds a flat platform with clear air above it where the check stands. Returns the y of the first air cell. */
    private static int platform(ServerLevel level, int x, int z) {
        int base = 100;
        level.getChunk(new BlockPos(x, base, z));
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 14; dz++) {
                for (int y = base - 3; y < base; y++) {
                    level.setBlock(new BlockPos(x + dx, y, z + dz), Blocks.STONE.defaultBlockState(), 2);
                }
                for (int y = base; y <= base + 8; y++) {
                    level.setBlock(new BlockPos(x + dx, y, z + dz), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        return base;
    }

    public static int run(MinecraftServer minecraftServer) {
        server = minecraftServer;
        failures = 0;
        server.setDefaultGameType(net.minecraft.world.level.GameType.SURVIVAL);
        try {
            execute();
        } catch (Throwable t) {
            failures++;
            LOGGER.error("SELFTEST FAIL exception", t);
        }
        LOGGER.info("SELFTEST DONE failures={}", failures);
        return failures;
    }

    private static InteractionHand hand() {
        return InteractionHand.MAIN_HAND;
    }

    private static BlockHitResult hit(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }

    private static void execute() {
        ServerLevel level = server.overworld();
        int gx = 200;
        int gz = 200;
        int y = platform(level, gx, gz);
        ServerPlayer steve = player(level, "Steve");
        steve.snapTo(gx + 0.5, y, gz + 0.5, 0.0F, 0.0F);

        registry();
        hammer(level, steve, new BlockPos(gx, y, gz + 1));
        panning(level, steve, new BlockPos(gx + 3, y, gz));
        panningTicks(level, steve, new BlockPos(gx, y, gz + 3), gx, y, gz);
        handDrops(level, steve, new BlockPos(gx - 4, y, gz));
        basin(level, steve, new BlockPos(gx, y, gz + 6));
        crucible(level, steve, new BlockPos(gx + 3, y, gz + 6));
        lure(level, steve, new BlockPos(gx - 3, y, gz + 6));
        island(level);
        data();
    }

    private static void registry() {
        int missing = 0;
        for (String name : ModContent.ITEMS.keySet()) {
            if (!BuiltInRegistries.ITEM.containsKey(ProspectMod.id(name))) {
                missing++;
                LOGGER.error("item {} is not registered", name);
            }
        }
        for (String name : ModContent.BLOCKS.keySet()) {
            if (!BuiltInRegistries.BLOCK.containsKey(ProspectMod.id(name))) {
                missing++;
                LOGGER.error("block {} is not registered", name);
            }
        }
        check("every item and block is registered (" + ModContent.ITEMS.size() + " items, " + ModContent.BLOCKS.size() + " blocks)", missing == 0);
        check("the block entity types are registered", BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(ProspectMod.id("basin"))
                && BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(ProspectMod.id("crucible"))
                && BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(ProspectMod.id("lure")));
        // Every id the core mentions must exist in the game (or be an item of another mod), or the tables would silently drop nothing.
        int unknown = 0;
        for (String table : com.itmecasey.barelyprospecting.core.DropTables.names()) {
            for (var row : com.itmecasey.barelyprospecting.core.DropTables.table(table)) {
                if (!row.isNothing() && ProspectMod.item(row.item()) == null) {
                    unknown++;
                    LOGGER.error("drop table {} names the unknown item {}", table, row.item());
                }
            }
        }
        check("every item in the drop tables exists", unknown == 0);
        int badCrush = 0;
        for (Recipes.Crush c : Recipes.crushes()) {
            if (ProspectMod.block(c.from()) == null || ProspectMod.block(c.to()) == null) {
                badCrush++;
                LOGGER.error("crush {} to {} names an unknown block", c.from(), c.to());
            }
        }
        check("every block in the hammer rules exists", badCrush == 0);
        int badSoak = 0;
        for (Recipes.Soak s : Recipes.soaks()) {
            if (ProspectMod.item(s.input()) == null || ProspectMod.item(s.output()) == null) {
                badSoak++;
            }
        }
        check("every item in the soak rules exists", badSoak == 0);
    }

    private static void hammer(ServerLevel level, ServerPlayer steve, BlockPos p) {
        ItemStack hammer = new ItemStack(ModContent.PICK_HAMMER);
        steve.setItemInHand(hand(), hammer);
        Block[][] chains = {
                {Blocks.DIRT, ModContent.SILT},
                {Blocks.GRASS_BLOCK, ModContent.SILT},
                {Blocks.COBBLESTONE, ModContent.RUBBLE, ModContent.GRIT, ModContent.FINE_GRIT, Blocks.SAND},
                {Blocks.STONE, ModContent.RUBBLE},
                {Blocks.NETHERRACK, ModContent.NETHER_SILT},
                {Blocks.END_STONE, ModContent.END_SILT},
        };
        int blows = 0;
        for (Block[] chain : chains) {
            level.setBlock(p, chain[0].defaultBlockState(), 3);
            boolean ok = true;
            for (int i = 1; i < chain.length; i++) {
                hammer.getItem().useOn(new UseOnContext(steve, hand(), hit(p)));
                blows++;
                ok &= is(level, p, chain[i]);
            }
            check("the hammer turns " + BuiltInRegistries.BLOCK.getKey(chain[0]).getPath() + " into "
                    + BuiltInRegistries.BLOCK.getKey(chain[chain.length - 1]).getPath(), ok);
        }
        level.setBlock(p, Blocks.SAND.defaultBlockState(), 3);
        hammer.getItem().useOn(new UseOnContext(steve, hand(), hit(p)));
        check("sand is not crushed any further", is(level, p, Blocks.SAND));
        check("every blow wears the hammer by one", hammer.getDamageValue() == blows);
        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
    }

    private static void panning(ServerLevel level, ServerPlayer steve, BlockPos p) {
        ItemStack tray = new ItemStack(ModContent.WOODEN_TRAY);
        steve.setItemInHand(hand(), tray);
        level.setBlock(p, ModContent.SILT.defaultBlockState(), 3);
        for (int i = 1; i <= 3; i++) {
            Pan.debugFinish(level, steve, tray, Tier.WOOD, p);
            check("silt wears to stage " + i + " after pan " + i, is(level, p, ModContent.SILT) && level.getBlockState(p).getValue(PannableBlock.WEAR) == i);
        }
        Pan.debugFinish(level, steve, tray, Tier.WOOD, p);
        check("silt is used up after the fourth pan", level.getBlockState(p).isAir());
        check("each pan wears the tray", tray.getDamageValue() == 4);
        collect(level, p, Items.AIR, true);

        // Many pans with the best tray: every table gives something, and what it gives is allowed.
        ItemStack diamond = new ItemStack(ModContent.DIAMOND_TRAY);
        String[] materials = {"silt", "grit", "fine_grit", "nether_silt", "end_silt"};
        for (String material : materials) {
            int total = 0;
            for (int round = 0; round < 25; round++) {
                level.setBlock(p, ModContent.pannableBlock(material).defaultBlockState(), 3);
                Pan.debugFinish(level, steve, diamond, Tier.DIAMOND, p);
                total += collect(level, p, Items.AIR, true);
            }
            check("panning " + material + " with the diamond tray finds items", total > 0);
        }
        // The wooden tray cannot find what is gated behind better trays.
        int gated = 0;
        for (int round = 0; round < 80; round++) {
            level.setBlock(p, ModContent.GRIT.defaultBlockState(), 3);
            Pan.debugFinish(level, steve, new ItemStack(ModContent.WOODEN_TRAY), Tier.WOOD, p);
            gated += collect(level, p, ModContent.DIAMOND_SHARD, false) + collect(level, p, ModContent.IRON_FLAKE, false);
        }
        check("the wooden tray finds no iron or diamond in grit", gated == 0);
        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
    }

    /** The full hold-to-pan loop: the ring fills tick by tick while the player looks at the block, and resets when they look away. */
    private static void panningTicks(ServerLevel level, ServerPlayer steve, BlockPos p, int gx, int y, int gz) {
        ItemStack tray = new ItemStack(ModContent.COPPER_TRAY);
        steve.setItemInHand(hand(), tray);
        steve.snapTo(gx + 0.5, y, gz + 0.5, 0.0F, 0.0F);
        level.setBlock(p, ModContent.SILT.defaultBlockState(), 3);
        double dy = (p.getY() + 0.5) - (y + steve.getEyeHeight());
        double dz = (p.getZ() + 0.5) - (gz + 0.5);
        steve.setXRot((float) Math.toDegrees(Math.atan2(-dy, dz)));
        int total = Tier.COPPER.panTicks();
        for (int i = 0; i < total / 2; i++) {
            Pan.tick(level, steve, tray, Tier.COPPER, hand());
        }
        double half = Pan.progressOf(steve, Tier.COPPER);
        check("the ring is half full halfway through", half > 0.35 && half < 0.65);
        steve.setXRot(-80.0F);
        Pan.tick(level, steve, tray, Tier.COPPER, hand());
        check("looking away starts the ring over", Pan.progressOf(steve, Tier.COPPER) == 0.0);
        steve.setXRot((float) Math.toDegrees(Math.atan2(-dy, dz)));
        for (int i = 0; i < total; i++) {
            Pan.tick(level, steve, tray, Tier.COPPER, hand());
        }
        check("a full ring finishes one pan (silt wears to stage 1)", is(level, p, ModContent.SILT) && level.getBlockState(p).getValue(PannableBlock.WEAR) == 1);
        check("a finished ring wears the copper tray", tray.getDamageValue() == 1);
        // Nether silt needs better than wood.
        BlockPos q = p.east(2);
        level.setBlock(q, ModContent.NETHER_SILT.defaultBlockState(), 3);
        ItemStack wood = new ItemStack(ModContent.WOODEN_TRAY);
        steve.setItemInHand(hand(), wood);
        steve.snapTo(gx + 2.5, y, gz + 0.5, 0.0F, 0.0F);
        double dz2 = (q.getZ() + 0.5) - (gz + 0.5);
        steve.setXRot((float) Math.toDegrees(Math.atan2(-((q.getY() + 0.5) - (y + steve.getEyeHeight())), dz2)));
        for (int i = 0; i < 80; i++) {
            Pan.tick(level, steve, wood, Tier.WOOD, hand());
        }
        check("the wooden tray cannot pan Nether silt", is(level, q, ModContent.NETHER_SILT) && level.getBlockState(q).getValue(PannableBlock.WEAR) == 0);
        Pan.cancel(steve);
        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(q, Blocks.AIR.defaultBlockState(), 3);
        collect(level, p, Items.AIR, true);
    }

    private static void handDrops(ServerLevel level, ServerPlayer steve, BlockPos p) {
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState();
        steve.setItemInHand(hand(), new ItemStack(Items.STICK));
        for (int i = 0; i < 300; i++) {
            HandDrops.onBlockBroken(steve, level, p, leaves);
        }
        check("a held item shakes nothing loose", collect(level, p, Items.AIR, true) == 0);
        steve.setItemInHand(hand(), ItemStack.EMPTY);
        for (int i = 0; i < 300; i++) {
            HandDrops.onBlockBroken(steve, level, p, leaves);
        }
        int twigs = collect(level, p, ModContent.TWIG, false);
        check("breaking leaves by hand gives twigs", twigs > 40);
        for (int i = 0; i < 300; i++) {
            HandDrops.onBlockBroken(steve, level, p, Blocks.DIRT.defaultBlockState());
        }
        check("breaking dirt by hand gives pebbles", collect(level, p, ModContent.PEBBLE, false) > 40);
        for (int i = 0; i < 100; i++) {
            HandDrops.onBlockBroken(steve, level, p, Blocks.STONE.defaultBlockState());
        }
        check("stone gives nothing by hand", collect(level, p, Items.AIR, true) == 0);
    }

    private static void basin(ServerLevel level, ServerPlayer steve, BlockPos p) {
        level.setBlock(p, ModContent.BASIN.defaultBlockState(), 3);
        BasinBlockEntity be = (BasinBlockEntity) level.getBlockEntity(p);
        check("a basin block has its block entity", be != null);
        if (be == null) {
            return;
        }
        // A silt block in a dry basin is refused.
        ItemStack silt = new ItemStack(ModContent.SILT, 4);
        steve.setItemInHand(hand(), silt);
        level.getBlockState(p).useItemOn(silt, level, steve, hand(), hit(p));
        check("a dry basin does not take silt", !be.basin().isSoaking() && silt.getCount() == 4);
        // A clay bucket of water fills it.
        ItemStack bucket = new ItemStack(ModContent.CLAY_BUCKET_WATER);
        steve.setItemInHand(hand(), bucket);
        level.getBlockState(p).useItemOn(bucket, level, steve, hand(), hit(p));
        check("a clay bucket of water fills the basin", be.basin().levels() == 4 && level.getBlockState(p).getValue(BasinBlock.WATER) == 4);
        check("the bucket comes back empty", steve.getItemInHand(hand()).is(ModContent.CLAY_BUCKET));
        // Soak silt.
        steve.setItemInHand(hand(), silt);
        level.getBlockState(p).useItemOn(silt, level, steve, hand(), hit(p));
        check("silt starts soaking and is used up one at a time", be.basin().isSoaking() && silt.getCount() == 3);
        for (int i = 0; i < 400; i++) {
            BasinBlockEntity.serverTick(level, p, level.getBlockState(p), be);
        }
        check("the soak finishes", be.basin().hasResult());
        steve.setItemInHand(hand(), ItemStack.EMPTY);
        level.getBlockState(p).useWithoutItem(level, steve, hit(p));
        check("an empty hand takes two clay balls", count(steve, Items.CLAY_BALL) == 2 && !be.basin().hasResult());
        // The Nether recipe does not work here.
        be.basin().drain();
        be.basin().fill(Recipes.Fluid.LAVA, 4);
        check("fine grit does not soak in lava in the Overworld", be.basin().start("barelyprospecting:fine_grit", "minecraft:overworld") == null);
        check("fine grit soaks in lava in the Nether", be.basin().start("barelyprospecting:fine_grit", "minecraft:the_nether") != null);
        // The basin survives a save and load.
        BasinBlockEntity again = new BasinBlockEntity(p, level.getBlockState(p));
        again.basin().load(be.basin().fluidOrdinal(), be.basin().levels(), be.basin().soakingItem(), be.basin().ticksLeft(),
                be.basin().totalTicks(), be.basin().resultItem(), be.basin().resultCount());
        check("basin state round-trips", again.basin().levels() == be.basin().levels() && again.basin().isSoaking());
        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
    }

    private static void crucible(ServerLevel level, ServerPlayer steve, BlockPos p) {
        level.setBlock(p, ModContent.CRUCIBLE.defaultBlockState(), 3);
        CrucibleBlockEntity be = (CrucibleBlockEntity) level.getBlockEntity(p);
        check("a crucible block has its block entity", be != null);
        if (be == null) {
            return;
        }
        ItemStack rubble = new ItemStack(ModContent.RUBBLE, 8);
        steve.setItemInHand(hand(), rubble);
        for (int i = 0; i < 4; i++) {
            level.getBlockState(p).useItemOn(rubble, level, steve, hand(), hit(p));
        }
        check("rubble goes into the pot by hand", be.crucible().queued() == 1000 && rubble.getCount() == 4);
        for (int i = 0; i < 300; i++) {
            CrucibleBlockEntity.serverTick(level, p, level.getBlockState(p), be);
        }
        check("without a fire nothing melts", be.crucible().lava() == 0);
        ItemStack coal = new ItemStack(Items.COAL, 2);
        steve.setItemInHand(hand(), coal);
        level.getBlockState(p).useItemOn(coal, level, steve, hand(), hit(p));
        check("coal feeds the fire and the block lights up", be.crucible().isBurning() && level.getBlockState(p).getValue(CrucibleBlock.LIT));
        for (int i = 0; i < 600; i++) {
            CrucibleBlockEntity.serverTick(level, p, level.getBlockState(p), be);
        }
        check("the burning crucible melts rubble into lava", be.crucible().lava() == 1000 && be.crucible().queued() == 0);
        check("the pot shows its lava", level.getBlockState(p).getValue(CrucibleBlock.LAVA) >= 1);
        // A clay bucket takes a full bucket of lava.
        ItemStack bucket = new ItemStack(ModContent.CLAY_BUCKET);
        steve.setItemInHand(hand(), bucket);
        level.getBlockState(p).useItemOn(bucket, level, steve, hand(), hit(p));
        check("an empty clay bucket takes a bucket of lava", steve.getItemInHand(hand()).is(ModContent.CLAY_BUCKET_LAVA) && be.crucible().lava() == 0);
        // Pour it back and quench it with water for obsidian.
        ItemStack lava = steve.getItemInHand(hand());
        level.getBlockState(p).useItemOn(lava, level, steve, hand(), hit(p));
        check("the lava can be poured back in", be.crucible().lava() == 1000);
        ItemStack water = new ItemStack(ModContent.CLAY_BUCKET_WATER);
        steve.setItemInHand(hand(), water);
        level.getBlockState(p).useItemOn(water, level, steve, hand(), hit(p));
        check("water on a full pot of lava makes obsidian", count(steve, Items.OBSIDIAN) == 1 && be.crucible().lava() == 0);
        // A quarter of a bucket quenches into stone.
        be.crucible().load(0, 0, 500);
        water = new ItemStack(ModContent.CLAY_BUCKET_WATER);
        steve.setItemInHand(hand(), water);
        level.getBlockState(p).useItemOn(water, level, steve, hand(), hit(p));
        check("a smaller amount of lava quenches into stone", count(steve, Items.STONE) == 2);
        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        // The clay bucket pours water onto the ground.
        BlockPos ground = p.east(1);
        level.setBlock(ground.below(), Blocks.STONE.defaultBlockState(), 3);
        ItemStack poured = new ItemStack(ModContent.CLAY_BUCKET_WATER);
        steve.setItemInHand(hand(), poured);
        poured.getItem().useOn(new UseOnContext(steve, hand(), new BlockHitResult(Vec3.atCenterOf(ground.below()), Direction.UP, ground.below(), false)));
        check("a clay bucket of water pours a water source", level.getBlockState(ground).is(Blocks.WATER) && steve.getItemInHand(hand()).is(ModContent.CLAY_BUCKET));
        level.setBlock(ground, Blocks.AIR.defaultBlockState(), 3);
    }

    private static void lure(ServerLevel level, ServerPlayer steve, BlockPos p) {
        level.setBlock(p, ModContent.LURE.defaultBlockState(), 3);
        LureBlockEntity be = (LureBlockEntity) level.getBlockEntity(p);
        check("a lure block has its block entity", be != null);
        if (be == null) {
            return;
        }
        ItemStack bone = new ItemStack(Items.BONE, 3);
        ItemStack seeds = new ItemStack(Items.WHEAT_SEEDS, 5);
        steve.setItemInHand(hand(), seeds);
        level.getBlockState(p).useItemOn(seeds, level, steve, hand(), hit(p));
        steve.setItemInHand(hand(), bone);
        level.getBlockState(p).useItemOn(bone, level, steve, hand(), hit(p));
        check("the lure takes one kind of bait at a time", be.lure().meals() == 1 && bone.getCount() == 3);
        steve.setItemInHand(hand(), seeds);
        level.getBlockState(p).useItemOn(seeds, level, steve, hand(), hit(p));
        level.getBlockState(p).useItemOn(seeds, level, steve, hand(), hit(p));
        check("three meals start the hatch", be.lure().isHatching() && seeds.getCount() == 2);
        check("the lure shows three meals", level.getBlockState(p).getValue(com.itmecasey.barelyprospecting.block.LureBlock.MEALS) == 3);
        for (int i = 0; i < 400; i++) {
            LureBlockEntity.serverTick(level, p, level.getBlockState(p), be);
        }
        check("the lure resets after hatching", be.lure().meals() == 0 && !be.lure().isHatching());
        int found = collect(level, p, Items.AIR, true);
        // The farm table has a small chance to find nothing, so run a few more hatches.
        for (int round = 0; round < 6 && found == 0; round++) {
            for (int i = 0; i < 3; i++) {
                be.lure().feed("minecraft:wheat_seeds");
            }
            for (int i = 0; i < 400; i++) {
                LureBlockEntity.serverTick(level, p, level.getBlockState(p), be);
            }
            found += collect(level, p, Items.AIR, true);
        }
        check("a hatched lure drops its finds above the block", found > 0);
        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
    }

    private static void island(ServerLevel level) {
        boolean voidWorld = StarterIsland.isVoidWorld(level);
        check("the world check runs (this test world is " + (voidWorld ? "void" : "not void") + ")", true);
        BlockPos spawnBefore = BlockPos.ZERO;
        StarterIsland.build(level);
        check("the starter island has grass on top", is(level, new BlockPos(0, StarterIsland.TOP, 0), Blocks.GRASS_BLOCK));
        check("the starter island has stone below", is(level, new BlockPos(0, StarterIsland.TOP - 5, 0), Blocks.STONE));
        check("the starter island has one oak", is(level, new BlockPos(2, StarterIsland.TOP + 1, 1), Blocks.OAK_LOG)
                && is(level, new BlockPos(2, StarterIsland.TOP + 3, 1), Blocks.OAK_LOG));
        BlockPos chest = new BlockPos(-2, StarterIsland.TOP + 1, 0);
        boolean stocked = level.getBlockEntity(chest) instanceof Container c && c.countItem(Items.OAK_SAPLING) == 2 && c.countItem(Items.WATER_BUCKET) == 1;
        check("the starter chest holds the starter items", stocked);
    }

    private static void data() {
        List<String> recipes = List.of("pick_hammer", "wooden_panning_tray", "turf", "basin", "unfired_clay_bucket", "unfired_crucible",
                "clay_bucket_campfire", "crucible_campfire", "copper_panning_tray", "iron_panning_tray", "gold_panning_tray",
                "diamond_panning_tray", "raw_copper", "raw_iron", "raw_gold", "diamond_from_shards", "lure", "end_portal_frame");
        int missing = 0;
        for (String name : recipes) {
            if (server.getResourceManager().getResourceStack(ProspectMod.id("recipe/" + name + ".json")).isEmpty()) {
                missing++;
                LOGGER.error("recipe {} is missing from the data pack", name);
            }
        }
        check("all " + recipes.size() + " recipes are in the data pack", missing == 0);
        int noLoot = 0;
        for (String name : ModContent.BLOCKS.keySet()) {
            var table = server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, ProspectMod.id("blocks/" + name)));
            if (table == net.minecraft.world.level.storage.loot.LootTable.EMPTY) {
                noLoot++;
                LOGGER.error("block {} has no loot table", name);
            }
        }
        check("every block drops itself (loot tables load)", noLoot == 0);
        check("the void-island world preset is registered", server.registryAccess().lookupOrThrow(Registries.WORLD_PRESET)
                .containsKey(ResourceKey.create(Registries.WORLD_PRESET, ProspectMod.id("void_island"))));
        boolean coalTag = server.registryAccess().lookupOrThrow(Registries.ITEM).get(Identifier.parse("barelyprospecting:coal_dust"))
                .map(h -> h.is(net.minecraft.tags.ItemTags.COALS)).orElse(false);
        check("coal dust counts as coal for campfires and torches", coalTag);
    }
}
