package com.itmecasey.barelyprospecting.compat.jade;

import com.itmecasey.barelyprospecting.ProspectMod;
import com.itmecasey.barelyprospecting.block.BasinBlock;
import com.itmecasey.barelyprospecting.block.BasinBlockEntity;
import com.itmecasey.barelyprospecting.block.CrucibleBlockEntity;
import com.itmecasey.barelyprospecting.block.LureBlockEntity;
import com.itmecasey.barelyprospecting.block.CrucibleBlock;
import com.itmecasey.barelyprospecting.block.LureBlock;
import com.itmecasey.barelyprospecting.block.PannableBlock;
import com.itmecasey.barelyprospecting.compat.LookInfo;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
    private static final ResourceLocation UID = ProspectMod.id("look_info");
    private static final ResourceLocation PAN_UID = ProspectMod.id("pannable_info");
    private static final ResourceLocation DATA_UID = ProspectMod.id("look_info_data");
    private static final String TAG = "barelyprospecting_lines";

    private static final List<Class<? extends Block>> MACHINES = List.of(BasinBlock.class, CrucibleBlock.class, LureBlock.class);

    private static final List<Class<? extends BlockEntity>> MACHINE_ENTITIES = List.of(BasinBlockEntity.class, CrucibleBlockEntity.class, LureBlockEntity.class);

    @Override
    public void register(IWailaCommonRegistration registration) {
        for (Class<? extends BlockEntity> entity : MACHINE_ENTITIES) {
            registration.registerBlockDataProvider(ServerData.INSTANCE, entity);
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
                ListTag lines = new ListTag();
                for (Component line : LookInfo.lines(entity)) {
                    lines.add(StringTag.valueOf(Component.Serializer.toJson(line)));
                }
                data.put(TAG, lines);
            }
        }

        @Override
        public ResourceLocation getUid() {
            return DATA_UID;
        }
    }

    /** The client half for machines: prints the lines the server sent. */
    private enum MachineTooltip implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            ListTag lines = accessor.getServerData().getList(TAG, Tag.TAG_STRING);
            for (int i = 0; i < lines.size(); i++) {
                Component line = Component.Serializer.fromJson(lines.getString(i));
                if (line != null) {
                    tooltip.add(line);
                }
            }
        }

        @Override
        public ResourceLocation getUid() {
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
        public ResourceLocation getUid() {
            return PAN_UID;
        }
    }
}
