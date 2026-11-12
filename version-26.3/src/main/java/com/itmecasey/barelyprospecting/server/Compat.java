package com.itmecasey.barelyprospecting.server;

import com.itmecasey.barelyprospecting.ProspectMod;
import com.itmecasey.barelyprospecting.core.Drop;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** The few game calls that differ between Minecraft versions, kept in one place per version so the rest of the code ports as is. */
public final class Compat {
    private Compat() {
    }

    /** Shows a short translated message above the hotbar. */
    public static void bar(Player player, String key, Object... args) {
        // A player without a connection (the headless self test) has nobody to show it to.
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.connection != null) {
            serverPlayer.sendOverlayMessage(Component.translatable(key, args));
        }
    }

    /** The dimension id as text: {@code minecraft:overworld}. */
    public static String dimensionId(Level level) {
        return level.dimension().identifier().toString();
    }

    /** Lets an item stack fall out of a block position, with a small random push so stacks do not pile into one spot. */
    public static void drop(ServerLevel level, double x, double y, double z, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemEntity entity = new ItemEntity(level, x, y, z, stack);
        entity.setDeltaMovement((level.getRandom().nextDouble() - 0.5) * 0.12, 0.18, (level.getRandom().nextDouble() - 0.5) * 0.12);
        level.addFreshEntity(entity);
    }

    /** Drops the roll's result above a block. Items that do not exist (a missing mod) are skipped. */
    public static void drop(ServerLevel level, BlockPos pos, Drop drop) {
        ItemStack stack = ProspectMod.stack(drop.item(), drop.count());
        drop(level, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, stack);
    }

    /** Puts a stack in the player's inventory or, when it is full, on the ground at their feet. */
    public static void give(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(stack) && player.level() instanceof ServerLevel level) {
            drop(level, player.getX(), player.getY() + 0.5, player.getZ(), stack.copy());
        }
    }
}
