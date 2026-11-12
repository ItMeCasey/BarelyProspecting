package com.itmecasey.barelyprospecting.block;

import com.itmecasey.barelyprospecting.ModContent;
import com.itmecasey.barelyprospecting.ProspectMod;
import com.itmecasey.barelyprospecting.core.BasinState;
import com.itmecasey.barelyprospecting.core.Drop;
import com.itmecasey.barelyprospecting.core.Recipes;
import com.itmecasey.barelyprospecting.server.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The data of one soaking basin: its fluid, the item that is soaking and the result that waits for a hand. Shared by everyone. */
public class BasinBlockEntity extends BlockEntity {
    private final BasinState basin = new BasinState();

    public BasinBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.BASIN_BE, pos, state);
    }

    public BasinState basin() {
        return basin;
    }

    /** Copies the fluid level to the block's look and saves. */
    public void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState now = getBlockState()
                    .setValue(BasinBlock.WATER, basin.fluid() == Recipes.Fluid.WATER ? basin.levels() : 0)
                    .setValue(BasinBlock.LAVA, basin.fluid() == Recipes.Fluid.LAVA ? basin.levels() : 0);
            level.setBlock(worldPosition, now, Block.UPDATE_ALL);
            level.sendBlockUpdated(worldPosition, now, now, Block.UPDATE_ALL);
        }
    }

    /** Server tick: runs the soak and shows it with bubbles. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, BasinBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel) || !be.basin.isSoaking()) {
            return;
        }
        if (level.getGameTime() % 10L == 0L) {
            serverLevel.sendParticles(be.basin.fluid() == Recipes.Fluid.LAVA ? ParticleTypes.LAVA : ParticleTypes.BUBBLE_POP,
                    pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 3, 0.25, 0.05, 0.25, 0.01);
        }
        if (be.basin.tick(Compat.dimensionId(level))) {
            serverLevel.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.6F, 1.2F);
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0.0);
            ProspectMod.LOGGER.debug("Basin at {} finished soaking", pos);
        }
        be.changed();
    }

    /** What a hand takes out of the basin. */
    public Drop takeResult() {
        Drop drop = basin.takeResult();
        changed();
        return drop;
    }

    @Override
    protected void saveAdditional(CompoundTag output) {
        super.saveAdditional(output);
        output.putInt("Fluid", basin.fluidOrdinal());
        output.putInt("Levels", basin.levels());
        output.putString("Soaking", basin.soakingItem());
        output.putInt("TicksLeft", basin.ticksLeft());
        output.putInt("TotalTicks", basin.totalTicks());
        output.putString("ResultItem", basin.resultItem());
        output.putInt("ResultCount", basin.resultCount());
    }

    @Override
    public void load(CompoundTag input) {
        super.load(input);
        basin.load(input.getInt("Fluid"), input.getInt("Levels"), input.getString("Soaking"),
                input.getInt("TicksLeft"), input.getInt("TotalTicks"), input.getString("ResultItem"),
                input.getInt("ResultCount"));
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
