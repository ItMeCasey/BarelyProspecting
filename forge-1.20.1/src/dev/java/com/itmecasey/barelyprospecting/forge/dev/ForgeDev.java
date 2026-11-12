package com.itmecasey.barelyprospecting.forge.dev;

import com.itmecasey.barelyprospecting.dev.SelfTest;
import com.itmecasey.barelyprospecting.dev.ShotTour;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Development hooks for Forge 1.20.1 (and the NeoForge 1.20.1 fork): the server self test and the client screenshot tour. Built only with -PdevTools. */
public final class ForgeDev {
    private ForgeDev() {
    }

    public static void install() {
        if (SelfTest.requested()) {
            MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent e) -> {
                SelfTest.run(e.getServer());
                e.getServer().halt(false);
            });
        }
        if (FMLEnvironment.dist.isClient() && System.getenv("BARELYPROSPECTING_SHOTS") != null) {
            MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> {
                if (e.phase == TickEvent.Phase.END) {
                    ShotTour.tick();
                }
            });
        }
    }
}
