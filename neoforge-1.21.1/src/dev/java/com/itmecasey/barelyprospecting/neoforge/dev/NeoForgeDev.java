package com.itmecasey.barelyprospecting.neoforge.dev;

import com.itmecasey.barelyprospecting.dev.SelfTest;
import net.neoforged.bus.api.IEventBus;

/** Development hooks for NeoForge 1.21.1: the self test. Built only with -PdevTools. */
public final class NeoForgeDev {
    private NeoForgeDev() {
    }

    public static void install(IEventBus modBus) {
        if (SelfTest.requested()) {
            // 1.21.1's game test server refuses to start with no tests, so the self test lives in one game test.
            modBus.addListener((net.neoforged.neoforge.event.RegisterGameTestsEvent e) -> e.register(SelfTestCases.class));
        }
    }
}
