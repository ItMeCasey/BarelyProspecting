package com.itmecasey.barelyprospecting.compat;

import com.itmecasey.barelyprospecting.ProspectMod;
import com.itmecasey.barelyprospecting.core.Tier;
import com.itmecasey.barelyprospecting.core.ViewerData;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * What every recipe viewer plugin shares: turning the item ids of {@link ViewerData} into stacks, one card layout for all five
 * categories, and the tooltips. No viewer classes are used here, so it loads with or without any viewer installed.
 */
public final class ViewerSupport {
    public static final int SLOT = 18;
    public static final int PAD = 6;
    public static final int ARROW = 22;
    public static final int COLUMNS = 6;
    public static final int NOTE_HEIGHT = 12;

    private ViewerSupport() {
    }

    /** The widest page of a kind decides how many slots its category has. */
    public record Shape(int inputs, int outputs, int columns, int rows, int width, int height) {
        public int inputX() {
            return PAD;
        }

        public int inputY(int i) {
            return PAD + i * SLOT;
        }

        public int arrowX() {
            return PAD + SLOT + 4;
        }

        public int outputX(int i) {
            return arrowX() + ARROW + 6 + (i % columns) * SLOT;
        }

        public int outputY(int i) {
            return PAD + (i / columns) * SLOT;
        }
    }

    public static Shape shape(ViewerData.Kind kind) {
        int inputs = 1;
        int outputs = 1;
        for (ViewerData.Page page : ViewerData.pages(kind)) {
            inputs = Math.max(inputs, page.inputs().size());
            outputs = Math.max(outputs, page.outputs().size());
        }
        int columns = Math.min(COLUMNS, outputs);
        int rows = (outputs + columns - 1) / columns;
        int body = Math.max(inputs, rows) * SLOT;
        int width = Math.max(190, PAD + SLOT + 4 + ARROW + 6 + columns * SLOT + PAD);
        return new Shape(inputs, outputs, columns, rows, width, PAD + body + NOTE_HEIGHT + 4);
    }

    public static ItemStack stack(String id, int count) {
        return ProspectMod.stack(id, count);
    }

    /** The stacks that may sit in one input slot (items that do not exist are left out). */
    public static List<ItemStack> alternatives(List<String> ids) {
        List<ItemStack> out = new ArrayList<>();
        for (String id : ids) {
            ItemStack stack = ProspectMod.stack(id, 1);
            if (!stack.isEmpty()) {
                out.add(stack);
            }
        }
        return out;
    }

    public static Component title(ViewerData.Kind kind) {
        return Component.translatable("viewer.barelyprospecting." + kind.path());
    }

    public static Component note(ViewerData.Page page) {
        if (page.noteKey().isEmpty()) {
            return Component.empty();
        }
        Object[] args = page.noteArgs().stream().map(a -> a instanceof Integer ? (Object) a : (Object) a.toString()).toArray();
        if (page.kind() == ViewerData.Kind.SOAK && args.length == 2 && args[1].toString().isEmpty()) {
            args = new Object[] {args[0], Component.translatable("viewer.barelyprospecting.anywhere")};
        } else if (page.kind() == ViewerData.Kind.SOAK && args.length == 2) {
            args = new Object[] {args[0], Component.translatable("viewer.barelyprospecting.dimension." + args[1])};
        }
        if (page.kind() == ViewerData.Kind.PAN) {
            int min = (Integer) page.noteArgs().get(0);
            return min <= 0 ? Component.translatable("viewer.barelyprospecting.pan_any")
                    : Component.translatable(page.noteKey(), Component.translatable("item.barelyprospecting." + Tier.byLevel(min).itemPath()));
        }
        return Component.translatable(page.noteKey(), args);
    }

    /** The hover lines of one output: how many, and the chance for each tray (panning) or per roll (the lure). */
    public static List<Component> tooltip(ViewerData.Kind kind, ViewerData.Out out) {
        List<Component> lines = new ArrayList<>();
        if (out.max() > 1 || out.min() > 1) {
            lines.add(Component.translatable("viewer.barelyprospecting.count", out.min() == out.max() ? String.valueOf(out.min()) : out.min() + "-" + out.max())
                    .withStyle(ChatFormatting.GRAY));
        }
        float[] chances = out.chances();
        if (kind == ViewerData.Kind.PAN) {
            for (int t = 0; t < chances.length; t++) {
                lines.add(Component.translatable("item.barelyprospecting." + Tier.byLevel(t).itemPath()).append(": ")
                        .append(chances[t] <= 0 ? "-" : String.format(Locale.ROOT, "%.1f%%", chances[t] * 100)).withStyle(ChatFormatting.GRAY));
            }
        } else if (chances.length == 1) {
            lines.add(Component.translatable("viewer.barelyprospecting.chance", String.format(Locale.ROOT, "%.1f", chances[0] * 100))
                    .withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    /** Which output slot is under the mouse, or -1. */
    public static int outputAt(Shape shape, int count, int mouseX, int mouseY) {
        for (int i = 0; i < count && i < shape.outputs(); i++) {
            int x = shape.outputX(i);
            int y = shape.outputY(i);
            if (mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT) {
                return i;
            }
        }
        return -1;
    }
}
