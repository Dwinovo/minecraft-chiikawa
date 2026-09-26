package com.dwinovo.chiikawa.qualification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.InclusiveRange;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LicenceTest {
    private static final ResourceLocation WEEDING = ResourceLocation.fromNamespaceAndPath("chiikawa", "weeding");
    private static final ResourceLocation WEEDING_SLIP = ResourceLocation.fromNamespaceAndPath("chiikawa", "weeding");
    private static final ResourceLocation OTHER_SLIP = ResourceLocation.fromNamespaceAndPath("chiikawa", "street_performance");

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @AfterEach
    void forgetLicences() {
        Qualifications.replaceAll(Map.of());
    }

    /** The exam spends the practice and the book; the result waits for the morning. */
    @Test
    void sittingSpendsThePracticeAndTheBookAndKeepsTheResultForTheMorning() {
        Licence sat = Licence.NONE.practised().practised().withBookRead().sat(true);

        assertEquals(0, sat.held());
        assertEquals(0, sat.practice());
        assertFalse(sat.read());
        assertEquals(Optional.of(true), sat.pending());
    }

    @Test
    void aPassGoesUpAGradeAndForgetsTheFailsAndAFailAddsOne() {
        Licence failedTwice = Licence.NONE.practised().sat(false).announced().practised().sat(false).announced();
        assertEquals(0, failedTwice.held());
        assertEquals(2, failedTwice.fails());

        Licence passed = failedTwice.practised().sat(true).announced();
        assertEquals(1, passed.held());
        assertEquals(0, passed.fails());
        assertTrue(passed.pending().isEmpty());
    }

    @Test
    void finishingASlipIsPracticeOnlyForTheLicencesItIsPracticeFor() {
        Qualifications.replaceAll(Map.of(WEEDING, weeding()));
        PetLicences licences = new PetLicences();

        licences.practised(WEEDING_SLIP);
        licences.practised(OTHER_SLIP);

        assertEquals(1, licences.get(WEEDING).practice());
    }

    /** Saved with the pet, and a licence it never had anything to do with is not saved at all. */
    @Test
    void licencesAreSavedAndLoadedWithThePet() {
        PetLicences licences = new PetLicences();
        licences.set(WEEDING, new Licence(2, 3, 1, true, Optional.of(false), 13L, true));
        CompoundTag tag = new CompoundTag();
        licences.save(tag);

        PetLicences loaded = new PetLicences();
        loaded.load(tag);
        assertEquals(licences.get(WEEDING), loaded.get(WEEDING));

        CompoundTag none = new CompoundTag();
        new PetLicences().save(none);
        assertTrue(none.isEmpty());
    }

    private static Qualification weeding() {
        return new Qualification(5, 7, WEEDING_SLIP, 1, List.of(0.40F, 0.30F, 0.22F, 0.15F, 0.08F), 0.03F, 0.30F,
            0.05F, 0.20F, Items.BOOK, 0.25F, 0.95F, new InclusiveRange<>(480, 560), List.of(70, 18, 8, 3, 1, 0));
    }
}
