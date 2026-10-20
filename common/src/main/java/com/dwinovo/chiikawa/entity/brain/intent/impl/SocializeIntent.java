package com.dwinovo.chiikawa.entity.brain.intent.impl;

import com.dwinovo.chiikawa.entity.brain.intent.IntentCategory;
import com.dwinovo.chiikawa.entity.brain.intent.IntentCheck;
import com.dwinovo.chiikawa.entity.brain.intent.IntentContext;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntent;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntents;
import com.dwinovo.chiikawa.init.InitActivity;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.schedule.Activity;

/**
 * Goes over to another pet and plays a little scene with it, one the pet has thought of on
 * looking around (see {@code PetSocialSensor}).
 *
 * <p>Scored above pottering about and below any of the pet's errands and work, so a scene
 * fills idle time and never gets in the way of a job. Like the labor board, it starts only
 * on a partner within the anchor's reach and keeps going while the partner stays within
 * the leash: a pet at heel only plays with pets around its owner's feet.
 */
public final class SocializeIntent implements PetIntent {
    @Override
    public ResourceLocation id() {
        return PetIntents.SOCIALIZE;
    }

    @Override
    public IntentCategory category() {
        return IntentCategory.SOCIAL;
    }

    @Override
    public Activity activity() {
        return InitActivity.SOCIALIZE.get();
    }

    @Override
    public IntentCheck canRun(IntentContext ctx) {
        return ctx.socialPartner().isPresent() ? IntentCheck.OK : IntentCheck.fail("no_partner");
    }

    @Override
    public Optional<GlobalPos> destination(IntentContext ctx) {
        return ctx.socialPartner();
    }
}
