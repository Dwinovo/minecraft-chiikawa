package com.dwinovo.chiikawa.client.model;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;

import net.minecraft.resources.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.constant.dataticket.DataTicket;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.constant.DataTickets;

public abstract class AbstractPetModel<T extends AbstractPet> extends GeoModel<T> {
    public static final DataTicket<Float> LIMB_SWING_AMOUNT = DataTicket.create("chiikawa_limb_swing_amount", Float.class);
    private final String id;

    protected AbstractPetModel(String id) {
        this.id = id;
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(Constants.MOD_ID, id);
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/entities/" + id + ".png");
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        return Identifier.fromNamespaceAndPath(Constants.MOD_ID, id);
    }

    @Override
    public void addAdditionalStateData(T animatable, Object relatedObject, GeoRenderState renderState) {
        super.addAdditionalStateData(animatable, relatedObject, renderState);
        float partialTick = renderState.getOrDefaultGeckolibData(DataTickets.PARTIAL_TICK, 0f);
        renderState.addGeckolibData(LIMB_SWING_AMOUNT, animatable.walkAnimation.speed(partialTick));
    }
}

