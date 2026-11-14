package com.itmecasey.barelyprospecting.block;

import com.itmecasey.barelyprospecting.core.script.ScriptEvents;
import com.itmecasey.barelyprospecting.ModContent;
import com.itmecasey.barelyprospecting.ProspectMod;
import com.itmecasey.barelyprospecting.core.LureState;
import com.itmecasey.barelyprospecting.core.Recipes;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The lure: a nest of twigs and copper that you feed three times by hand. Seeds call up the farm table (feathers, eggs, flesh),
 * flesh and bone call up the night table (string, gunpowder, ender pearls). It does nothing without your bait.
 */
public class LureBlock extends BaseEntityBlock {
    /** How many of the three meals it has eaten; the look changes with each. */
    public static final IntegerProperty MEALS = IntegerProperty.create("meals", 0, LureState.MEALS);

    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 7.0, 14.0);

    public LureBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(MEALS, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MEALS);
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
        return new LureBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModContent.LURE_BE, LureBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (Recipes.lureTable(ProspectMod.idOf(stack.getItem())) == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof LureBlockEntity be) {
            String baitId = ProspectMod.idOf(stack.getItem());
            if (be.lure().feed(baitId)) {
                ScriptEvents.fire("lureFed", player.getName().getString(), Compat.dimensionId(level), pos.getX(), pos.getY(), pos.getZ(),
                        baitId, "", be.lure().meals(), be.lure().table(), null);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.GENERIC_EAT.value(), SoundSource.BLOCKS, 0.6F, 1.3F);
                be.changed();
            } else {
                Compat.bar(player, be.lure().isHatching() ? "message.barelyprospecting.lure_hatching" : "message.barelyprospecting.lure_other_bait");
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** An empty hand reads the lure. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof LureBlockEntity be) {
            LureState lure = be.lure();
            if (lure.isHatching()) {
                Compat.bar(player, "message.barelyprospecting.lure_waiting", lure.ticksLeft() / 20 + 1);
            } else {
                Compat.bar(player, "message.barelyprospecting.lure_status", lure.meals(), LureState.MEALS);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
