package com.dwinovo.chiikawa.entity.brain.errand.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** What falls on a pet, from the level's weather and the biome it stands in. */
class WeatherTest {
    @Test
    void itRainsOnAPetWhereTheLevelRainsAndTheBiomeLetsItFallAsRain() {
        assertEquals(Weather.RAIN, Weather.of(true, false, true));
        assertEquals(Weather.THUNDER, Weather.of(true, true, true));
    }

    @Test
    void nothingFallsOnAPetInADryOrSnowyBiomeOrWhenTheSkyIsClear() {
        assertEquals(Weather.CLEAR, Weather.of(true, false, false));
        assertEquals(Weather.CLEAR, Weather.of(true, true, false));
        assertEquals(Weather.CLEAR, Weather.of(false, false, true));
        assertEquals(Weather.CLEAR, Weather.of(false, true, true), "a storm has to be raining");
    }

    @Test
    void aStormIsAlsoRain() {
        assertTrue(Weather.THUNDER.raining());
        assertTrue(Weather.RAIN.raining());
        assertFalse(Weather.CLEAR.raining());
    }
}
