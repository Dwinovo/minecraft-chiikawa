package com.dwinovo.chiikawa.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PetDirectiveTest {
    @Test
    void savedValuesKeepTheirPetModeMeaning() {
        assertEquals(PetDirective.FOLLOW, PetDirective.fromId(0));
        assertEquals(PetDirective.STAY, PetDirective.fromId(1));
        assertEquals(PetDirective.FREE, PetDirective.fromId(2));
    }

    @Test
    void unknownValuesFallBackToFollow() {
        assertEquals(PetDirective.FOLLOW, PetDirective.fromId(-1));
        assertEquals(PetDirective.FOLLOW, PetDirective.fromId(3));
    }

    @Test
    void sneakClickCyclesFollowStayFree() {
        assertEquals(PetDirective.STAY, PetDirective.FOLLOW.next());
        assertEquals(PetDirective.FREE, PetDirective.STAY.next());
        assertEquals(PetDirective.FOLLOW, PetDirective.FREE.next());
    }
}
