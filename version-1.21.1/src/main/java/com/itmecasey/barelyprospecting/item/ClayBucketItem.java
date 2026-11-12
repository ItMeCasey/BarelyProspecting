package com.itmecasey.barelyprospecting.item;

import com.itmecasey.barelyprospecting.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The fired clay bucket: empty, or carrying a bucket of water or lava. It is filled from a basin or a crucible (their blocks do
 * that, because they are the ones that hold the fluid) and poured out on the ground here.
 */
public class ClayBucketItem extends Item {
    /** What the bucket carries. */
    public enum Kind {
        EMPTY, WATER, LAVA
    }

    private final Kind kind;

    public ClayBucketItem(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    /** Pours the fluid onto the block next to the clicked face (a source block), when the space is free. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (kind == Kind.EMPTY) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos target = context.getClickedPos().relative(context.getClickedFace());
        BlockState there = level.getBlockState(target);
        if (!there.canBeReplaced() || !there.getFluidState().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        boolean lava = kind == Kind.LAVA;
        if (!lava && level.dimension() == Level.NETHER) {
            level.playSound(null, target, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.0F);
        } else {
            level.setBlock(target, (lava ? Blocks.LAVA : Blocks.WATER).defaultBlockState(), 11);
            level.playSound(null, target, lava ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        Player player = context.getPlayer();
        if (player != null && !player.getAbilities().instabuild) {
            player.setItemInHand(context.getHand(), new ItemStack(ModContent.CLAY_BUCKET));
        }
        return InteractionResult.SUCCESS;
    }
}
