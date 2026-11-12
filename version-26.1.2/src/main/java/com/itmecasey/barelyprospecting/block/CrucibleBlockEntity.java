package com.itmecasey.barelyprospecting.block;

import com.itmecasey.barelyprospecting.ModContent;
import com.itmecasey.barelyprospecting.core.CrucibleState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The data of one crucible: its fire, the rubble waiting to melt and the lava in the pot. Shared by everyone. */
public class CrucibleBlockEntity extends BlockEntity {
    private final CrucibleState crucible = new CrucibleState();

    public CrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.CRUCIBLE_BE, pos, state);
    }

    public CrucibleState crucible() {
        return crucible;
    }

    /** Copies the fire and the lava level to the block's look and saves. */
    public void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState now = getBlockState()
                    .setValue(CrucibleBlock.LIT, crucible.isBurning())
                    .setValue(CrucibleBlock.LAVA, crucible.fillLevel());
            level.setBlock(worldPosition, now, Block.UPDATE_ALL);
            level.sendBlockUpdated(worldPosition, now, now, Block.UPDATE_ALL);
        }
    }

    /** Server tick: burns fuel, melts rubble and shows it with smoke and the odd lava pop. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, CrucibleBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel) || !be.crucible.isBurning()) {
            return;
        }
        boolean visible = be.crucible.tick();
        long time = level.getGameTime();
        if (time % 8L == 0L) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 1, 0.15, 0.05, 0.15, 0.0);
            if (be.crucible.queued() > 0) {
                serverLevel.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 1, 0.2, 0.0, 0.2, 0.0);
            }
        }
        if (time % 60L == 0L) {
            serverLevel.playSound(null, pos, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.5F, 1.0F);
        }
        if (visible) {
            be.changed();
        } else if (time % 40L == 0L) {
            be.setChanged();
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Fuel", crucible.fuel());
        output.putInt("Queued", crucible.queued());
        output.putInt("Lava", crucible.lava());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        crucible.load(input.getIntOr("Fuel", 0), input.getIntOr("Queued", 0), input.getIntOr("Lava", 0));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
