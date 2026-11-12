package com.itmecasey.barelyprospecting.neoforge.dev;

import com.itmecasey.barelyprospecting.dev.SelfTest;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Development hooks for NeoForge: the server self test. Built only with -PdevTools. */
public final class NeoForgeDev {
    private NeoForgeDev() {
    }

    public static void install() {
        if (SelfTest.requested()) {
            NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> {
                SelfTest.run(e.getServer());
                e.getServer().halt(false);
            });
        }
    }
}
