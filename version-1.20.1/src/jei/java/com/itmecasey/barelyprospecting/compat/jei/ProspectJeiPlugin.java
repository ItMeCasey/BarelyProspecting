package com.itmecasey.barelyprospecting.compat.jei;

import com.itmecasey.barelyprospecting.ProspectMod;
import com.itmecasey.barelyprospecting.compat.ViewerSupport;
import com.itmecasey.barelyprospecting.core.ViewerData;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * JEI support: one page type each for panning, hammering, soaking, melting and the lure, built from the same data as the BEI, REI and
 * EMI pages. JEI finds this class by its annotation and only when JEI is installed.
 */
@JeiPlugin
public class ProspectJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ProspectMod.id("jei");
    private static final Map<ViewerData.Kind, RecipeType<ViewerData.Page>> TYPES = new EnumMap<>(ViewerData.Kind.class);

    static {
        for (ViewerData.Kind kind : ViewerData.Kind.values()) {
            TYPES.put(kind, RecipeType.create(ProspectMod.MODID, kind.path(), ViewerData.Page.class));
        }
    }

    private static IJeiRuntime runtime;

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    /** For the dev tours: opens JEI's recipe screen on the pages of one kind. False before JEI has started. */
    public static boolean show(String kindPath) {
        if (runtime == null) {
            return false;
        }
        for (ViewerData.Kind kind : ViewerData.Kind.values()) {
            if (kind.path().equals(kindPath)) {
                runtime.getRecipesGui().showTypes(List.of(TYPES.get(kind)));
                return true;
            }
        }
        return false;
    }

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        for (ViewerData.Kind kind : ViewerData.Kind.values()) {
            registration.addRecipeCategories(new Category(kind, gui));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (ViewerData.Kind kind : ViewerData.Kind.values()) {
            registration.addRecipes(TYPES.get(kind), ViewerData.pages(kind));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (ViewerData.Kind kind : ViewerData.Kind.values()) {
            List<ItemLike> stations = new ArrayList<>();
            for (String id : ViewerData.pages(kind).get(0).catalysts()) {
                Item item = ProspectMod.item(id);
                if (item != null) {
                    stations.add(item);
                }
            }
            registration.addRecipeCatalysts(TYPES.get(kind), stations.toArray(new ItemLike[0]));
        }
    }

    private static final class Category implements IRecipeCategory<ViewerData.Page> {
        private final ViewerData.Kind kind;
        private final ViewerSupport.Shape shape;
        private final IDrawable icon;

        Category(ViewerData.Kind kind, IGuiHelper gui) {
            this.kind = kind;
            this.shape = ViewerSupport.shape(kind);
            this.icon = gui.createDrawableItemLike(ProspectMod.item(ViewerData.pages(kind).get(0).catalysts().get(0)));
        }

        @Override
        public RecipeType<ViewerData.Page> getRecipeType() {
            return TYPES.get(kind);
        }

        @Override
        public Component getTitle() {
            return ViewerSupport.title(kind);
        }

        @Override
        public int getWidth() {
            return shape.width();
        }

        @Override
        public int getHeight() {
            return shape.height();
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, ViewerData.Page page, IFocusGroup focuses) {
            for (int i = 0; i < page.inputs().size(); i++) {
                builder.addInputSlot(shape.inputX() + 1, shape.inputY(i) + 1).setStandardSlotBackground()
                        .addItemStacks(ViewerSupport.alternatives(page.inputs().get(i)));
            }
            for (int i = 0; i < page.outputs().size(); i++) {
                ViewerData.Out out = page.outputs().get(i);
                ItemStack stack = ViewerSupport.stack(out.item(), out.max());
                builder.addOutputSlot(shape.outputX(i) + 1, shape.outputY(i) + 1).setStandardSlotBackground().addItemStack(stack)
                        .addRichTooltipCallback((view, tooltip) -> ViewerSupport.tooltip(kind, out).forEach(tooltip::add));
            }
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, ViewerData.Page page, IFocusGroup focuses) {
            builder.addRecipeArrowWidget().setPosition(shape.arrowX(), ViewerSupport.PAD);
            builder.addText(ViewerSupport.note(page), shape.width() - 2 * ViewerSupport.PAD, ViewerSupport.NOTE_HEIGHT)
                    .setPosition(ViewerSupport.PAD, shape.height() - ViewerSupport.NOTE_HEIGHT - 2).setColor(0xFF808080);
        }
    }
}
