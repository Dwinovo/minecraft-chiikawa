package com.dwinovo.chiikawa.client.render;

import com.dwinovo.chiikawa.client.model.AbstractPetModel;
import com.dwinovo.chiikawa.client.render.layer.PetHeldItemLayer;
import com.dwinovo.chiikawa.entity.AbstractPet;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.constant.dataticket.DataTicket;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.BoneSnapshots;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.renderer.base.RenderPassInfo;

import java.util.Map;

public abstract class AbstractPetRender<T extends AbstractPet> extends GeoEntityRenderer<T, AbstractPetRender.PetRenderState> {
    public static final DataTicket<Float> HEAD_YAW = DataTicket.create("chiikawa_head_yaw", Float.class);
    public static final DataTicket<Float> HEAD_PITCH = DataTicket.create("chiikawa_head_pitch", Float.class);

    protected AbstractPetRender(Context renderManager, GeoModel<T> model) {
        super(renderManager, model);
        withRenderLayer(new PetHeldItemLayer<>(this));
    }

    @Override
    public PetRenderState createRenderState(T entity, Void relatedObject) {
        return new PetRenderState();
    }

    @Override
    public void extractRenderState(T entity, PetRenderState renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);

        float headYaw = Mth.rotLerp(partialTick, entity.yHeadRotO, entity.getYHeadRot());
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        float netHeadYaw = headYaw - bodyYaw;
        float headPitch = Mth.rotLerp(partialTick, entity.xRotO, entity.getXRot());

        renderState.addGeckolibData(HEAD_YAW, netHeadYaw);
        renderState.addGeckolibData(HEAD_PITCH, headPitch);
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<PetRenderState> renderPass, BoneSnapshots boneSnapshots) {
        super.adjustModelBonesForRender(renderPass, boneSnapshots);

        PetRenderState renderState = renderPass.renderState();
        float netHeadYaw = renderState.getOrDefaultGeckolibData(HEAD_YAW, 0f);
        float headPitch = renderState.getOrDefaultGeckolibData(HEAD_PITCH, 0f);
        float limbSwingAmount = renderState.getOrDefaultGeckolibData(AbstractPetModel.LIMB_SWING_AMOUNT, 0f);
        double animationTicks = renderState.getOrDefaultGeckolibData(DataTickets.TICK, 0.0);

        boneSnapshots.ifPresent("AllHead", snapshot -> {
            snapshot.setRotY(-netHeadYaw * ((float) Math.PI / 180F));
            snapshot.setRotX(-headPitch * ((float) Math.PI / 180F));
        });

        float breathingSpeed = 0.1F;
        float earSwingAmount = 0.1F;
        float earTwistAmount = 0.1F;
        float earBackwardSwing = -limbSwingAmount * 1.0F;

        boneSnapshots.ifPresent("LeftEar", snapshot -> {
            snapshot.setRotY(Mth.cos((float) animationTicks * breathingSpeed) * earSwingAmount - earBackwardSwing);
            snapshot.setRotZ(Mth.sin((float) animationTicks * breathingSpeed) * earTwistAmount);
        });
        boneSnapshots.ifPresent("RightEar", snapshot -> {
            snapshot.setRotY(-Mth.cos((float) animationTicks * breathingSpeed) * earSwingAmount + earBackwardSwing);
            snapshot.setRotZ(-Mth.sin((float) animationTicks * breathingSpeed) * earTwistAmount);
        });
        boneSnapshots.ifPresent("tail", snapshot -> {
            snapshot.setRotY(Mth.cos((float) animationTicks * breathingSpeed) * 0.15F);
        });
    }

    public static class PetRenderState extends LivingEntityRenderState implements GeoRenderState {
        private final Map<DataTicket<?>, Object> geckolibData = new Reference2ObjectOpenHashMap<>();

        @Override
        public <D> void addGeckolibData(DataTicket<D> dataTicket, @Nullable D data) {
            this.geckolibData.put(dataTicket, data);
        }

        @Override
        public boolean hasGeckolibData(DataTicket<?> dataTicket) {
            return this.geckolibData.containsKey(dataTicket);
        }

        @Override
        public Map<DataTicket<?>, Object> getDataMap() {
            return this.geckolibData;
        }
    }
}
