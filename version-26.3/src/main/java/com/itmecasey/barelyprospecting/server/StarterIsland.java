package com.itmecasey.barelyprospecting.server;

import com.itmecasey.barelyprospecting.ProspectMod;
import com.itmecasey.barelyprospecting.core.Settings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelResource;

/**
 * The void-island world preset. The preset is a flat world whose only layer is air; the first time such a world starts, this builds
 * a small island of dirt and stone at the spawn point, plants one oak and places a chest of starter items, then points the world
 * spawn at it. A marker file in the world folder keeps it from ever building twice.
 */
public final class StarterIsland {
    /** The height of the island's grass layer. */
    public static final int TOP = 63;

    private StarterIsland() {
    }

    /** True when the overworld generator is a flat world with no solid layer (the void preset). */
    public static boolean isVoidWorld(ServerLevel overworld) {
        if (overworld.getChunkSource().getGenerator() instanceof FlatLevelSource flat) {
            return flat.settings().getLayers().stream().allMatch(BlockState::isAir);
        }
        return false;
    }

    private static Path marker(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("barelyprospecting").resolve("island_built");
    }

    /** Builds the island when this is a void-preset world and it has not been built yet. Returns whether it built one. */
    public static boolean ensure(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        if (!Settings.buildStarterIsland() || !isVoidWorld(overworld) || Files.exists(marker(server))) {
            return false;
        }
        build(overworld);
        try {
            Files.createDirectories(marker(server).getParent());
            Files.writeString(marker(server), "built");
        } catch (IOException e) {
            ProspectMod.LOGGER.warn("Could not write the island marker; the island may be built again next start", e);
        }
        return true;
    }

    /** Places the island with its centre column at x 0, z 0. */
    public static void build(ServerLevel level) {
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                int r2 = dx * dx + dz * dz;
                BlockPos column = new BlockPos(dx, TOP, dz);
                if (r2 <= 14) {
                    set(level, column, Blocks.GRASS_BLOCK);
                }
                if (r2 <= 14) {
                    set(level, column.below(1), Blocks.DIRT);
                }
                if (r2 <= 10) {
                    set(level, column.below(2), Blocks.DIRT);
                }
                if (r2 <= 8) {
                    set(level, column.below(3), Blocks.STONE);
                }
                if (r2 <= 5) {
                    set(level, column.below(4), Blocks.STONE);
                }
                if (r2 <= 2) {
                    set(level, column.below(5), Blocks.STONE);
                }
            }
        }
        // One oak: a four-block trunk and a rounded crown.
        BlockPos trunk = new BlockPos(2, TOP + 1, 1);
        for (int y = 0; y < 4; y++) {
            set(level, trunk.above(y), Blocks.OAK_LOG);
        }
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (int dy = 2; dy <= 4; dy++) {
            int radius = dy == 4 ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    boolean corner = Math.abs(dx) == 2 && Math.abs(dz) == 2;
                    BlockPos at = trunk.offset(dx, dy, dz);
                    if (!corner && level.getBlockState(at).isAir()) {
                        level.setBlock(at, leaves, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        // The starter chest, facing the spawn point.
        BlockPos chest = new BlockPos(-2, TOP + 1, 0);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST), Block.UPDATE_CLIENTS);
        if (level.getBlockEntity(chest) instanceof Container container) {
            ItemStack[] starter = {
                    new ItemStack(Items.OAK_SAPLING, 2),
                    new ItemStack(Items.BONE_MEAL, 8),
                    new ItemStack(Items.WATER_BUCKET),
                    new ItemStack(Items.BREAD, 6),
                    new ItemStack(Items.COAL, 4),
            };
            for (int i = 0; i < starter.length; i++) {
                container.setItem(i, starter[i]);
            }
        }
        level.setRespawnData(LevelData.RespawnData.of(Level.OVERWORLD, new BlockPos(0, TOP + 1, 0), 0.0F, 0.0F));
        ProspectMod.LOGGER.info("Built the void-island starter island at 0 {} 0", TOP);
    }

    private static void set(ServerLevel level, BlockPos pos, Block block) {
        level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_CLIENTS);
    }
}
