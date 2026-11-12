package com.itmecasey.barelyprospecting.block;

import com.itmecasey.barelyprospecting.ModContent;
import com.itmecasey.barelyprospecting.ProspectMod;
import com.itmecasey.barelyprospecting.core.BasinState;
import com.itmecasey.barelyprospecting.core.Drop;
import com.itmecasey.barelyprospecting.core.Recipes;
import com.itmecasey.barelyprospecting.core.Settings;
import com.itmecasey.barelyprospecting.item.ClayBucketItem;
import com.itmecasey.barelyprospecting.server.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The soaking basin. Fill it by hand (a clay bucket of water or lava, or rain) and drop one item in to soak: silt turns into clay,
 * fine grit into Nether silt in a basin of lava in the Nether, and so on. The result waits until an empty hand takes it.
 */
public class BasinBlock extends BaseEntityBlock {
    public static final IntegerProperty WATER = IntegerProperty.create("water", 0, BasinState.CAPACITY);
    public static final IntegerProperty LAVA = IntegerProperty.create("lava", 0, BasinState.CAPACITY);

    private static final VoxelShape SHAPE = Shapes.join(Block.box(0.0, 0.0, 0.0, 16.0, 9.0, 16.0),
            Block.box(2.0, 2.0, 2.0, 14.0, 9.0, 14.0), BooleanOp.ONLY_FIRST);

    public BasinBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(WATER, 0).setValue(LAVA, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATER, LAVA);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BasinBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModContent.BASIN_BE, BasinBlockEntity::serverTick);
    }

    /** Rain fills a basin that is open to the sky, a little at a time. */
    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (Settings.rainFillsBasins() && level.isRainingAt(pos.above()) && level.getBlockEntity(pos) instanceof BasinBlockEntity be) {
            if (be.basin().fill(Recipes.Fluid.WATER, 1) > 0) {
                be.changed();
            }
        }
    }

    /** One entry point in this version: a held item goes to the item rules, an empty hand to the empty-hand rules. */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            return useWithoutItem(state, level, pos, player, hit);
        }
        return useItemOn(stack, state, level, pos, player, hand, hit);
    }

    private InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof BasinBlockEntity be)) {
            return InteractionResult.PASS;
        }
        BasinState basin = be.basin();
        if (stack.is(net.minecraft.world.item.Items.WATER_BUCKET) || stack.is(net.minecraft.world.item.Items.LAVA_BUCKET)) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            boolean lava = stack.is(net.minecraft.world.item.Items.LAVA_BUCKET);
            if (basin.fill(lava ? Recipes.Fluid.LAVA : Recipes.Fluid.WATER, BasinState.CAPACITY) > 0) {
                be.changed();
                level.playSound(null, pos, lava ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.8F, 1.0F);
                if (!player.getAbilities().instabuild) {
                    player.setItemInHand(hand, new ItemStack(net.minecraft.world.item.Items.BUCKET));
                }
            } else {
                Compat.bar(player, "message.barelyprospecting.basin_full");
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.getItem() instanceof ClayBucketItem bucket) {
            return level.isClientSide() ? InteractionResult.SUCCESS : useBucket(bucket, be, basin, player, hand, level, pos);
        }
        if (stack.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (Recipes.soaks().stream().noneMatch(s -> s.input().equals(ProspectMod.idOf(stack.getItem())))) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Recipes.Soak recipe = basin.start(ProspectMod.idOf(stack.getItem()), Compat.dimensionId(level));
        if (recipe == null) {
            Compat.bar(player, basin.levels() <= 0 ? "message.barelyprospecting.basin_dry" : basin.isSoaking() || basin.hasResult()
                    ? "message.barelyprospecting.basin_busy" : "message.barelyprospecting.basin_wrong");
            return InteractionResult.SUCCESS;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.4F, 1.4F);
        be.changed();
        return InteractionResult.SUCCESS;
    }

    private InteractionResult useBucket(ClayBucketItem bucket, BasinBlockEntity be, BasinState basin, Player player,
                                        InteractionHand hand, Level level, BlockPos pos) {
        ClayBucketItem.Kind kind = bucket.kind();
        if (kind == ClayBucketItem.Kind.EMPTY) {
            if (basin.levels() >= BasinState.CAPACITY && basin.fluid() != Recipes.Fluid.NONE) {
                Recipes.Fluid fluid = basin.fluid();
                basin.drain();
                be.changed();
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.8F, 1.0F);
                player.setItemInHand(hand, new ItemStack(fluid == Recipes.Fluid.LAVA ? ModContent.CLAY_BUCKET_LAVA : ModContent.CLAY_BUCKET_WATER));
            } else {
                Compat.bar(player, "message.barelyprospecting.basin_needs_full");
            }
            return InteractionResult.SUCCESS;
        }
        Recipes.Fluid fluid = kind == ClayBucketItem.Kind.LAVA ? Recipes.Fluid.LAVA : Recipes.Fluid.WATER;
        if (basin.fill(fluid, BasinState.CAPACITY) > 0) {
            be.changed();
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.8F, 1.0F);
            player.setItemInHand(hand, new ItemStack(ModContent.CLAY_BUCKET));
        } else {
            Compat.bar(player, "message.barelyprospecting.basin_full");
        }
        return InteractionResult.SUCCESS;
    }

    /** An empty hand takes the result, or reads the basin when there is nothing to take. */
    private InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof BasinBlockEntity be) {
            BasinState basin = be.basin();
            if (basin.hasResult()) {
                Drop drop = be.takeResult();
                Compat.give(player, ProspectMod.stack(drop.item(), drop.count()));
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.0F);
            } else if (basin.isSoaking()) {
                Compat.bar(player, "message.barelyprospecting.basin_soaking", Math.round(basin.progress() * 100));
            } else {
                Compat.bar(player, "message.barelyprospecting.basin_status", basin.levels(), BasinState.CAPACITY,
                        net.minecraft.network.chat.Component.translatable("fluid.barelyprospecting." + basin.fluid().name().toLowerCase()));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
