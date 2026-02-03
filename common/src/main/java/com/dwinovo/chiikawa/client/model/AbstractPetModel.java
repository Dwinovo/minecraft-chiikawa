package com.dwinovo.chiikawa.client.model;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animatable.processing.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.constant.dataticket.DataTicket;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import com.dwinovo.chiikawa.client.render.AbstractPetRender;

public abstract class AbstractPetModel<T extends AbstractPet> extends GeoModel<T> {
    private static final DataTicket<Float> LIMB_SWING_AMOUNT = DataTicket.create("chiikawa_limb_swing_amount", Float.class);
    private final String id;

    protected AbstractPetModel(String id) {
        this.id = id;
    }

    @Override
    public ResourceLocation getModelResource(GeoRenderState renderState) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, id);
    }

    @Override
    public ResourceLocation getTextureResource(GeoRenderState renderState) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/entities/" + id + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, id);
    }

    @Override
    public void addAdditionalStateData(T animatable, GeoRenderState renderState) {
        float partialTick = renderState.getOrDefaultGeckolibData(DataTickets.PARTIAL_TICK, 0f);
        renderState.addGeckolibData(LIMB_SWING_AMOUNT, animatable.walkAnimation.speed(partialTick));
    }

    @Override
    public void setCustomAnimations(AnimationState<T> animationState) {
        super.setCustomAnimations(animationState);
        GeoBone headBone = this.getAnimationProcessor().getBone("AllHead");
        if (headBone != null) {
            float netHeadYaw = animationState.getDataOrDefault(AbstractPetRender.HEAD_YAW, 0f);
            float headPitch = animationState.getDataOrDefault(AbstractPetRender.HEAD_PITCH, 0f);
            headBone.setRotY(-netHeadYaw * ((float) Math.PI / 180F));
            headBone.setRotX(-headPitch * ((float) Math.PI / 180F));
        }

        GeoBone leftEarBone = this.getAnimationProcessor().getBone("LeftEar");
        GeoBone rightEarBone = this.getAnimationProcessor().getBone("RightEar");
        GeoBone tailBone = this.getAnimationProcessor().getBone("tail");

        double animationTicks = animationState.getData(DataTickets.ANIMATION_TICKS);
        float limbSwingAmount = animationState.getDataOrDefault(LIMB_SWING_AMOUNT, 0f);

        float breathingSpeed = 0.1F;
        float earSwingAmount = 0.1F;
        float earTwistAmount = 0.1F;
        float earBackwardSwing = -limbSwingAmount * 1.0F;

        if (leftEarBone != null) {
            leftEarBone.setRotY(Mth.cos((float) animationTicks * breathingSpeed) * earSwingAmount - earBackwardSwing);
            leftEarBone.setRotZ(Mth.sin((float) animationTicks * breathingSpeed) * earTwistAmount);
        }

        if (rightEarBone != null) {
            rightEarBone.setRotY(-Mth.cos((float) animationTicks * breathingSpeed) * earSwingAmount + earBackwardSwing);
            rightEarBone.setRotZ(-Mth.sin((float) animationTicks * breathingSpeed) * earTwistAmount);
        }

        if (tailBone != null) {
            tailBone.setRotY(Mth.cos((float) animationTicks * breathingSpeed) * 0.15F);
        }
    }
}

