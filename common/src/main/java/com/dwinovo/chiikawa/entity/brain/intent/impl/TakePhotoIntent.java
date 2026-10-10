package com.dwinovo.chiikawa.entity.brain.intent.impl;

import com.dwinovo.chiikawa.entity.brain.intent.IntentCategory;
import com.dwinovo.chiikawa.entity.brain.intent.IntentCheck;
import com.dwinovo.chiikawa.entity.brain.intent.IntentContext;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntent;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntents;
import com.dwinovo.chiikawa.init.InitActivity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.schedule.Activity;

/**
 * Hachiware, with a camera and film in its backpack, takes a photo when there is something
 * worth one: its owner in sight, a friend caught in a little scene, a view at sunrise or
 * sunset or in a biome it has not photographed yet. What it is that makes a moment worth
 * it is {@link com.dwinovo.chiikawa.camera.PhotoOutlook}'s; this only asks.
 *
 * <p>Scored above picking things up and playing a scene, below a slip: a photo is rare
 * enough, with the camera's cooldown of half a day, that when there is a good moment it is
 * the thing to do, and nothing a pet works at is worth missing it for.
 */
public final class TakePhotoIntent implements PetIntent {
    private static final float SCORE = 0.45F;

    @Override
    public ResourceLocation id() {
        return PetIntents.TAKE_PHOTO;
    }

    @Override
    public IntentCategory category() {
        return IntentCategory.PHOTO;
    }

    @Override
    public Activity activity() {
        return InitActivity.TAKE_PHOTO.get();
    }

    @Override
    public IntentCheck canRun(IntentContext ctx) {
        return ctx.photo().canStart();
    }

    @Override
    public IntentCheck canContinue(IntentContext ctx) {
        return ctx.photo().canContinue();
    }

    @Override
    public float score(IntentContext ctx) {
        return SCORE;
    }
}
