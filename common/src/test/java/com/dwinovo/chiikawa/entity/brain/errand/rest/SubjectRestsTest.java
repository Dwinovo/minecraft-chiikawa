package com.dwinovo.chiikawa.entity.brain.errand.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** The memory of what a pet has lately given up on. */
class SubjectRestsTest {
    private static final ResourceLocation HARVEST = ResourceLocation.fromNamespaceAndPath("chiikawa", "harvest");
    private static final ResourceLocation PLANT = ResourceLocation.fromNamespaceAndPath("chiikawa", "plant");

    @Test
    void aRestIsOnAnErrandAndASubjectAndRunsOut() {
        SubjectRests rests = SubjectRests.NONE.with(HARVEST, "10,64,10", 200, 1000);

        assertEquals(200, rests.remaining(HARVEST, "10,64,10", 1000));
        assertEquals(50, rests.remaining(HARVEST, "10,64,10", 1150));
        assertEquals(0, rests.remaining(HARVEST, "10,64,10", 1200));
        assertEquals(0, rests.remaining(HARVEST, "11,64,10", 1000), "another subject");
        assertEquals(0, rests.remaining(PLANT, "10,64,10", 1000), "another errand");
    }

    @Test
    void settingARestForNothingEndsIt() {
        SubjectRests rests = SubjectRests.NONE.with(HARVEST, "a", 200, 1000).with(HARVEST, "a", 0, 1010);

        assertEquals(0, rests.remaining(HARVEST, "a", 1010));
        assertEquals(0, rests.until().size());
    }

    @Test
    void restsThatAreOverAreLeftOutWhenAnotherIsSet() {
        SubjectRests rests = SubjectRests.NONE.with(HARVEST, "old", 100, 1000).with(HARVEST, "new", 100, 1500);

        assertEquals(1, rests.until().size());
        assertEquals(100, rests.remaining(HARVEST, "new", 1500));
    }
}
