package com.dwinovo.chiikawa.client.render;

import com.dwinovo.chiikawa.client.render.layer.PetHeldItemLayer;
import com.dwinovo.chiikawa.entity.AbstractPet;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.GeckoLibConstants;
import net.minecraft.client.renderer.LevelRenderer;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.constant.dataticket.DataTicket;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;

import java.util.Map;

public abstract class AbstractPetRender<T extends AbstractPet> extends GeoEntityRenderer<T, AbstractPetRender.PetRenderState> {
    public static final DataTicket<Float> HEAD_YAW = DataTicket.create("chiikawa_head_yaw", Float.class);
    public static final DataTicket<Float> HEAD_PITCH = DataTicket.create("chiikawa_head_pitch", Float.class);

    protected AbstractPetRender(Context renderManager, GeoModel<T> model) {
        super(renderManager, model);
        addRenderLayer(new PetHeldItemLayer<>(this));
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
    public void addRenderData(T animatable, Void relatedObject, PetRenderState renderState) {
        float partialTick = renderState.getOrDefaultGeckolibData(DataTickets.PARTIAL_TICK, 0f);

        if (!renderState.hasGeckolibData(DataTickets.PACKED_LIGHT)) {
            renderState.addGeckolibData(DataTickets.PACKED_LIGHT,
                    LevelRenderer.getLightColor(animatable.level(), animatable.blockPosition()));
        }
    }

    @Override
    protected PetRenderState createBaseRenderState(T entity) {
        return new PetRenderState();
    }

    @Override
    public PetRenderState createRenderState() {
        return new PetRenderState();
    }

    public static class PetRenderState extends LivingEntityRenderState implements GeoRenderState {
        private final Map<DataTicket<?>, Object> geckolibData = new Object2ObjectOpenHashMap<>();

        @Override
        public <D> void addGeckolibData(DataTicket<D> dataTicket, @Nullable D data) {
            this.geckolibData.put(dataTicket, data);
        }

        @Override
        public boolean hasGeckolibData(DataTicket<?> dataTicket) {
            return this.geckolibData.containsKey(dataTicket);
        }

        @Nullable
        @Override
        public <D> D getGeckolibData(DataTicket<D> dataTicket) {
            Object data = this.geckolibData.get(dataTicket);

            if (data == null && !hasGeckolibData(dataTicket)) {
                GeckoLibConstants.LOGGER.warn("Missing GeoRenderState data for ticket: {}", dataTicket);
                return null;
            }

            try {
                return (D)data;
            }
            catch (ClassCastException ex) {
                GeckoLibConstants.LOGGER.error("Attempted to retrieve incorrectly typed data from GeoRenderState. Possibly a mod or DataTicket conflict? Expected: {}, found data type {}", dataTicket, data.getClass().getName(), ex);

                throw ex;
            }
        }

        @Nullable
        @Override
        public <D> D getOrDefaultGeckolibData(DataTicket<D> dataTicket, @Nullable D defaultValue) {
            Object data = this.geckolibData.get(dataTicket);

            if (data == null && !hasGeckolibData(dataTicket))
                return defaultValue;

            try {
                return (D)data;
            }
            catch (ClassCastException ex) {
                GeckoLibConstants.LOGGER.error("Attempted to retrieve incorrectly typed data from GeoRenderState. Possibly a mod or DataTicket conflict? Expected: {}, found data type {}", dataTicket, data.getClass().getName(), ex);

                return defaultValue;
            }
        }

        @Override
        public Map<DataTicket<?>, Object> getDataMap() {
            return this.geckolibData;
        }
    }
}
