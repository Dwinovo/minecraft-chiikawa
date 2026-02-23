package com.dwinovo.chiikawa.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CakeReviveRitualTest {
    @Test
    void offeringConsumesExactlyOneCakeBite() {
        assertEquals(3, CakeReviveRitual.nextBitesAfterOffering(2));
        assertEquals(false, CakeReviveRitual.shouldRemoveCakeAfterOffering(2));
    }

    @Test
    void offeringOnLastBiteConsumesEntireCake() {
        assertEquals(true, CakeReviveRitual.shouldRemoveCakeAfterOffering(6));
    }

    @Test
    void rejectsOutOfRangeBiteValues() {
        assertThrows(IllegalArgumentException.class, () -> CakeReviveRitual.nextBitesAfterOffering(-1));
        assertThrows(IllegalArgumentException.class, () -> CakeReviveRitual.shouldRemoveCakeAfterOffering(7));
    }
}
