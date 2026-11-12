package com.itmecasey.barelyprospecting.compat.smwti;

import com.itmecasey.barelyprospecting.ProspectMod;
import com.itmecasey.barelyprospecting.block.BasinBlockEntity;
import com.itmecasey.barelyprospecting.block.CrucibleBlockEntity;
import com.itmecasey.barelyprospecting.block.LureBlockEntity;
import com.itmecasey.barelyprospecting.compat.LookInfo;
import com.itmecasey.smwti.api.ClientRegistration;
import com.itmecasey.smwti.api.CommonRegistration;
import com.itmecasey.smwti.api.ISmwtiPlugin;
import com.itmecasey.smwti.api.Progress;
import com.itmecasey.smwti.api.SmwtiPlugin;

/**
 * The Barely Prospecting lines in Show Me What This Is: the server works out what a basin, crucible or lure holds and SMWTI sends it
 * to the player looking at the block, with a progress bar for the soak, the melt and the hatch. Pannable blocks are described on the
 * client from their block state. SMWTI finds this class by its annotation, so nothing here loads without SMWTI.
 */
@SmwtiPlugin
public class ProspectSmwtiPlugin implements ISmwtiPlugin {
    @Override
    public void register(CommonRegistration registration) {
        registration.serverBlock((tooltip, level, pos, entity, detailed) -> LookInfo.lines(entity).forEach(tooltip::add));
        registration.progress(BasinBlockEntity.class, be -> be.basin().isSoaking() ? Progress.ratio(be.basin().progress(), 1.0) : java.util.OptionalDouble.empty(), 0);
        registration.progress(CrucibleBlockEntity.class, be -> be.crucible().queued() > 0 && be.crucible().isBurning()
                ? Progress.ratio(be.crucible().lava(), 2000.0) : java.util.OptionalDouble.empty(), 0);
        registration.progress(LureBlockEntity.class, be -> be.lure().isHatching()
                ? Progress.ratio(300.0 - be.lure().ticksLeft(), 300.0) : java.util.OptionalDouble.empty(), 0);
    }

    @Override
    public void registerClient(ClientRegistration registration) {
        registration.block((tooltip, view) -> {
            java.util.List<net.minecraft.network.chat.Component> lines = LookInfo.pannable(view.state());
            if (!lines.isEmpty()) {
                ProspectMod.LOGGER.debug("SMWTI pannable lines {}", lines.size());
            }
            lines.forEach(tooltip::add);
        });
        ProspectMod.LOGGER.info("Registered the Barely Prospecting lines with Show Me What This Is");
    }
}
