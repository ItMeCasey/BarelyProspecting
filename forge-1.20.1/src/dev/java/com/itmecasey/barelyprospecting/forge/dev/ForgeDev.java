package com.itmecasey.barelyprospecting.forge.dev;

import com.itmecasey.barelyprospecting.dev.SelfTest;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;

/** Development hooks for Forge 1.20.1: the server self test. Built only with -PdevTools. */
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
    }
}
