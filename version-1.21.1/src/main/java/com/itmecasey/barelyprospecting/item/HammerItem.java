package com.itmecasey.barelyprospecting.item;

import com.itmecasey.barelyprospecting.core.script.ScriptEvents;
import com.itmecasey.barelyprospecting.ProspectMod;
import com.itmecasey.barelyprospecting.core.Recipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The pick-hammer. Right-click a block to strike it: dirt breaks into silt, stone into rubble, rubble into grit, grit into fine
 * grit and fine grit into sand. One blow, one step, and the block changes under your hand.
 */
public class HammerItem extends Item {
    public HammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        String to = Recipes.crush(ProspectMod.idOf(state.getBlock()));
        Block target = to == null ? null : ProspectMod.block(to);
        if (target == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        level.setBlock(pos, target.defaultBlockState(), Block.UPDATE_ALL);
        ScriptEvents.fire("hammered", context.getPlayer() == null ? "" : context.getPlayer().getName().getString(), com.itmecasey.barelyprospecting.server.Compat.dimensionId(level),
                pos.getX(), pos.getY(), pos.getZ(), ProspectMod.idOf(state.getBlock()), to, 1, "", null);
        level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.7F, 0.8F + level.getRandom().nextFloat() * 0.4F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                    10, 0.3, 0.2, 0.3, 0.05);
        }
        Player player = context.getPlayer();
        if (player != null && !player.getAbilities().instabuild) {
            context.getItemInHand().hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(context.getHand()));
        }
        return InteractionResult.SUCCESS;
    }
}
