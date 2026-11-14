package com.itmecasey.barelyprospecting.server;

import com.itmecasey.barelyprospecting.core.script.ScriptEvents;
import com.itmecasey.barelyprospecting.ProspectMod;
import com.itmecasey.barelyprospecting.block.PannableBlock;
import com.itmecasey.barelyprospecting.core.Drop;
import com.itmecasey.barelyprospecting.core.DropTables;
import com.itmecasey.barelyprospecting.core.Ring;
import com.itmecasey.barelyprospecting.core.Settings;
import com.itmecasey.barelyprospecting.core.Tier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The panning session of each player: which block the tray is held on and how far the ring has filled. Called every tick while a
 * tray is in use. The whole loop runs on the server; the ring is drawn with particles every client sees.
 */
public final class Pan {
    /** Points around the ring. */
    private static final int RING_POINTS = 24;
    private static final double REACH = 4.5;

    private record Session(BlockPos pos, int ticks) {
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private Pan() {
    }

    /** One tick of holding the tray. */
    public static void tick(ServerLevel level, ServerPlayer player, ItemStack tray, Tier tier, InteractionHand hand) {
        BlockPos target = lookedAtBlock(level, player);
        PannableBlock block = target == null ? null : level.getBlockState(target).getBlock() instanceof PannableBlock p ? p : null;
        if (block == null) {
            SESSIONS.remove(player.getUUID());
            return;
        }
        if (tier.level() < DropTables.minimumTier(block.material())) {
            if (level.getGameTime() % 20L == 0L) {
                Compat.bar(player, "message.barelyprospecting.tray_too_weak");
            }
            SESSIONS.remove(player.getUUID());
            return;
        }
        Session old = SESSIONS.get(player.getUUID());
        int ticks = old != null && old.pos().equals(target) ? old.ticks() + 1 : 1;
        int total = tier.panTicks();
        if (ticks >= total) {
            SESSIONS.remove(player.getUUID());
            finish(level, player, tray, tier, hand, target, block);
            return;
        }
        SESSIONS.put(player.getUUID(), new Session(target, ticks));
        drawRing(level, target, ticks, total);
        if (ticks % 6 == 0) {
            level.playSound(null, target, SoundEvents.GRAVEL_HIT, SoundSource.BLOCKS, 0.35F, 1.1F + level.getRandom().nextFloat() * 0.3F);
        }
    }

    /** The player let go of the tray (or switched item): the ring starts over next time. */
    public static void cancel(ServerPlayer player) {
        SESSIONS.remove(player.getUUID());
    }

    /** How far the player's ring is, 0..1; 0 when they are not panning. Used by tests and in-world info. */
    public static double progressOf(ServerPlayer player, Tier tier) {
        Session s = SESSIONS.get(player.getUUID());
        return s == null ? 0.0 : Math.min(1.0, s.ticks() / (double) tier.panTicks());
    }

    private static BlockPos lookedAtBlock(ServerLevel level, ServerPlayer player) {
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getLookAngle().scale(REACH));
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : null;
    }

    /** The lit part of the ring sits just above the block's top face. */
    private static void drawRing(ServerLevel level, BlockPos pos, int ticks, int total) {
        if (ticks % 2 != 0) {
            return;
        }
        int lit = Ring.litPoints(ticks, total, RING_POINTS);
        double cx = pos.getX() + 0.5;
        double cz = pos.getZ() + 0.5;
        double y = pos.getY() + 1.05;
        for (int i = 0; i < lit; i++) {
            level.sendParticles(ParticleTypes.END_ROD, cx + Ring.x(i, RING_POINTS, 0.62), y, cz + Ring.z(i, RING_POINTS, 0.62), 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** The ring closed: roll the tray's table, drop what it found and wear the block one stage. */
    static void finish(ServerLevel level, ServerPlayer player, ItemStack tray, Tier tier, InteractionHand hand, BlockPos pos, PannableBlock block) {
        BlockState state = level.getBlockState(pos);
        List<Drop> found = DropTables.roll(block.material(), tier.level(), tier.rolls(), ProspectMod.dice(level.getRandom()));
        ScriptEvents.fire("panned", player.getName().getString(), Compat.dimensionId(level), pos.getX(), pos.getY(), pos.getZ(),
                ProspectMod.idOf(block), "", found.stream().mapToInt(Drop::count).sum(), "", found);
        for (Drop drop : found) {
            Compat.drop(level, pos, drop);
        }
        level.playSound(null, pos, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 0.7F, 1.0F);
        level.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 4, 0.25, 0.05, 0.25, 0.01);
        if (found.isEmpty()) {
            Compat.bar(player, "message.barelyprospecting.pan_nothing");
        }
        int wear = state.getValue(PannableBlock.WEAR);
        if (wear + 1 >= Settings.panUsesPerBlock()) {
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        } else {
            level.setBlock(pos, state.setValue(PannableBlock.WEAR, wear + 1), Block.UPDATE_ALL);
        }
        if (!player.getAbilities().instabuild) {
            tray.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
        }
    }

    /** Test hook: runs the finish step directly for a player as if the ring had just closed. */
    public static void debugFinish(ServerLevel level, ServerPlayer player, ItemStack tray, Tier tier, BlockPos pos) {
        if (level.getBlockState(pos).getBlock() instanceof PannableBlock block) {
            finish(level, player, tray, tier, InteractionHand.MAIN_HAND, pos, block);
        }
    }
}
