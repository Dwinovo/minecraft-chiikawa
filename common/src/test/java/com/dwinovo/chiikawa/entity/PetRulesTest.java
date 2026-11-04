package com.dwinovo.chiikawa.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;

/** What a pet is told it may do: how it is saved and sent, and that a save from before it is today's behaviour. */
class PetRulesTest {
    private static PetRules read(CompoundTag tag) {
        return PetRules.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
    }

    private static CompoundTag write(PetRules rules) {
        return (CompoundTag) PetRules.CODEC.encodeStart(NbtOps.INSTANCE, rules).getOrThrow();
    }

    @Test
    void aPetNobodyHasToldAnythingDoesEverythingAsItAlwaysHas() {
        assertTrue(PetRules.DEFAULT.pickUp());
        assertEquals(CombatStance.CLEAR, PetRules.DEFAULT.stance());
        for (ErrandCategory category : ErrandCategory.values()) {
            assertTrue(PetRules.DEFAULT.permits(category), category.name());
        }
    }

    @Test
    void whatIsSavedComesBackAsItWas() {
        PetRules rules = new PetRules(false, CombatStance.RETALIATE);

        assertEquals(rules, read(write(rules)));
    }

    @Test
    void everyStanceWithPickingUpOnOrOffRoundTrips() {
        for (CombatStance stance : CombatStance.values()) {
            for (boolean pickUp : new boolean[] {true, false}) {
                PetRules rules = PetRules.DEFAULT.withStance(stance).withPickUp(pickUp);
                assertEquals(rules, read(write(rules)));
            }
        }
    }

    @Test
    void aKeyThatIsMissingIsTheDefaultsAndNothingElseIsLost() {
        assertEquals(PetRules.DEFAULT, read(new CompoundTag()));

        CompoundTag onlyTheStance = new CompoundTag();
        onlyTheStance.putString("Stance", "none");
        assertEquals(PetRules.DEFAULT.withStance(CombatStance.NONE), read(onlyTheStance));

        CompoundTag onlyPickingUp = new CompoundTag();
        onlyPickingUp.putBoolean("PickUp", false);
        assertEquals(PetRules.DEFAULT.withPickUp(false), read(onlyPickingUp));
    }

    @Test
    void theKeysAreTheNamedOnes() {
        CompoundTag tag = write(new PetRules(false, CombatStance.RETALIATE));

        assertEquals(Set.of("PickUp", "Stance"), tag.keySet());
        assertEquals("retaliate", tag.getStringOr("Stance", ""));
        assertFalse(tag.getBooleanOr("PickUp", true));
    }

    @Test
    void theDefaultsAreSavedAsNothing() {
        assertTrue(write(PetRules.DEFAULT).isEmpty());
    }

    @Test
    void aNameNothingKnowsIsNotTaken() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Stance", "berserk");

        assertTrue(PetRules.CODEC.parse(NbtOps.INSTANCE, tag).error().isPresent());
    }

    @Test
    void whatIsSentArrivesAsItWas() {
        PetRules rules = new PetRules(false, CombatStance.NONE);
        ByteBuf buffer = Unpooled.buffer();

        PetRules.STREAM_CODEC.encode(buffer, rules);

        assertEquals(rules, PetRules.STREAM_CODEC.decode(buffer));
    }

    @Test
    void onlyPickingUpIsForbiddenWhenItIsSwitchedOff() {
        PetRules off = PetRules.DEFAULT.withPickUp(false);

        for (ErrandCategory category : ErrandCategory.values()) {
            assertEquals(category != ErrandCategory.PICK_UP, off.permits(category), category.name());
        }
    }

    @Test
    void changingOneThingLeavesTheRestAndTheOldRulesAlone() {
        PetRules rules = PetRules.DEFAULT.withPickUp(false);

        PetRules changed = rules.withStance(CombatStance.NONE);

        assertFalse(changed.pickUp());
        assertEquals(CombatStance.NONE, changed.stance());
        assertEquals(CombatStance.CLEAR, rules.stance());
    }
}
