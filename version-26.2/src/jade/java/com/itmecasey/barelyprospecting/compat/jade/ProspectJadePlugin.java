package com.itmecasey.barelyprospecting.compat.jade;

import com.itmecasey.barelyprospecting.ProspectMod;
import com.itmecasey.barelyprospecting.block.BasinBlock;
import com.itmecasey.barelyprospecting.block.CrucibleBlock;
import com.itmecasey.barelyprospecting.block.LureBlock;
import com.itmecasey.barelyprospecting.block.PannableBlock;
import com.itmecasey.barelyprospecting.compat.LookInfo;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade support. The server works out the lines for a basin, crucible or lure (see {@link LookInfo}) and sends them with Jade's own
 * data; the client prints them. Pannable blocks are described on the client from their block state. Jade finds this class by its
 * annotation and only when Jade is installed, so the mod runs fine without it.
 */
@WailaPlugin
public class ProspectJadePlugin implements IWailaPlugin {
    private static final Identifier UID = ProspectMod.id("look_info");
    private static final Identifier PAN_UID = ProspectMod.id("pannable_info");
    private static final Identifier DATA_UID = ProspectMod.id("look_info_data");
    private static final String TAG = "barelyprospecting_lines";

    private static final List<Class<? extends Block>> MACHINES = List.of(BasinBlock.class, CrucibleBlock.class, LureBlock.class);

    @Override
    public void register(IWailaCommonRegistration registration) {
        for (Class<? extends Block> block : MACHINES) {
            registration.registerBlockDataProvider(ServerData.INSTANCE, block);
        }
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        for (Class<? extends Block> block : MACHINES) {
            registration.registerBlockComponent(MachineTooltip.INSTANCE, block);
        }
        registration.registerBlockComponent(PannableTooltip.INSTANCE, PannableBlock.class);
    }

    /** The server half: works the lines out and sends them. */
    private enum ServerData implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            BlockEntity entity = accessor.getBlockEntity();
            if (entity != null) {
                data.store(TAG, ComponentSerialization.CODEC.listOf(), LookInfo.lines(entity));
            }
        }

        @Override
        public Identifier getUid() {
            return DATA_UID;
        }
    }

    /** The client half for machines: prints the lines the server sent. */
    private enum MachineTooltip implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            List<Component> lines = accessor.getServerData().read(TAG, ComponentSerialization.CODEC.listOf()).orElse(List.of());
            lines.forEach(tooltip::add);
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }

    /** Pannable blocks: pans left, the tray they need and what they yield, all from the block state. */
    private enum PannableTooltip implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            LookInfo.pannable(accessor.getBlockState()).forEach(tooltip::add);
        }

        @Override
        public Identifier getUid() {
            return PAN_UID;
        }
    }
}
