package com.dwinovo.chiikawa.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import io.netty.buffer.Unpooled;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

/** What a pet is told it may do: how it is saved and sent, and that a save from before it is today's behaviour. */
class PetRulesTest {
    private static PetRules read(CompoundTag tag) {
        return PetRules.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow(false, org.junit.jupiter.api.Assertions::fail);
    }

    private static CompoundTag write(PetRules rules) {
        return (CompoundTag) PetRules.CODEC.encodeStart(NbtOps.INSTANCE, rules).getOrThrow(false, org.junit.jupiter.api.Assertions::fail);
    }

    @Test
    void aPetNobodyHasToldAnythingDoesEverythingAsItAlwaysHas() {
        assertTrue(PetRules.DEFAULT.pickUp());
        for (ErrandCategory category : ErrandCategory.values()) {
            assertTrue(PetRules.DEFAULT.permits(category), category.name());
        }
    }

    @Test
    void whatIsSavedComesBackAsItWas() {
        for (boolean pickUp : new boolean[] {true, false}) {
            PetRules rules = PetRules.DEFAULT.withPickUp(pickUp);

            assertEquals(rules, read(write(rules)));
        }
    }

    @Test
    void aKeyThatIsMissingIsTheDefaultsAndNothingElseIsLost() {
        assertEquals(PetRules.DEFAULT, read(new CompoundTag()));

        CompoundTag onlyPickingUp = new CompoundTag();
        onlyPickingUp.putBoolean("PickUp", false);
        assertEquals(PetRules.DEFAULT.withPickUp(false), read(onlyPickingUp));
    }

    @Test
    void theKeyIsTheNamedOne() {
        CompoundTag tag = write(PetRules.DEFAULT.withPickUp(false));

        assertEquals(Set.of("PickUp"), tag.getAllKeys());
        assertFalse(tag.getBoolean("PickUp"));
    }

    @Test
    void theDefaultsAreSavedAsNothing() {
        assertTrue(write(PetRules.DEFAULT).isEmpty());
    }

    @Test
    void whatIsSentArrivesAsItWas() {
        PetRules rules = PetRules.DEFAULT.withPickUp(false);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        rules.write(buffer);

        assertEquals(rules, PetRules.read(buffer));
    }

    @Test
    void onlyPickingUpIsForbiddenWhenItIsSwitchedOff() {
        PetRules off = PetRules.DEFAULT.withPickUp(false);

        for (ErrandCategory category : ErrandCategory.values()) {
            assertEquals(category != ErrandCategory.PICK_UP, off.permits(category), category.name());
        }
    }

    @Test
    void changingPickingUpLeavesTheOldRulesAlone() {
        PetRules changed = PetRules.DEFAULT.withPickUp(false);

        assertFalse(changed.pickUp());
        assertTrue(PetRules.DEFAULT.pickUp());
    }
}
