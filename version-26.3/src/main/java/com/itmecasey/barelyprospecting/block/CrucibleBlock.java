package com.itmecasey.barelyprospecting.block;

import com.itmecasey.barelyprospecting.ModContent;
import com.itmecasey.barelyprospecting.ProspectMod;
import com.itmecasey.barelyprospecting.core.CrucibleState;
import com.itmecasey.barelyprospecting.core.Drop;
import com.itmecasey.barelyprospecting.core.Recipes;
import com.itmecasey.barelyprospecting.item.ClayBucketItem;
import com.itmecasey.barelyprospecting.server.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The crucible: a clay pot with a fire of its own that you feed by hand. Drop rubble in, keep the fire going and lava collects;
 * take it with a clay bucket, or quench it with a bucket of water for stone and obsidian.
 */
public class CrucibleBlock extends BaseEntityBlock {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    /** 0 empty, 1 a puddle, 2 half, 3 full. */
    public static final IntegerProperty LAVA = IntegerProperty.create("lava", 0, 3);

    private static final VoxelShape SHAPE = Shapes.join(Block.box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0),
            Block.box(4.0, 4.0, 4.0, 12.0, 13.0, 12.0), BooleanOp.ONLY_FIRST);

    public CrucibleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false).setValue(LAVA, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, LAVA);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrucibleBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModContent.CRUCIBLE_BE, CrucibleBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CrucibleBlockEntity be)) {
            return InteractionResult.PASS;
        }
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        CrucibleState crucible = be.crucible();
        String id = ProspectMod.idOf(stack.getItem());
        if (stack.getItem() instanceof ClayBucketItem bucket) {
            return level.isClientSide() ? InteractionResult.SUCCESS : useBucket(bucket, be, crucible, player, hand, level, pos);
        }
        boolean fuel = Recipes.fuelTicks(id) > 0;
        boolean melts = Recipes.meltUnits(id) > 0;
        if (!fuel && !melts) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        boolean used = melts ? crucible.feed(id) > 0 : crucible.addFuel(Recipes.fuelTicks(id)) > 0;
        if (used) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.playSound(null, pos, melts ? SoundEvents.GRAVEL_PLACE : SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.6F, 1.0F);
            be.changed();
        } else {
            Compat.bar(player, melts ? "message.barelyprospecting.crucible_full" : "message.barelyprospecting.crucible_fire_full");
        }
        return InteractionResult.SUCCESS;
    }

    private InteractionResult useBucket(ClayBucketItem bucket, CrucibleBlockEntity be, CrucibleState crucible, Player player,
                                        InteractionHand hand, Level level, BlockPos pos) {
        switch (bucket.kind()) {
            case EMPTY -> {
                if (crucible.takeBucket()) {
                    be.changed();
                    level.playSound(null, pos, SoundEvents.BUCKET_FILL_LAVA, SoundSource.BLOCKS, 0.8F, 1.0F);
                    player.setItemInHand(hand, new ItemStack(ModContent.CLAY_BUCKET_LAVA));
                } else {
                    Compat.bar(player, "message.barelyprospecting.crucible_needs_lava", crucible.lava(), Recipes.BUCKET);
                }
            }
            case WATER -> {
                Drop drop = crucible.quench();
                if (drop == null) {
                    Compat.bar(player, "message.barelyprospecting.crucible_nothing_to_quench");
                } else {
                    be.changed();
                    level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 1.0F);
                    Compat.give(player, ProspectMod.stack(drop.item(), drop.count()));
                    player.setItemInHand(hand, new ItemStack(ModContent.CLAY_BUCKET));
                }
            }
            case LAVA -> {
                if (crucible.pourLava()) {
                    be.changed();
                    level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 0.8F, 1.0F);
                    player.setItemInHand(hand, new ItemStack(ModContent.CLAY_BUCKET));
                } else {
                    Compat.bar(player, "message.barelyprospecting.crucible_full");
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** An empty hand reads the crucible: fire, rubble waiting and lava. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CrucibleBlockEntity be) {
            CrucibleState c = be.crucible();
            Compat.bar(player, "message.barelyprospecting.crucible_status", c.fuel() / 20, c.queued(), c.lava());
        }
        return InteractionResult.SUCCESS;
    }
}
