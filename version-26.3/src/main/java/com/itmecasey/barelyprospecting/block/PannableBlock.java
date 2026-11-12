package com.itmecasey.barelyprospecting.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Silt, grit, fine grit, Nether silt and End silt: a loose block you pan with a tray. Every finished pan wears it one stage
 * (the texture gets more pitted) until the last stage is used up and the block is gone.
 */
public class PannableBlock extends Block {
    /** How worn the block is: 0 is fresh, 3 is on its last pan. */
    public static final IntegerProperty WEAR = IntegerProperty.create("wear", 0, 3);

    /** The material name used in the drop tables ({@code silt}, {@code grit}, ...). */
    private final String material;

    public PannableBlock(String material, Properties properties) {
        super(properties);
        this.material = material;
        registerDefaultState(stateDefinition.any().setValue(WEAR, 0));
    }

    public String material() {
        return material;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WEAR);
    }
}
