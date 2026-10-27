package com.dwinovo.chiikawa.init;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.PetRules;
import com.dwinovo.chiikawa.platform.Services;
import com.dwinovo.chiikawa.qualification.LicenceView;
import com.dwinovo.chiikawa.task.PetTask;
import java.util.List;
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

    /** What the owner's screen shows of a pet's licences, see {@link com.dwinovo.chiikawa.entity.AbstractPet#licenceViews}. */
    public static final EntityDataSerializer<List<LicenceView>> LICENCE_VIEWS =
        EntityDataSerializer.forValueType(ByteBufCodecs.fromCodecWithRegistries(LicenceView.LIST_CODEC));

    /** What the owner has told a pet it may and may not do, see {@link com.dwinovo.chiikawa.entity.AbstractPet#getRules}. */
    public static final EntityDataSerializer<PetRules> PET_RULES = EntityDataSerializer.forValueType(PetRules.STREAM_CODEC);

    private InitEntityDataSerializers() {
    }

    public static void init() {
        Services.REGISTRY.registerEntityDataSerializer(
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "optional_pet_task"), OPTIONAL_PET_TASK);
        Services.REGISTRY.registerEntityDataSerializer(
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "licence_views"), LICENCE_VIEWS);
        Services.REGISTRY.registerEntityDataSerializer(
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "pet_rules"), PET_RULES);
    }
}
