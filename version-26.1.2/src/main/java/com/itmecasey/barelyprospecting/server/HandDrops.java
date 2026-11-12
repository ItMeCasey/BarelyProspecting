package com.itmecasey.barelyprospecting.server;

import com.itmecasey.barelyprospecting.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Phase 0: breaking leaves or dirt with a bare hand sometimes shakes loose a twig or a pebble. */
public final class HandDrops {
    public static final double TWIG_CHANCE = 0.30;
    public static final double PEBBLE_CHANCE = 0.35;

    private HandDrops() {
    }

    /** Called by the loader when a player breaks a block. Only an empty main hand finds anything. */
    public static void onBlockBroken(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
        if (!player.getMainHandItem().isEmpty()) {
            return;
        }
        RandomSource random = level.getRandom();
        if (state.is(BlockTags.LEAVES)) {
            if (random.nextDouble() < TWIG_CHANCE) {
                Compat.drop(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(ModContent.TWIG, 1 + random.nextInt(2)));
            }
        } else if (state.is(BlockTags.DIRT)) {
            if (random.nextDouble() < PEBBLE_CHANCE) {
                Compat.drop(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(ModContent.PEBBLE, 1 + random.nextInt(2)));
            }
        }
    }
}
