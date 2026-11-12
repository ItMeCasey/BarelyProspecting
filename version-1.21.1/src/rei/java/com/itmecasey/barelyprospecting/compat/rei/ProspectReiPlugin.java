package com.itmecasey.barelyprospecting.compat.rei;

import com.itmecasey.barelyprospecting.ProspectMod;
import com.itmecasey.barelyprospecting.compat.ViewerSupport;
import com.itmecasey.barelyprospecting.core.ViewerData;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * REI support: one page type each for panning, hammering, soaking, melting and the lure, built from the same data as the other
 * viewers. The displays are made on the client and never travel. REI finds this class by its annotation and only when REI is
 * installed.
 */
@REIPluginClient
public class ProspectReiPlugin implements REIClientPlugin {
    private static CategoryIdentifier<PageDisplay> id(ViewerData.Kind kind) {
        return CategoryIdentifier.of(ProspectMod.id(kind.path()));
    }

    /** For the dev tours: opens REI's recipe screen on the pages of one kind. */
    public static boolean show(String kindPath) {
        for (ViewerData.Kind kind : ViewerData.Kind.values()) {
            if (kind.path().equals(kindPath)) {
                return ViewSearchBuilder.builder().addCategory(id(kind)).open();
            }
        }
        return false;
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        for (ViewerData.Kind kind : ViewerData.Kind.values()) {
            registry.add(new Category(kind));
            registry.configure(id(kind), config -> {
                for (String station : ViewerData.pages(kind).get(0).catalysts()) {
                    ItemStack stack = ViewerSupport.stack(station, 1);
                    if (!stack.isEmpty()) {
                        config.addWorkstations(EntryStacks.of(stack));
                    }
                }
            });
        }
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        for (ViewerData.Page page : ViewerData.all()) {
            registry.add(new PageDisplay(page));
        }
    }

    /** One page as REI sees it. */
    static final class PageDisplay extends BasicDisplay {
        private final ViewerData.Page page;

        PageDisplay(ViewerData.Page page) {
            super(inputs(page), outputs(page), Optional.of(ProspectMod.id(page.kind().path() + "/" + page.id())));
            this.page = page;
        }

        ViewerData.Page page() {
            return page;
        }

        private static List<EntryIngredient> inputs(ViewerData.Page page) {
            List<EntryIngredient> inputs = new ArrayList<>();
            for (List<String> slot : page.inputs()) {
                inputs.add(EntryIngredients.ofItemStacks(ViewerSupport.alternatives(slot)));
            }
            return inputs;
        }

        private static List<EntryIngredient> outputs(ViewerData.Page page) {
            List<EntryIngredient> outputs = new ArrayList<>();
            for (ViewerData.Out out : page.outputs()) {
                outputs.add(EntryIngredients.of(ViewerSupport.stack(out.item(), out.max())));
            }
            return outputs;
        }

        @Override
        public CategoryIdentifier<?> getCategoryIdentifier() {
            return id(page.kind());
        }
    }

    private static final class Category implements DisplayCategory<PageDisplay> {
        private final ViewerData.Kind kind;
        private final ViewerSupport.Shape shape;

        Category(ViewerData.Kind kind) {
            this.kind = kind;
            this.shape = ViewerSupport.shape(kind);
        }

        @Override
        public CategoryIdentifier<? extends PageDisplay> getCategoryIdentifier() {
            return id(kind);
        }

        @Override
        public Component getTitle() {
            return ViewerSupport.title(kind);
        }

        @Override
        public Renderer getIcon() {
            return EntryStacks.of(ViewerSupport.stack(ViewerData.pages(kind).get(0).catalysts().get(0), 1));
        }

        @Override
        public int getDisplayHeight() {
            return shape.height() + 8;
        }

        @Override
        public int getDisplayWidth(PageDisplay display) {
            return shape.width() + 8;
        }

        @Override
        public List<Widget> setupDisplay(PageDisplay display, Rectangle bounds) {
            List<Widget> widgets = new ArrayList<>();
            widgets.add(Widgets.createRecipeBase(bounds));
            int ox = bounds.x + 4;
            int oy = bounds.y + 4;
            List<EntryIngredient> inputs = display.getInputEntries();
            for (int i = 0; i < inputs.size(); i++) {
                widgets.add(Widgets.createSlot(new Point(ox + shape.inputX(), oy + shape.inputY(i))).entries(inputs.get(i)).markInput());
            }
            widgets.add(Widgets.createArrow(new Point(ox + shape.arrowX(), oy + ViewerSupport.PAD)));
            ViewerData.Page page = display.page();
            for (int i = 0; i < page.outputs().size(); i++) {
                List<Component> lines = ViewerSupport.tooltip(kind, page.outputs().get(i));
                widgets.add(Widgets.createSlot(new Point(ox + shape.outputX(i), oy + shape.outputY(i)))
                        .entries(display.getOutputEntries().get(i).map(stack -> lines.isEmpty() ? stack : stack.copy().tooltip(lines.toArray(new Component[0]))))
                        .markOutput());
            }
            widgets.add(Widgets.createLabel(new Point(ox + ViewerSupport.PAD, oy + shape.height() - ViewerSupport.NOTE_HEIGHT), ViewerSupport.note(page))
                    .leftAligned().noShadow().color(0xFF606060));
            return widgets;
        }
    }
}
