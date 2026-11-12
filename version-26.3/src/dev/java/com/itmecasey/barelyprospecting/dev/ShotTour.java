package com.itmecasey.barelyprospecting.dev;

import com.itmecasey.barelyprospecting.ModContent;
import com.itmecasey.barelyprospecting.ProspectMod;
import com.itmecasey.barelyprospecting.block.BasinBlockEntity;
import com.itmecasey.barelyprospecting.block.CrucibleBlockEntity;
import com.itmecasey.barelyprospecting.block.LureBlockEntity;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A development-only camera tour on a real client. It creates a fresh world with the mod's own Void Island preset, then walks
 * through every hands-on step with the game's real use paths (the game mode's use methods, no OS input): hammering dirt, holding a
 * tray until its ring closes, soaking silt in a basin, melting rubble in a crucible, feeding a lure. Screenshots land in
 * {@code run/screenshots/barelyprospecting_<name>.png}. It does nothing unless the environment variable
 * {@code BARELYPROSPECTING_SHOTS} is set; progress goes to the log as {@code PROSPECT-SHOTS} lines.
 */
public final class ShotTour {
    private static final Logger LOGGER = LoggerFactory.getLogger("BarelyProspecting");
    private static final boolean ENABLED = System.getenv("BARELYPROSPECTING_SHOTS") != null;
    private static final String WORLD = "barelyprospecting_shots";
    /** The stage: a stone platform floating in the void, away from the island. */
    private static final int SX = 40;
    private static final int SY = 70;
    private static final int SZ = 40;

    private enum Stage { WAITING_FOR_MENU, LOADING_WORLD, TOURING, DONE }

    private record Step(int delay, Runnable action, BooleanSupplier ready, int timeout) {
    }

    private static final Deque<Step> STEPS = new ArrayDeque<>();
    private static Stage stage = Stage.WAITING_FOR_MENU;
    private static int idle;
    private static int wait;
    private static int held;
    private static boolean pauseOnLostFocus;
    /** While true the game's own "use" key state is held down, as a held right mouse button would be (no OS input is sent). */
    private static boolean holdingUse;
    /** Block entity facts, copied on the server thread every tick (the game refuses block entity reads from the render thread). */
    private static volatile boolean snapBasinResult;
    private static volatile int snapCrucibleLava;
    private static volatile boolean snapLureIdle;
    private static volatile boolean snapLureSawHatch;
    private static volatile String snapLureText = "no block entity";

    private ShotTour() {
    }

    public static void tick() {
        if (!ENABLED || stage == Stage.DONE) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        try {
            if (minecraft.mouseHandler.isMouseGrabbed()) {
                minecraft.mouseHandler.releaseMouse();
            }
            minecraft.options.keyUse.setDown(holdingUse);
            MinecraftServer server = minecraft.getSingleplayerServer();
            if (server != null) {
                server.execute(() -> {
                    BasinBlockEntity b = basin();
                    snapBasinResult = b != null && b.basin().hasResult();
                    CrucibleBlockEntity c = crucible();
                    snapCrucibleLava = c == null ? 0 : c.crucible().lava();
                    LureBlockEntity l = lure();
                    snapLureText = l == null ? "no block entity" : "meals=" + l.lure().meals() + " table=" + l.lure().table() + " left=" + l.lure().ticksLeft();
                    snapLureSawHatch |= l != null && l.lure().isHatching();
                    snapLureIdle = snapLureSawHatch && l != null && !l.lure().isHatching() && l.lure().meals() == 0;
                });
            }
            switch (stage) {
                case WAITING_FOR_MENU -> waitForMenu(minecraft);
                case LOADING_WORLD -> waitForWorld(minecraft);
                case TOURING -> runSteps(minecraft);
                default -> {
                }
            }
        } catch (RuntimeException | Error e) {
            LOGGER.error("PROSPECT-SHOTS the tour failed", e);
            stage = Stage.DONE;
            minecraft.stop();
        }
    }

    private static void waitForMenu(Minecraft minecraft) {
        if (minecraft.gui.overlay() != null || minecraft.level != null || minecraft.gui.screen() == null || ++idle < 20) {
            return;
        }
        pauseOnLostFocus = minecraft.options.pauseOnLostFocus;
        minecraft.options.pauseOnLostFocus = false;
        LevelStorageSource source = minecraft.getLevelSource();
        if (source.levelExists(WORLD)) {
            try (LevelStorageSource.LevelStorageAccess access = source.createAccess(WORLD)) {
                access.deleteLevel();
            } catch (IOException e) {
                LOGGER.warn("PROSPECT-SHOTS could not delete the old world", e);
            }
        }
        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE,
                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
        stage = Stage.LOADING_WORLD;
        idle = 0;
        LOGGER.info("PROSPECT-SHOTS creating a world with the Void Island preset");
        ResourceKey<net.minecraft.world.level.levelgen.presets.WorldPreset> preset = ResourceKey.create(Registries.WORLD_PRESET, ProspectMod.id("void_island"));
        minecraft.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(0L, false, false),
                registries -> registries.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(preset).value().createWorldDimensions(), new TitleScreen());
    }

    private static void waitForWorld(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null || minecraft.gui.screen() != null
                || minecraft.getSingleplayerServer() == null || ++idle < 60) {
            return;
        }
        LOGGER.info("PROSPECT-SHOTS in the world at {}", minecraft.player.blockPosition());
        plan();
        stage = Stage.TOURING;
    }

    private static void runSteps(Minecraft minecraft) {
        if (wait > 0) {
            wait--;
            return;
        }
        Step step = STEPS.peek();
        if (step == null) {
            return;
        }
        if (step.ready() != null && !step.ready().getAsBoolean()) {
            if (++held > step.timeout()) {
                throw new IllegalStateException("a step timed out waiting (lure " + snapLureText + ")");
            }
            return;
        }
        held = 0;
        STEPS.poll();
        step.action().run();
        Step next = STEPS.peek();
        wait = next == null ? 0 : next.delay();
    }

    // ---- the tour ----

    private static BlockPos at(int dx, int dy, int dz) {
        return new BlockPos(SX + dx, SY + 1 + dy, SZ + dz);
    }

    private static String xyz(int dx, int dy, int dz) {
        BlockPos p = at(dx, dy, dz);
        return p.getX() + " " + p.getY() + " " + p.getZ();
    }

    /** Stands at the stage and looks at a block of the stage from a given offset. */
    private static void view(double ex, double ey, double ez, int tx, int ty, int tz) {
        BlockPos t = at(tx, ty, tz);
        look(new Vec3(SX + 0.5 + ex, SY + 1 + ey, SZ + 0.5 + ez), new Vec3(t.getX() + 0.5, t.getY() + 0.5, t.getZ() + 0.5));
    }

    private static void plan() {
        if ("compat".equals(System.getenv("BARELYPROSPECTING_SHOTS"))) {
            planCompat();
            return;
        }
        if ("viewers".equals(System.getenv("BARELYPROSPECTING_SHOTS"))) {
            planViewers();
            return;
        }
        then(0, () -> {
            server("time set noon", "weather clear", "kill @e[type=!player]", "clear @p", "gamerule spawn_radius 0");
            Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
        });
        // 01: the island, built by the preset on first start.
        then(10, () -> look(new Vec3(0.5, 66.0, 7.0), new Vec3(0.5, 64.5, 0.5)));
        shoot("01_void_island", 30);
        then(5, () -> look(new Vec3(-5.0, 66.5, 3.5), new Vec3(-2.0, 64.5, 0.5)));
        shoot("02_starter_chest", 20);
        // The stage.
        then(5, () -> server("fill " + (SX - 8) + " " + SY + " " + (SZ - 8) + " " + (SX + 8) + " " + SY + " " + (SZ + 8) + " minecraft:smooth_stone",
                "fill " + (SX - 8) + " " + (SY + 1) + " " + (SZ - 8) + " " + (SX + 8) + " " + (SY + 6) + " " + (SZ + 8) + " minecraft:air",
                "tp @p " + (SX + 0.5) + " " + (SY + 1) + " " + (SZ + 0.5) + " 0 0"));
        // Items gallery: every material, tool and tray in the inventory.
        then(15, () -> server("give @p barelyprospecting:twig 12", "give @p barelyprospecting:pebble 16", "give @p barelyprospecting:coal_dust 8",
                "give @p barelyprospecting:pick_hammer", "give @p barelyprospecting:wooden_panning_tray", "give @p barelyprospecting:copper_panning_tray",
                "give @p barelyprospecting:iron_panning_tray", "give @p barelyprospecting:gold_panning_tray", "give @p barelyprospecting:diamond_panning_tray",
                "give @p barelyprospecting:copper_flake 8", "give @p barelyprospecting:iron_flake 8", "give @p barelyprospecting:gold_flake 8",
                "give @p barelyprospecting:diamond_shard 8", "give @p barelyprospecting:unfired_clay_bucket", "give @p barelyprospecting:clay_bucket",
                "give @p barelyprospecting:clay_bucket_water", "give @p barelyprospecting:clay_bucket_lava", "give @p minecraft:wheat_seeds 16"));
        then(10, () -> Minecraft.getInstance().gui.setScreen(new InventoryScreen(Minecraft.getInstance().player)));
        shoot("03_inventory_items", 10);
        then(2, () -> Minecraft.getInstance().gui.setScreen(null));

        // Block gallery: five materials in four wear stages, rubble, and the three machines.
        then(5, () -> {
            String[] names = {"silt", "grit", "fine_grit", "nether_silt", "end_silt"};
            for (int row = 0; row < names.length; row++) {
                for (int wear = 0; wear < 4; wear++) {
                    server("setblock " + xyz(-3 + wear, 0, 3 + row) + " barelyprospecting:" + names[row] + "[wear=" + wear + "]");
                }
            }
            server("setblock " + xyz(2, 0, 3) + " barelyprospecting:rubble", "setblock " + xyz(2, 0, 4) + " barelyprospecting:unfired_crucible",
                    "setblock " + xyz(2, 0, 5) + " barelyprospecting:basin[water=2]", "setblock " + xyz(2, 0, 6) + " barelyprospecting:crucible[lit=true,lava=2]",
                    "setblock " + xyz(2, 0, 7) + " barelyprospecting:lure[meals=2]");
        });
        then(5, () -> view(0.0, 7.0, -1.5, -1, 0, 5));
        shoot("04_block_gallery", 30);
        then(5, () -> view(5.0, 3.0, 0.5, 2, 0, 5));
        shoot("05_machines_close", 20);
        then(5, () -> server("fill " + xyz(-3, 0, 3) + " " + xyz(2, 0, 7) + " minecraft:air"));

        // 06 and 07: the hammer turns dirt into silt, with the game's own use path.
        then(10, () -> server("setblock " + xyz(0, 0, 3) + " minecraft:dirt", "setblock " + xyz(1, 0, 3) + " minecraft:cobblestone"));
        then(5, () -> view(0.0, 1.6, 0.0, 0, 0, 3));
        then(5, () -> hold(ModContent.PICK_HAMMER));
        shoot("06_hammer_on_dirt", 15);
        then(14, () -> useOn(at(0, 0, 3)));
        shoot("07_dirt_became_silt", 10);
        then(5, () -> view(0.0, 1.6, 0.0, 1, 0, 3));
        then(14, () -> useOn(at(1, 0, 3)));
        then(14, () -> useOn(at(1, 0, 3)));
        shoot("08_cobble_to_grit", 10);

        // 09 to 11: hold the tray on silt; the ring of sparks fills up, then the drops and the worn block.
        then(5, () -> server("setblock " + xyz(0, 0, 3) + " barelyprospecting:silt", "setblock " + xyz(1, 0, 3) + " minecraft:air"));
        then(5, () -> view(0.0, 1.6, 0.0, 0, 0, 3));
        then(5, () -> hold(ModContent.WOODEN_TRAY));
        then(5, () -> {
            Minecraft m = Minecraft.getInstance();
            holdingUse = true;
            m.gameMode.useItem(m.player, InteractionHand.MAIN_HAND);
        });
        shoot("09_ring_early", 8);
        shoot("10_ring_filling", 14);
        until(() -> Minecraft.getInstance().level.getBlockState(at(0, 0, 3)).hasProperty(com.itmecasey.barelyprospecting.block.PannableBlock.WEAR)
                && Minecraft.getInstance().level.getBlockState(at(0, 0, 3)).getValue(com.itmecasey.barelyprospecting.block.PannableBlock.WEAR) >= 1, 200);
        then(3, () -> {
            Minecraft m = Minecraft.getInstance();
            holdingUse = false;
            m.gameMode.releaseUsingItem(m.player);
        });
        shoot("11_after_one_pan", 6);

        // 12 to 14: the basin, filled with water, takes silt and gives clay.
        then(5, () -> server("setblock " + xyz(0, 0, 3) + " minecraft:air", "setblock " + xyz(0, 0, 4) + " barelyprospecting:basin",
                "tp @p " + (SX + 0.5) + " " + (SY + 1) + " " + (SZ + 0.5) + " 0 0"));
        then(15, () -> view(0.0, 1.6, 0.0, 0, 0, 4));
        then(5, () -> hold(ModContent.CLAY_BUCKET_WATER));
        then(14, () -> useOn(at(0, 0, 4)));
        shoot("12_basin_filled", 8);
        then(5, () -> server("give @p barelyprospecting:silt 2"));
        then(10, () -> hold(ModContent.SILT.asItem()));
        then(14, () -> useOn(at(0, 0, 4)));
        shoot("13_basin_soaking", 40);
        until(() -> snapBasinResult, 1000);
        shoot("14_basin_result_ready", 10);
        then(5, () -> {
            Minecraft m = Minecraft.getInstance();
            m.player.getInventory().setSelectedSlot(8);
            server("item replace entity @p hotbar.8 with minecraft:air");
        });
        then(14, () -> useOn(at(0, 0, 4)));
        shoot("15_clay_in_inventory", 20);

        // 16 to 19: the crucible takes rubble and fuel, lights, melts, gives a bucket of lava and quenches to obsidian.
        then(5, () -> server("setblock " + xyz(0, 0, 4) + " minecraft:air", "setblock " + xyz(0, 0, 5) + " barelyprospecting:crucible",
                "give @p barelyprospecting:rubble 8", "give @p minecraft:coal 4"));
        then(15, () -> view(0.0, 1.6, 0.0, 0, 0, 5));
        then(10, () -> hold(ModContent.RUBBLE.asItem()));
        then(8, () -> useOn(at(0, 0, 5)));
        then(8, () -> useOn(at(0, 0, 5)));
        then(8, () -> useOn(at(0, 0, 5)));
        then(8, () -> useOn(at(0, 0, 5)));
        then(5, () -> hold(net.minecraft.world.item.Items.COAL));
        then(8, () -> useOn(at(0, 0, 5)));
        shoot("16_crucible_lit", 25);
        until(() -> snapCrucibleLava >= 1000, 1500);
        shoot("17_crucible_lava", 10);
        then(5, () -> hold(ModContent.CLAY_BUCKET));
        then(14, () -> useOn(at(0, 0, 5)));
        shoot("18_bucket_of_lava", 15);
        then(14, () -> useOn(at(0, 0, 5)));
        then(5, () -> hold(ModContent.CLAY_BUCKET_WATER));
        then(14, () -> useOn(at(0, 0, 5)));
        then(10, () -> Minecraft.getInstance().gui.setScreen(new InventoryScreen(Minecraft.getInstance().player)));
        shoot("19_obsidian_in_inventory", 10);
        then(2, () -> Minecraft.getInstance().gui.setScreen(null));

        // 20 to 22: the lure is fed three times by hand, then something answers.
        then(5, () -> server("setblock " + xyz(0, 0, 5) + " minecraft:air", "setblock " + xyz(0, 0, 6) + " barelyprospecting:lure",
                "tp @p " + (SX + 0.5) + " " + (SY + 1) + " " + (SZ + 0.5) + " 0 0"));
        then(15, () -> view(0.0, 1.6, 0.0, 0, 0, 6));
        then(5, () -> hold(net.minecraft.world.item.Items.WHEAT_SEEDS));
        then(14, () -> useOn(at(0, 0, 6)));
        shoot("20_lure_one_meal", 10);
        then(14, () -> useOn(at(0, 0, 6)));
        then(14, () -> useOn(at(0, 0, 6)));
        shoot("21_lure_hatching", 40);
        until(() -> snapLureIdle, 1000);
        shoot("22_lure_drops", 10);

        then(20, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.options.pauseOnLostFocus = pauseOnLostFocus;
            LOGGER.info("PROSPECT-SHOTS DONE");
            stage = Stage.DONE;
            minecraft.stop();
        });
    }

    // ---- the compatibility tour: Show Me What This Is on the blocks, Barely Enough Items on the recipes ----

    private static void planCompat() {
        then(0, () -> {
            server("time set noon", "weather clear", "kill @e[type=!player]", "clear @p");
            Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
        });
        then(10, () -> server("fill " + (SX - 8) + " " + SY + " " + (SZ - 8) + " " + (SX + 8) + " " + SY + " " + (SZ + 8) + " minecraft:smooth_stone",
                "fill " + (SX - 8) + " " + (SY + 1) + " " + (SZ - 8) + " " + (SX + 8) + " " + (SY + 6) + " " + (SZ + 8) + " minecraft:air",
                "tp @p " + (SX + 0.5) + " " + (SY + 1) + " " + (SZ + 0.5) + " 0 0",
                "setblock " + xyz(0, 0, 4) + " barelyprospecting:basin[water=3]", "setblock " + xyz(2, 0, 4) + " barelyprospecting:crucible[lit=true,lava=2]",
                "setblock " + xyz(-2, 0, 4) + " barelyprospecting:lure[meals=2]", "setblock " + xyz(4, 0, 4) + " barelyprospecting:silt[wear=2]",
                "setblock " + xyz(-4, 0, 4) + " barelyprospecting:nether_silt"));
        // The server needs the blocks fed so the lines have something to say.
        then(20, () -> onServerThread(server -> {
            var level = server.overworld();
            if (level.getBlockEntity(at(0, 0, 4)) instanceof BasinBlockEntity b) {
                b.basin().fill(com.itmecasey.barelyprospecting.core.Recipes.Fluid.WATER, 4);
                b.basin().start("barelyprospecting:silt", "minecraft:overworld");
                b.changed();
            }
            if (level.getBlockEntity(at(2, 0, 4)) instanceof CrucibleBlockEntity c) {
                c.crucible().load(1500, 250, 600);
                c.changed();
            }
            if (level.getBlockEntity(at(-2, 0, 4)) instanceof LureBlockEntity l) {
                l.lure().load("farm", 3, 200);
                l.changed();
            }
        }));
        String[] names = {"basin", "crucible", "lure", "silt", "nether_silt"};
        int[] xs = {0, 2, -2, 4, -4};
        for (int i = 0; i < names.length; i++) {
            final int x = xs[i];
            then(5, () -> view(x * 0.7, 1.6, 1.5, x, 0, 4));
            shoot("smwti_" + names[i], 45);
        }
        // Barely Enough Items: the inventory with the browser, then the recipe popups.
        then(5, () -> server("tp @p " + (SX + 0.5) + " " + (SY + 1.5) + " " + (SZ + 0.5) + " 0 0", "gamemode survival @a"));
        then(40, () -> Minecraft.getInstance().gui.setScreen(new InventoryScreen(Minecraft.getInstance().player)));
        then(40, () -> beiSearch("barelyprospecting"));
        shoot("bei_browser", 30);
        String[][] queries = {{"silt", "false", "bei_silt_recipes"}, {"wooden_panning_tray", "true", "bei_tray_uses"}, {"pick_hammer", "true", "bei_hammer_uses"},
                {"clay_ball", "true", "bei_clay_uses"}, {"obsidian", "false", "bei_obsidian"}, {"clay_bucket_lava", "false", "bei_lava_bucket"},
                {"ender_pearl", "false", "bei_pearl"}, {"crucible", "true", "bei_crucible_uses"}, {"lure", "true", "bei_lure_uses"}};
        for (String[] q : queries) {
            then(25, () -> beiOpen(q[0], Boolean.parseBoolean(q[1])));
            then(20, () -> LOGGER.info("PROSPECT-SHOTS BEI {} popup open={}", q[0], beiPopupOpen()));
            shoot(q[2], 10);
        }
        for (String category : new String[] {"panning", "hammering", "soaking", "melting", "lure"}) {
            then(25, () -> beiOpenType("barelyprospecting:" + category));
            then(20, () -> LOGGER.info("PROSPECT-SHOTS BEI category {} popup open={}", category, beiPopupOpen()));
            shoot("bei_category_" + category, 10);
        }
        for (String category : new String[] {"panning", "hammering", "soaking", "melting", "lure"}) {
            then(30, () -> jeiShow(category));
            shoot("jei_category_" + category, 20);
        }
        then(20, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.options.pauseOnLostFocus = pauseOnLostFocus;
            LOGGER.info("PROSPECT-SHOTS DONE");
            stage = Stage.DONE;
            minecraft.stop();
        });
    }

    private static Object beiField(String name) throws ReflectiveOperationException {
        java.lang.reflect.Field field = Class.forName("com.itmecasey.bei.client.BrowserPanel").getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

    private static void beiSearch(String text) {
        try {
            ((net.minecraft.client.gui.components.EditBox) beiField("box")).setValue(text);
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("PROSPECT-SHOTS could not reach the BEI search box", e);
        }
    }

    private static void jeiShow(String category) {
        try {
            Object shown = Class.forName("com.itmecasey.barelyprospecting.compat.jei.ProspectJeiPlugin").getMethod("show", String.class).invoke(null, category);
            LOGGER.info("PROSPECT-SHOTS JEI {} shown={}", category, shown);
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("PROSPECT-SHOTS JEI is not installed here", e);
        }
    }

    private static void beiClosePopup() {
        try {
            Class.forName("com.itmecasey.bei.client.RecipePopup").getMethod("close").invoke(null);
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("PROSPECT-SHOTS could not close the popup", e);
        }
    }

    private static void beiOpen(String path, boolean uses) {
        try {
            net.minecraft.world.item.Item item = ProspectMod.item(path.contains(":") ? path : (path.equals("obsidian") || path.equals("ender_pearl") || path.equals("clay_ball") ? "minecraft:" : "barelyprospecting:") + path);
            Class.forName("com.itmecasey.bei.client.RecipePopup").getMethod("open", net.minecraft.world.item.Item.class, boolean.class).invoke(null, item, uses);
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("PROSPECT-SHOTS could not open the BEI popup", e);
        }
    }

    private static void beiOpenType(String id) {
        try {
            Class.forName("com.itmecasey.bei.client.RecipePopup").getMethod("openType", String.class).invoke(null, id);
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("PROSPECT-SHOTS could not open the BEI category", e);
        }
    }

    private static boolean beiPopupOpen() {
        try {
            return (Boolean) Class.forName("com.itmecasey.bei.client.RecipePopup").getMethod("isOpen").invoke(null);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    /** Moves the game cursor over the first cell of the BEI grid (twice: the game ignores a first move). */
    private static void beiMoveToFirstCell() {
        try {
            Minecraft game = Minecraft.getInstance();
            int gx = (Integer) beiField("gridX") + 9;
            int gy = (Integer) beiField("gridY") + 9;
            double sx = gx * (double) game.getWindow().getScreenWidth() / game.getWindow().getGuiScaledWidth();
            double sy = gy * (double) game.getWindow().getScreenHeight() / game.getWindow().getGuiScaledHeight();
            java.lang.reflect.Method move = net.minecraft.client.MouseHandler.class.getDeclaredMethod("onMove", long.class, double.class, double.class);
            move.setAccessible(true);
            move.invoke(game.mouseHandler, game.getWindow().handle(), sx + 3, sy);
            move.invoke(game.mouseHandler, game.getWindow().handle(), sx, sy);
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("PROSPECT-SHOTS could not move the game cursor", e);
        }
    }

    /** A click through the game mouse handler (button 0 recipes, 1 uses), wherever the game cursor is. */
    private static void beiClick(int button) {
        try {
            Minecraft game = Minecraft.getInstance();
            java.lang.reflect.Method press = net.minecraft.client.MouseHandler.class.getDeclaredMethod("onButton", long.class, net.minecraft.client.input.MouseButtonInfo.class, int.class);
            press.setAccessible(true);
            press.invoke(game.mouseHandler, game.getWindow().handle(), new net.minecraft.client.input.MouseButtonInfo(button, 0), 1);
            press.invoke(game.mouseHandler, game.getWindow().handle(), new net.minecraft.client.input.MouseButtonInfo(button, 0), 0);
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("PROSPECT-SHOTS could not click", e);
        }
    }

    private static void onServerThread(java.util.function.Consumer<MinecraftServer> action) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            server.execute(() -> action.accept(server));
        }
    }

    // ---- helpers ----

    // ---- the viewer tour: opens each recipe viewer that is installed on the five categories ----

    private static void planViewers() {
        then(0, () -> server("time set noon", "weather clear", "kill @e[type=!player]", "clear @p"));
        then(40, () -> server("gamemode survival @a"));
        then(30, () -> Minecraft.getInstance().gui.setScreen(new InventoryScreen(Minecraft.getInstance().player)));
        shoot("viewer_inventory", 40);
        for (String category : new String[] {"panning", "hammering", "soaking", "melting", "lure"}) {
            then(30, () -> viewerShow(category));
            shoot("viewer_" + category, 30);
        }
        then(20, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.options.pauseOnLostFocus = pauseOnLostFocus;
            LOGGER.info("PROSPECT-SHOTS DONE");
            stage = Stage.DONE;
            minecraft.stop();
        });
    }

    /** Asks every viewer plugin of the mod to open its recipe screen on the category; the ones whose viewer is missing are skipped. */
    private static void viewerShow(String category) {
        for (String plugin : new String[] {"jei.ProspectJeiPlugin", "rei.ProspectReiPlugin", "emi.ProspectEmiPlugin"}) {
            try {
                Object shown = Class.forName("com.itmecasey.barelyprospecting.compat." + plugin).getMethod("show", String.class).invoke(null, category);
                LOGGER.info("PROSPECT-SHOTS viewer {} {} shown={}", plugin, category, shown);
            } catch (ReflectiveOperationException | LinkageError e) {
                LOGGER.info("PROSPECT-SHOTS viewer {} is not available here ({})", plugin, e.getClass().getSimpleName());
            }
        }
    }

    private static BasinBlockEntity basin() {
        return Minecraft.getInstance().getSingleplayerServer().overworld().getBlockEntity(at(0, 0, 4)) instanceof BasinBlockEntity b ? b : null;
    }

    private static CrucibleBlockEntity crucible() {
        return Minecraft.getInstance().getSingleplayerServer().overworld().getBlockEntity(at(0, 0, 5)) instanceof CrucibleBlockEntity b ? b : null;
    }

    private static LureBlockEntity lure() {
        return Minecraft.getInstance().getSingleplayerServer().overworld().getBlockEntity(at(0, 0, 6)) instanceof LureBlockEntity b ? b : null;
    }

    /** Selects the hotbar slot that holds the item. */
    private static void hold(Item item) {
        LocalPlayer player = Minecraft.getInstance().player;
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getItem(slot).is(item)) {
                player.getInventory().setSelectedSlot(slot);
                return;
            }
        }
        // Not on the hotbar: put it into the selected slot through the server.
        String id = ProspectMod.idOf(item);
        server("item replace entity @p hotbar." + player.getInventory().getSelectedSlot() + " with " + id + " " + (new net.minecraft.world.item.ItemStack(item).getMaxStackSize() > 1 ? 8 : 1));
    }

    private static void useOn(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND, hit);
    }

    private static void then(int delay, Runnable action) {
        STEPS.add(new Step(delay, action, null, 0));
    }

    private static void until(BooleanSupplier ready, int timeoutTicks) {
        STEPS.add(new Step(1, () -> {
        }, ready, timeoutTicks));
    }

    private static void look(Vec3 eye, Vec3 target) {
        Vec3 d = target.subtract(eye);
        float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
        server(String.format(Locale.ROOT, "tp @a %.3f %.3f %.3f %.2f %.2f", eye.x, eye.y - 1.62, eye.z, yaw, pitch));
        LocalPlayer player = Minecraft.getInstance().player;
        player.snapTo(eye.x, eye.y - 1.62, eye.z, yaw, pitch);
        player.setYHeadRot(yaw);
        player.setDeltaMovement(Vec3.ZERO);
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
    }

    private static void shoot(String name, int delay) {
        then(delay, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.gui.hud.getChat().clearMessages(false);
            minecraft.gui.toastManager().clear();
        });
        then(2, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            LOGGER.info("PROSPECT-SHOTS {}", name);
            Screenshot.grab(minecraft.gameDirectory, "barelyprospecting_" + name + ".png", minecraft.gameRenderer.mainRenderTarget(), 1,
                    message -> LOGGER.info("PROSPECT-SHOTS saved {}", name));
        });
    }

    private static void server(String... commands) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            server.execute(() -> {
                for (String command : commands) {
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
                }
            });
        }
    }
}
