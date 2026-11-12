package com.itmecasey.barelyprospecting.block;

import com.itmecasey.barelyprospecting.ModContent;
import com.itmecasey.barelyprospecting.ProspectMod;
import com.itmecasey.barelyprospecting.core.Drop;
import com.itmecasey.barelyprospecting.core.LureState;
import com.itmecasey.barelyprospecting.server.Compat;
import java.util.List;
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

/** The data of one lure: what it was fed, how many meals, and how long until something answers. Shared by everyone. */
public class LureBlockEntity extends BlockEntity {
    private final LureState lure = new LureState();

    public LureBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.LURE_BE, pos, state);
    }

    public LureState lure() {
        return lure;
    }

    /** Copies the number of meals to the block's look and saves. */
    public void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState now = getBlockState().setValue(LureBlock.MEALS, lure.meals());
            level.setBlock(worldPosition, now, Block.UPDATE_ALL);
            level.sendBlockUpdated(worldPosition, now, now, Block.UPDATE_ALL);
        }
    }

    /** Server tick: counts down the hatch and, when it ends, lets the drops fall above the lure. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, LureBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel) || !be.lure.isHatching()) {
            return;
        }
        if (level.getGameTime() % 10L == 0L) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.2, 0.1, 0.2, 0.0);
        }
        List<Drop> drops = be.lure.tick(ProspectMod.dice(level.getRandom()));
        if (drops != null) {
            serverLevel.playSound(null, pos, SoundEvents.ALLAY_ITEM_GIVEN, SoundSource.BLOCKS, 0.8F, 0.8F);
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 8, 0.3, 0.2, 0.3, 0.0);
            for (Drop drop : drops) {
                Compat.drop(serverLevel, pos, drop);
            }
            be.changed();
        } else if (level.getGameTime() % 40L == 0L) {
            be.setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag output) {
        super.saveAdditional(output);
        output.putString("Table", lure.table());
        output.putInt("Meals", lure.meals());
        output.putInt("TicksLeft", lure.ticksLeft());
    }

    @Override
    public void load(CompoundTag input) {
        super.load(input);
        lure.load(input.getString("Table"), input.getInt("Meals"), input.getInt("TicksLeft"));
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
