package com.dwinovo.chiikawa.init;

import com.dwinovo.chiikawa.entity.PetCharacters;
import com.dwinovo.chiikawa.entity.impl.ChiikawaPet;
import com.dwinovo.chiikawa.entity.impl.FuruhonyaPet;
import com.dwinovo.chiikawa.entity.impl.HachiwarePet;
import com.dwinovo.chiikawa.entity.impl.KurimanjuPet;
import com.dwinovo.chiikawa.entity.impl.MomongaPet;
import com.dwinovo.chiikawa.entity.impl.RakkoPet;
import com.dwinovo.chiikawa.entity.impl.ShisaPet;
import com.dwinovo.chiikawa.entity.impl.UsagiPet;
import com.dwinovo.chiikawa.platform.Services;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Animal;

public final class InitEntity {
    public static final Supplier<EntityType<UsagiPet>> USAGI_PET = registerPet(PetCharacters.USAGI, UsagiPet::new);
    public static final Supplier<EntityType<HachiwarePet>> HACHIWARE_PET = registerPet(PetCharacters.HACHIWARE, HachiwarePet::new);
    public static final Supplier<EntityType<ChiikawaPet>> CHIIKAWA_PET = registerPet(PetCharacters.CHIIKAWA, ChiikawaPet::new);
    public static final Supplier<EntityType<ShisaPet>> SHISA_PET = registerPet(PetCharacters.SHISA, ShisaPet::new);
    public static final Supplier<EntityType<MomongaPet>> MOMONGA_PET = registerPet(PetCharacters.MOMONGA, MomongaPet::new);
    public static final Supplier<EntityType<KurimanjuPet>> KURIMANJU_PET = registerPet(PetCharacters.KURIMANJU, KurimanjuPet::new);
    public static final Supplier<EntityType<RakkoPet>> RAKKO_PET = registerPet(PetCharacters.RAKKO, RakkoPet::new);
    public static final Supplier<EntityType<FuruhonyaPet>> FURUHONYA_PET = registerPet(PetCharacters.FURUHONYA, FuruhonyaPet::new);

    private InitEntity() {
    }

    public static void init() {
    }

    private static <T extends Animal> Supplier<EntityType<T>> registerPet(ResourceLocation id, EntityType.EntityFactory<T> factory) {
        return Services.REGISTRY.<EntityType<T>>register(
            BuiltInRegistries.ENTITY_TYPE,
            id,
            () -> EntityType.Builder.of(factory, MobCategory.CREATURE)
                .sized(0.6F, 0.8F)
                .build(id.toString())
        );
    }
}
