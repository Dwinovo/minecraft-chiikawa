package com.dwinovo.chiikawa.init;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.platform.Services;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.ResourceLocation;

/**
 * Entity data serializers the game no longer has one of. A compound tag stopped being
 * something an entity could sync in 1.21.5; the pet's slip still travels as one.
 */
public final class InitDataSerializers {
    public static final EntityDataSerializer<CompoundTag> COMPOUND_TAG =
        Services.REGISTRY.registerEntityDataSerializer(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "compound_tag"),
            EntityDataSerializer.forValueType(ByteBufCodecs.COMPOUND_TAG)
        );

    private InitDataSerializers() {
    }

    public static void init() {
    }
}
