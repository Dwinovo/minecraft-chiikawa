package com.dwinovo.chiikawa.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

class PetDollDataTest {
    @Test
    void extractPetDataSupportsWrappedFormat() {
        CompoundTag petData = new CompoundTag();
        petData.putString("OwnerName", "tester");

        CompoundTag root = new CompoundTag();
        root.put(PetDollData.PET_DATA_KEY, petData.copy());

        CompoundTag extracted = PetDollData.extractPetData(root).orElseThrow();
        assertEquals(Optional.of("tester"), extracted.getString("OwnerName"));
    }

    @Test
    void sanitizeForReviveRemovesTransientFields() {
        CompoundTag petData = new CompoundTag();
        petData.putString("UUID", "8ca63f17-8da3-4de1-ab31-cf2857e345f7");
        petData.putInt("DeathTime", 22);
        petData.put("Pos", new ListTag());
        petData.put("Motion", new ListTag());
        petData.put("Rotation", new ListTag());
        petData.putString("CustomName", "\"remember me\"");

        PetDollData.sanitizeForRevive(petData);

        assertFalse(petData.contains("UUID"));
        assertFalse(petData.contains("DeathTime"));
        assertFalse(petData.contains("Pos"));
        assertFalse(petData.contains("Motion"));
        assertFalse(petData.contains("Rotation"));
        assertTrue(petData.contains("CustomName"));
    }
}
