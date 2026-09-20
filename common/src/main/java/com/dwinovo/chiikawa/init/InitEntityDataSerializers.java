package com.dwinovo.chiikawa.init;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.platform.Services;
import com.dwinovo.chiikawa.task.PetTask;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.Identifier;

/**
 * Entity data the game has no serializer for. Vanilla no longer syncs a bare compound
 * tag, so a value that used to travel as one travels as itself here.
 */
public final class InitEntityDataSerializers {
    /** The slip a pet carries, see {@link com.dwinovo.chiikawa.entity.AbstractPet#getTask}. */
    public static final EntityDataSerializer<Optional<PetTask>> OPTIONAL_PET_TASK =
        EntityDataSerializer.forValueType(ByteBufCodecs.optional(ByteBufCodecs.fromCodecWithRegistries(PetTask.CODEC)));

    private InitEntityDataSerializers() {
    }

    public static void init() {
        Services.REGISTRY.registerEntityDataSerializer(
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "optional_pet_task"), OPTIONAL_PET_TASK);
    }
}
