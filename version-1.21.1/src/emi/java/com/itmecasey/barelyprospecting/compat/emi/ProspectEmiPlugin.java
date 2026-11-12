package com.itmecasey.barelyprospecting.compat.emi;

import com.itmecasey.barelyprospecting.ProspectMod;
import com.itmecasey.barelyprospecting.compat.ViewerSupport;
import com.itmecasey.barelyprospecting.core.ViewerData;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * EMI support: one category each for panning, hammering, soaking, melting and the lure, built from the same data as the BEI, JEI and
 * REI pages. EMI finds this class by its annotation and only when EMI is installed.
 */
@EmiEntrypoint
public class ProspectEmiPlugin implements EmiPlugin {
    /** The category of each kind, made once: EMI compares categories by identity. */
    private static final Map<ViewerData.Kind, EmiRecipeCategory> CATEGORIES = new EnumMap<>(ViewerData.Kind.class);

    static {
        for (ViewerData.Kind kind : ViewerData.Kind.values()) {
            CATEGORIES.put(kind, new KindCategory(kind));
        }
    }

    @Override
    public void register(EmiRegistry registry) {
        for (ViewerData.Kind kind : ViewerData.Kind.values()) {
            EmiRecipeCategory category = CATEGORIES.get(kind);
            registry.addCategory(category);
            for (String station : ViewerData.pages(kind).get(0).catalysts()) {
                ItemStack stack = ViewerSupport.stack(station, 1);
                if (!stack.isEmpty()) {
                    registry.addWorkstation(category, EmiStack.of(stack));
                }
            }
            for (ViewerData.Page page : ViewerData.pages(kind)) {
                registry.addRecipe(new PageRecipe(category, page));
            }
        }
    }

    private static EmiIngredient ingredient(List<String> ids) {
        List<EmiStack> stacks = new ArrayList<>();
        for (ItemStack stack : ViewerSupport.alternatives(ids)) {
            stacks.add(EmiStack.of(stack));
        }
        return EmiIngredient.of(stacks);
    }

    private static final class KindCategory extends EmiRecipeCategory {
        private final ViewerData.Kind kind;

        KindCategory(ViewerData.Kind kind) {
            super(ProspectMod.id(kind.path()), EmiStack.of(ViewerSupport.stack(ViewerData.pages(kind).get(0).catalysts().get(0), 1)));
            this.kind = kind;
        }

        @Override
        public Component getName() {
            return ViewerSupport.title(kind);
        }
    }

    /** One page in EMI: the inputs, an arrow, the outputs with their chances in the tooltips, and the note below. */
    private static final class PageRecipe extends BasicEmiRecipe {
        private final ViewerData.Page page;
        private final ViewerSupport.Shape shape;

        PageRecipe(EmiRecipeCategory category, ViewerData.Page page) {
            super(category, ResourceLocation.fromNamespaceAndPath(ProspectMod.MODID, "/" + page.kind().path() + "/" + page.id()),
                    ViewerSupport.shape(page.kind()).width(), ViewerSupport.shape(page.kind()).height());
            this.page = page;
            this.shape = ViewerSupport.shape(page.kind());
            for (List<String> slot : page.inputs()) {
                inputs.add(ingredient(slot));
            }
            for (ViewerData.Out out : page.outputs()) {
                outputs.add(EmiStack.of(ViewerSupport.stack(out.item(), out.max())));
            }
            for (String id : page.catalysts()) {
                ItemStack stack = ViewerSupport.stack(id, 1);
                if (!stack.isEmpty()) {
                    catalysts.add(EmiStack.of(stack));
                }
            }
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            for (int i = 0; i < inputs.size(); i++) {
                widgets.addSlot(inputs.get(i), shape.inputX(), shape.inputY(i));
            }
            widgets.addTexture(EmiTexture.EMPTY_ARROW, shape.arrowX(), ViewerSupport.PAD + 1);
            for (int i = 0; i < outputs.size(); i++) {
                SlotWidget slot = widgets.addSlot(outputs.get(i), shape.outputX(i), shape.outputY(i)).recipeContext(this);
                for (Component line : ViewerSupport.tooltip(page.kind(), page.outputs().get(i))) {
                    slot.appendTooltip(line);
                }
            }
            widgets.addText(ViewerSupport.note(page), ViewerSupport.PAD, shape.height() - ViewerSupport.NOTE_HEIGHT, 0xFF606060, false);
        }
    }
}
