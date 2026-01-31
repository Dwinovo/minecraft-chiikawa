package com.dwinovo.chiikawa.client.model;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.core.animation.AnimationState;

public abstract class AbstractPetModel<T extends AbstractPet> extends GeoModel<T> {
    private final String id;

    protected AbstractPetModel(String id) {
        this.id = id;
    }

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return new ResourceLocation(Constants.MOD_ID, "geo/" + id + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return new ResourceLocation(Constants.MOD_ID, "textures/entities/" + id + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return new ResourceLocation(Constants.MOD_ID, "animations/" + id + ".animation.json");
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        CoreGeoBone headBone = this.getAnimationProcessor().getBone("AllHead");
        if (headBone != null) {
            float netHeadYaw = modelData != null ? modelData.netHeadYaw() : 0.0F;
            float headPitch = modelData != null ? modelData.headPitch() : 0.0F;
            headBone.setRotY(netHeadYaw * ((float) Math.PI / 180F));
            headBone.setRotX(headPitch * ((float) Math.PI / 180F));
        }

        CoreGeoBone leftEarBone = this.getAnimationProcessor().getBone("LeftEar");
        CoreGeoBone rightEarBone = this.getAnimationProcessor().getBone("RightEar");
        CoreGeoBone tailBone = this.getAnimationProcessor().getBone("tail");

        double animationTicks = animationState.getData(DataTickets.TICK);
        float limbSwingAmount = animationState.getLimbSwingAmount();

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
