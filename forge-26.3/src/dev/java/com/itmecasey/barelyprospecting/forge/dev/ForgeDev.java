package com.itmecasey.barelyprospecting.forge.dev;

import com.itmecasey.barelyprospecting.dev.SelfTest;
import net.minecraftforge.event.server.ServerStartedEvent;

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
    }
}
