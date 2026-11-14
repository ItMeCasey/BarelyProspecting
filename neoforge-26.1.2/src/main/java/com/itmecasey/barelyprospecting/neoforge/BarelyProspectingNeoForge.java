package com.itmecasey.barelyprospecting.neoforge;

import com.itmecasey.barelyprospecting.ModContent;
import com.itmecasey.barelyprospecting.ProspectMod;
import com.itmecasey.barelyprospecting.core.Settings;
import com.itmecasey.barelyprospecting.server.HandDrops;
import com.itmecasey.barelyprospecting.server.StarterIsland;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge entry point for Minecraft 26.x: registers the content, reads the config and forwards game events. */
@Mod(ProspectMod.MODID)
public final class BarelyProspectingNeoForge {
    public BarelyProspectingNeoForge(IEventBus modBus, ModContainer container) {
        Settings.install(new NeoForgeSettings());
        com.itmecasey.barelyprospecting.core.script.Bridges.install();
        container.registerConfig(ModConfig.Type.COMMON, NeoForgeSettings.SPEC);
        modBus.addListener(BarelyProspectingNeoForge::register);

        NeoForge.EVENT_BUS.addListener((BreakBlockEvent event) -> {
            if (event.getPlayer() instanceof ServerPlayer player && event.getLevel() instanceof ServerLevel level) {
                HandDrops.onBlockBroken(player, level, event.getPos(), event.getState());
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> StarterIsland.ensure(event.getServer()));

        if (ProspectMod.DEV) {
            // Development tools (self test) exist only in builds made with -PdevTools.
            try {
                Class.forName("com.itmecasey.barelyprospecting.neoforge.dev.NeoForgeDev").getMethod("install").invoke(null);
            } catch (ReflectiveOperationException e) {
                ProspectMod.LOGGER.warn("barelyprospecting.dev is set but this build has no development tools");
            }
        }
        ProspectMod.LOGGER.info("Barely Prospecting loaded (NeoForge 26.1.2)");
    }

    private static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> ModContent.BLOCKS.forEach((name, block) -> helper.register(ProspectMod.id(name), block)));
        event.register(Registries.ITEM, helper -> ModContent.ITEMS.forEach((name, item) -> helper.register(ProspectMod.id(name), item)));
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> {
            helper.register(ProspectMod.id("basin"), ModContent.BASIN_BE);
            helper.register(ProspectMod.id("crucible"), ModContent.CRUCIBLE_BE);
            helper.register(ProspectMod.id("lure"), ModContent.LURE_BE);
        });
        event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(ProspectMod.id("main"), ModContent.creativeTab()));
    }
}
