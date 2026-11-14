package com.itmecasey.barelyprospecting.server;

import com.itmecasey.barelyprospecting.block.BasinBlockEntity;
import com.itmecasey.barelyprospecting.block.CrucibleBlockEntity;
import com.itmecasey.barelyprospecting.block.LureBlockEntity;
import com.itmecasey.barelyprospecting.block.PannableBlock;
import com.itmecasey.barelyprospecting.core.BasinState;
import com.itmecasey.barelyprospecting.core.CrucibleState;
import com.itmecasey.barelyprospecting.core.LureState;
import com.itmecasey.barelyprospecting.core.script.ProspectApi;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Gives scripts read access to the blocks of this mod (ProspectApi.blockAt). Attached each time a server starts. */
public final class ScriptState {
    private static volatile MinecraftServer server;

    private ScriptState() {
    }

    public static void attach(MinecraftServer started) {
        server = started;
        ProspectApi.install(ScriptState::read);
    }

    private static Map<String, Object> read(String dimension, int x, int y, int z) {
        MinecraftServer s = server;
        if (s == null || !s.isRunning()) {
            return null;
        }
        for (ServerLevel level : s.getAllLevels()) {
            if (Compat.dimensionId(level).equals(dimension)) {
                BlockPos pos = new BlockPos(x, y, z);
                return level.hasChunkAt(pos) ? describe(level.getBlockState(pos), level.getBlockEntity(pos)) : null;
            }
        }
        return null;
    }

    static Map<String, Object> describe(BlockState state, BlockEntity entity) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (entity instanceof BasinBlockEntity be) {
            BasinState b = be.basin();
            out.put("kind", "basin");
            out.put("fluid", b.fluid().name().toLowerCase(Locale.ROOT));
            out.put("levels", b.levels());
            out.put("soaking", b.soakingItem());
            out.put("progress", b.progress());
            out.put("result", b.resultItem());
            out.put("resultCount", b.resultCount());
        } else if (entity instanceof CrucibleBlockEntity be) {
            CrucibleState c = be.crucible();
            out.put("kind", "crucible");
            out.put("fireSeconds", c.fuel() / 20);
            out.put("queued", c.queued());
            out.put("lava", c.lava());
        } else if (entity instanceof LureBlockEntity be) {
            LureState l = be.lure();
            out.put("kind", "lure");
            out.put("table", l.table());
            out.put("meals", l.meals());
            out.put("secondsLeft", l.ticksLeft() / 20);
        } else if (state.getBlock() instanceof PannableBlock p) {
            out.put("kind", "pannable");
            out.put("material", p.material());
            out.put("wear", state.getValue(PannableBlock.WEAR));
        } else {
            return null;
        }
        return out;
    }
}
