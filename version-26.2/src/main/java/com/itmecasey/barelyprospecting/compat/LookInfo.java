package com.itmecasey.barelyprospecting.compat;

import com.itmecasey.barelyprospecting.ProspectMod;
import com.itmecasey.barelyprospecting.block.BasinBlockEntity;
import com.itmecasey.barelyprospecting.block.CrucibleBlockEntity;
import com.itmecasey.barelyprospecting.block.LureBlockEntity;
import com.itmecasey.barelyprospecting.block.PannableBlock;
import com.itmecasey.barelyprospecting.core.BasinState;
import com.itmecasey.barelyprospecting.core.CrucibleState;
import com.itmecasey.barelyprospecting.core.DropEntry;
import com.itmecasey.barelyprospecting.core.DropTables;
import com.itmecasey.barelyprospecting.core.LureState;
import com.itmecasey.barelyprospecting.core.Settings;
import com.itmecasey.barelyprospecting.core.Tier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The lines the look-at overlays (SMWTI and Jade) show for the mod's blocks. The server works out the lines for blocks that have a
 * block entity (the real state lives there); the client works out the lines for pannable blocks from the block state alone.
 */
public final class LookInfo {
    private LookInfo() {
    }

    private static Component name(String id) {
        ItemStack stack = ProspectMod.stack(id, 1);
        return stack.isEmpty() ? Component.literal(id) : stack.getHoverName();
    }

    /** Lines for a basin, crucible or lure; empty for anything else. Call on the server. */
    public static List<Component> lines(BlockEntity entity) {
        List<Component> lines = new ArrayList<>();
        if (entity instanceof BasinBlockEntity be) {
            BasinState basin = be.basin();
            lines.add(Component.translatable("look.barelyprospecting.basin_fluid", basin.levels(), BasinState.CAPACITY,
                    Component.translatable("fluid.barelyprospecting." + basin.fluid().name().toLowerCase(Locale.ROOT))));
            if (basin.isSoaking()) {
                lines.add(Component.translatable("look.barelyprospecting.soaking", name(basin.soakingItem()),
                        Math.round(basin.progress() * 100)).withStyle(ChatFormatting.YELLOW));
            } else if (basin.hasResult()) {
                lines.add(Component.translatable("look.barelyprospecting.result", basin.resultCount(), name(basin.resultItem()))
                        .withStyle(ChatFormatting.GREEN));
            }
        } else if (entity instanceof CrucibleBlockEntity be) {
            CrucibleState c = be.crucible();
            lines.add(Component.translatable(c.isBurning() ? "look.barelyprospecting.fire" : "look.barelyprospecting.fire_out", c.fuel() / 20));
            lines.add(Component.translatable("look.barelyprospecting.lava", c.lava(), CrucibleState.CAPACITY));
            if (c.queued() > 0) {
                lines.add(Component.translatable("look.barelyprospecting.queued", c.queued()).withStyle(ChatFormatting.GRAY));
            }
        } else if (entity instanceof LureBlockEntity be) {
            LureState lure = be.lure();
            lines.add(Component.translatable("look.barelyprospecting.meals", lure.meals(), LureState.MEALS));
            if (lure.isHatching()) {
                lines.add(Component.translatable("look.barelyprospecting.hatching", lure.ticksLeft() / 20 + 1).withStyle(ChatFormatting.YELLOW));
            } else if (!lure.table().isEmpty()) {
                lines.add(Component.translatable("look.barelyprospecting.table_" + lure.table()).withStyle(ChatFormatting.GRAY));
            }
        }
        return lines;
    }

    /** Lines for a pannable block: how many pans it has left, which tray it needs and what it can yield. Works on the client. */
    public static List<Component> pannable(BlockState state) {
        List<Component> lines = new ArrayList<>();
        if (!(state.getBlock() instanceof PannableBlock block)) {
            return lines;
        }
        int left = Settings.panUsesPerBlock() - state.getValue(PannableBlock.WEAR);
        lines.add(Component.translatable("look.barelyprospecting.pans_left", Math.max(1, left)));
        int min = DropTables.minimumTier(block.material());
        if (min > 0) {
            lines.add(Component.translatable("look.barelyprospecting.needs_tray",
                    Component.translatable("item.barelyprospecting." + Tier.byLevel(min).itemPath())).withStyle(ChatFormatting.RED));
        }
        int level = Math.max(min, 0) + DropTables.tierShift(block.material());
        List<DropEntry> rows = new ArrayList<>(DropTables.table(block.material()));
        rows.removeIf(r -> r.isNothing() || r.weightFor(level) <= 0);
        rows.sort(Comparator.comparingDouble((DropEntry r) -> -r.weightFor(level)));
        Component finds = Component.empty();
        for (int i = 0; i < Math.min(5, rows.size()); i++) {
            finds = finds.copy().append(i == 0 ? Component.empty() : Component.literal(", ")).append(name(rows.get(i).item()));
        }
        lines.add(Component.translatable("look.barelyprospecting.finds", finds).withStyle(ChatFormatting.GRAY));
        return lines;
    }
}
