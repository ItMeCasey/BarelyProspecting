package com.itmecasey.barelyprospecting.compat.bei;

import com.itmecasey.barelyprospecting.ProspectMod;
import com.itmecasey.barelyprospecting.compat.ViewerSupport;
import com.itmecasey.barelyprospecting.core.ViewerData;
import com.itmecasey.bei.api.BeiPlugin;
import com.itmecasey.bei.api.CardPainter;
import com.itmecasey.bei.api.CustomRecipe;
import com.itmecasey.bei.api.IBeiPlugin;
import com.itmecasey.bei.api.ICategoryRegistration;
import com.itmecasey.bei.api.IRecipeRegistration;
import com.itmecasey.bei.api.RecipeCategory;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Barely Enough Items support: one card category each for panning, hammering, soaking, melting and the lure, built from
 * {@link ViewerData}, the same rules the game runs. BEI finds this class by its annotation, so it is only loaded when BEI is installed.
 */
@BeiPlugin
public class ProspectBeiPlugin implements IBeiPlugin {
    private final Map<ViewerData.Kind, RecipeCategory> categories = new EnumMap<>(ViewerData.Kind.class);

    @Override
    public void registerCategories(ICategoryRegistration registration) {
        for (ViewerData.Kind kind : ViewerData.Kind.values()) {
            ViewerSupport.Shape shape = ViewerSupport.shape(kind);
            ItemStack station = ViewerSupport.stack(ViewerData.pages(kind).get(0).catalysts().get(0), 1);
            RecipeCategory.Builder builder = RecipeCategory.builder(ProspectMod.id(kind.path()))
                    .title(ViewerSupport.title(kind))
                    .station(station)
                    .size(shape.width(), shape.height());
            for (int i = 0; i < shape.inputs(); i++) {
                builder.input(shape.inputX(), shape.inputY(i));
            }
            for (int i = 0; i < shape.outputs(); i++) {
                builder.output(shape.outputX(i), shape.outputY(i));
            }
            RecipeCategory category = builder.painter(new Painter(kind, shape)).build();
            categories.put(kind, category);
            registration.register(category);
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (ViewerData.Page page : ViewerData.all()) {
            CustomRecipe.Builder builder = CustomRecipe.builder().id(ProspectMod.MODID + ":" + page.kind().path() + "/" + page.id());
            for (List<String> slot : page.inputs()) {
                builder.input(ViewerSupport.alternatives(slot));
            }
            for (ViewerData.Out out : page.outputs()) {
                builder.output(ViewerSupport.stack(out.item(), out.max()));
            }
            registration.add(categories.get(page.kind()), builder.data(page).build());
        }
    }

    private record Painter(ViewerData.Kind kind, ViewerSupport.Shape shape) implements CardPainter {
        @Override
        public void paint(GuiGraphicsExtractor g, CustomRecipe recipe, int mouseX, int mouseY) {
            int y = ViewerSupport.PAD + 8;
            int x = shape.arrowX();
            g.fill(x, y, x + ViewerSupport.ARROW - 6, y + 2, 0xFF8B8B8B);
            g.fill(x + ViewerSupport.ARROW - 6, y - 3, x + ViewerSupport.ARROW - 4, y + 5, 0xFF8B8B8B);
            if (recipe.data() instanceof ViewerData.Page page) {
                g.text(Minecraft.getInstance().font, ViewerSupport.note(page), ViewerSupport.PAD, shape.height() - ViewerSupport.NOTE_HEIGHT,
                        0xFFA0A0A0, false);
            }
        }

        @Override
        public List<Component> tooltip(CustomRecipe recipe, int mouseX, int mouseY) {
            if (recipe.data() instanceof ViewerData.Page page) {
                int i = ViewerSupport.outputAt(shape, page.outputs().size(), mouseX, mouseY);
                if (i >= 0) {
                    return ViewerSupport.tooltip(kind, page.outputs().get(i));
                }
            }
            return List.of();
        }
    }
}
