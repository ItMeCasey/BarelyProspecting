package com.itmecasey.barelyprospecting.forge.dev;

import com.itmecasey.barelyprospecting.dev.SelfTest;
import com.itmecasey.barelyprospecting.dev.ShotTour;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Development hooks for Forge: the server self test. Built only with -PdevTools. */
public final class ForgeDev {
    private ForgeDev() {
    }

    public static void install() {
        if (SelfTest.requested()) {
            ServerStartedEvent.BUS.addListener((ServerStartedEvent e) -> {
                SelfTest.run(e.getServer());
                e.getServer().halt(false);
            });
        }
        if (FMLEnvironment.dist.isClient() && System.getenv("BARELYPROSPECTING_SHOTS") != null) {
            TickEvent.ClientTickEvent.Post.BUS.addListener((TickEvent.ClientTickEvent.Post e) -> ShotTour.tick());
        }
    }
}
