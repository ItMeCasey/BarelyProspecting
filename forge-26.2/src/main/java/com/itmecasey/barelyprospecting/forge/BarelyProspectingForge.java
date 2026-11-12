package com.itmecasey.barelyprospecting.forge;

import com.itmecasey.barelyprospecting.ModContent;
import com.itmecasey.barelyprospecting.ProspectMod;
import com.itmecasey.barelyprospecting.core.Settings;
import com.itmecasey.barelyprospecting.server.HandDrops;
import com.itmecasey.barelyprospecting.server.StarterIsland;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;

/** Forge entry point for Minecraft 26.x: registers the content, reads the config and forwards game events. */
@Mod(ProspectMod.MODID)
public final class BarelyProspectingForge {
    public BarelyProspectingForge(FMLJavaModLoadingContext context) {
        Settings.install(new ForgeSettings());
        context.registerConfig(ModConfig.Type.COMMON, ForgeSettings.SPEC);
        RegisterEvent.getBus(context.getModBusGroup()).addListener(BarelyProspectingForge::register);

        BlockEvent.BreakEvent.BUS.addListener(event -> {
            if (event.getPlayer() instanceof ServerPlayer player && event.getLevel() instanceof ServerLevel level) {
                HandDrops.onBlockBroken(player, level, event.getPos(), event.getState());
            }
            return false;
        });
        ServerStartedEvent.BUS.addListener(event -> StarterIsland.ensure(event.getServer()));

        if (ProspectMod.DEV) {
            // Development tools (self test) exist only in builds made with -PdevTools.
            try {
                Class.forName("com.itmecasey.barelyprospecting.forge.dev.ForgeDev").getMethod("install").invoke(null);
            } catch (ReflectiveOperationException e) {
                ProspectMod.LOGGER.warn("barelyprospecting.dev is set but this build has no development tools");
            }
        }
        ProspectMod.LOGGER.info("Barely Prospecting loaded (Forge 26.2)");
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
