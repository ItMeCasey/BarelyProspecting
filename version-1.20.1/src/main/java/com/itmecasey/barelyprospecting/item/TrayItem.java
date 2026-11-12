package com.itmecasey.barelyprospecting.item;

import com.itmecasey.barelyprospecting.core.Tier;
import com.itmecasey.barelyprospecting.server.Pan;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * A panning tray. Hold the use button while looking at silt, grit or another pannable block: a ring of sparks fills up around
 * the block and, when it closes, the tray turns up whatever its tier can find. Letting go or looking away starts the ring over.
 */
public class TrayItem extends Item {
    private final Tier tier;

    public TrayItem(Tier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public Tier tier() {
        return tier;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BRUSH;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingTicks) {
        if (level instanceof ServerLevel serverLevel && entity instanceof ServerPlayer player) {
            Pan.tick(serverLevel, player, stack, tier, entity.getUsedItemHand());
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTicks) {
        if (entity instanceof ServerPlayer player) {
            Pan.cancel(player);
        }
    }
}
