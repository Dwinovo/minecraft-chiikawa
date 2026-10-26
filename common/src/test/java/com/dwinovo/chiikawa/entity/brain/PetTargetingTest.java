package com.dwinovo.chiikawa.entity.brain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PetTargetingTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID STRANGER = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID MOB = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    void neverTargetsItself() {
        assertTrue(PetTargeting.isProtected(true, false, false, OWNER, MOB, null));
        assertTrue(PetTargeting.isProtected(true, false, false, null, MOB, null));
    }

    @Test
    void neverTargetsOwner() {
        assertTrue(PetTargeting.isProtected(false, true, false, OWNER, OWNER, null));
        assertTrue(PetTargeting.isProtected(false, false, false, OWNER, OWNER, null));
    }

    @Test
    void neverTargetsAnyPlayer() {
        assertTrue(PetTargeting.isProtected(false, true, false, OWNER, STRANGER, null));
        assertTrue(PetTargeting.isProtected(false, true, false, null, STRANGER, null));
    }

    @Test
    void neverTargetsAnAllyWhoeverItIs() {
        assertTrue(PetTargeting.isProtected(false, false, true, OWNER, MOB, null));
        assertTrue(PetTargeting.isProtected(false, false, true, null, MOB, STRANGER));
    }

    @Test
    void neverTargetsPetsOfTheSameOwner() {
        assertTrue(PetTargeting.isProtected(false, false, false, OWNER, MOB, OWNER));
    }

    @Test
    void targetsHostilesAndOtherOwnersPets() {
        assertFalse(PetTargeting.isProtected(false, false, false, OWNER, MOB, null));
        assertFalse(PetTargeting.isProtected(false, false, false, OWNER, MOB, STRANGER));
    }

    @Test
    void untamedPetDoesNotTreatOwnerlessMobsAsAllies() {
        assertFalse(PetTargeting.isProtected(false, false, false, null, MOB, null));
    }
}
