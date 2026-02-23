package com.dwinovo.chiikawa.item;

import com.dwinovo.chiikawa.entity.AbstractPet;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class PetDollData {
    public static final String PET_DATA_KEY = "PetData";

    private PetDollData() {
    }

    public static void writePetToDoll(ItemStack stack, AbstractPet pet) {
        CompoundTag petData = new CompoundTag();
        pet.saveWithoutId(petData);

        stack.getOrCreateTag().put(PET_DATA_KEY, petData);
    }

    public static Optional<CompoundTag> readPetData(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null || root.isEmpty()) {
            return Optional.empty();
        }
        return extractPetData(root.copy());
    }

    public static Optional<CompoundTag> extractPetData(CompoundTag root) {
        if (root.isEmpty()) {
            return Optional.empty();
        }
        if (root.contains(PET_DATA_KEY, 10)) {
            return Optional.of(root.getCompound(PET_DATA_KEY));
        }

        // Backward compatibility: treat root itself as entity data if no wrapper exists.
        return Optional.of(root);
    }

    public static void sanitizeForRevive(CompoundTag petData) {
        petData.remove("UUID");
        petData.remove("Pos");
        petData.remove("Motion");
        petData.remove("Rotation");
        petData.remove("FallDistance");
        petData.remove("OnGround");
        petData.remove("Air");
        petData.remove("Fire");
        petData.remove("PortalCooldown");
        petData.remove("DeathTime");
        petData.remove("HurtTime");
        petData.remove("HurtByTimestamp");
        petData.remove("Leash");
        petData.remove("Health");
        petData.remove("AbsorptionAmount");
    }
}
