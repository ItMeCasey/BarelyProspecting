package com.itmecasey.barelyprospecting.neoforge.dev;

import com.itmecasey.barelyprospecting.dev.SelfTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The single game test that hosts {@link SelfTest} on 1.21.1, where the game test server refuses to start with no tests. */
@GameTestHolder("barelyprospecting")
public final class SelfTestCases {
    private SelfTestCases() {
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = "empty")
    public static void prospecting(GameTestHelper helper) {
        SelfTest.run(helper.getLevel().getServer());
        helper.succeed();
    }
}
