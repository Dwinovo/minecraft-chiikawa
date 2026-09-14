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
        // Fall distance: "FallDistance" up to 1.21.4, "fall_distance" from 1.21.5. Keeping the
        // lethal value makes the revived pet die again on its first tiny landing. Both keys are
        // stripped so dolls carried across a Minecraft upgrade are cleaned too.
        petData.remove("FallDistance");
        petData.remove("fall_distance");
        petData.remove("FallFlying");
        petData.remove("OnGround");
        petData.remove("Air");
        petData.remove("Fire");
        petData.remove("PortalCooldown");
        petData.remove("DeathTime");
        petData.remove("HurtTime");
        petData.remove("HurtByTimestamp");
        // Leash: "Leash" up to 1.20.4, "leash" from 1.20.5. A pet that died on a lead must not
        // come back leashed (and duplicate the lead).
        petData.remove("Leash");
        petData.remove("leash");
        // Effects: "ActiveEffects" up to 1.20.4, "active_effects" from 1.20.5. A pet killed by
        // Wither or Poison would otherwise come back with the effect that killed it.
        petData.remove("ActiveEffects");
        petData.remove("active_effects");
        petData.remove("Health");
        petData.remove("AbsorptionAmount");
    }
}
