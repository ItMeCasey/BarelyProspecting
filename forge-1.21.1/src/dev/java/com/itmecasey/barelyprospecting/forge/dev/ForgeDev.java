package com.itmecasey.barelyprospecting.forge.dev;

import com.itmecasey.barelyprospecting.dev.SelfTest;

/** Development hooks for Forge 1.21.1: the self test. Built only with -PdevTools. */
public final class ForgeDev {
    private ForgeDev() {
    }

    public static void install() {
        if (SelfTest.requested()) {
            // 1.21.1's game test server refuses to start with no tests, so the self test lives in one game test.
            net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus()
                    .addListener((net.minecraftforge.event.RegisterGameTestsEvent e) -> e.register(SelfTestCases.class));
        }
    }
}
