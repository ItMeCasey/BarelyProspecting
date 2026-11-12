package com.itmecasey.barelyprospecting;

import com.itmecasey.barelyprospecting.core.Dice;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;

/** Names and small lookups shared by every part of Barely Prospecting. */
public final class ProspectMod {
    /** Mod id; must match {@code mod_id} in gradle.properties and the mod metadata file. */
    public static final String MODID = "barelyprospecting";

    public static final Logger LOGGER = LogUtils.getLogger();

    /** True in dev runs (the Gradle run tasks set the property when -PdevTools is given). */
    public static final boolean DEV = System.getProperty("barelyprospecting.dev") != null;

    private ProspectMod() {
    }

    /** An identifier in this mod's namespace. */
    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }

    /** The registry id of an item as text, such as {@code minecraft:flint}. */
    public static String idOf(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    /** The registry id of a block as text. */
    public static String idOf(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    /** The item with this id, or null when it does not exist (for example an item of a mod that is not installed). */
    public static Item item(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key != null && BuiltInRegistries.ITEM.containsKey(key) ? BuiltInRegistries.ITEM.get(key) : null;
    }

    /** The block with this id, or null. */
    public static Block block(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key != null && BuiltInRegistries.BLOCK.containsKey(key) ? BuiltInRegistries.BLOCK.get(key) : null;
    }

    /** A stack of the named item, or an empty stack when the item does not exist. */
    public static ItemStack stack(String id, int count) {
        Item item = item(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    /** Wraps Minecraft's random source for the game logic in {@code core}. */
    public static Dice dice(RandomSource random) {
        return new Dice() {
            @Override
            public int nextInt(int bound) {
                return random.nextInt(bound);
            }

            @Override
            public double nextDouble() {
                return random.nextDouble();
            }
        };
    }
}
