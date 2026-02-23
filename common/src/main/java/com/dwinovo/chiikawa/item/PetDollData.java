package com.dwinovo.chiikawa.item;

import com.dwinovo.chiikawa.entity.AbstractPet;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class PetDollData {
    public static final String PET_DATA_KEY = "PetData";

    private PetDollData() {
    }

    public static void writePetToDoll(ItemStack stack, AbstractPet pet) {
        CompoundTag petData = new CompoundTag();
        pet.saveWithoutId(petData);

        CompoundTag root = new CompoundTag();
        root.put(PET_DATA_KEY, petData);
        CustomData.set(DataComponents.CUSTOM_DATA, stack, root);
    }

    public static Optional<CompoundTag> readPetData(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) {
            return Optional.empty();
        }
        return extractPetData(customData.copyTag());
    }

    public static Optional<CompoundTag> extractPetData(CompoundTag root) {
        if (root.isEmpty()) {
            return Optional.empty();
        }
        if (root.contains(PET_DATA_KEY)) {
            Optional<CompoundTag> wrapped = root.getCompound(PET_DATA_KEY);
            if (wrapped.isPresent()) {
                return wrapped;
            }
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
