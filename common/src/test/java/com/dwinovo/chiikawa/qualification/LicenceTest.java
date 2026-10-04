package com.dwinovo.chiikawa.qualification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.InclusiveRange;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LicenceTest {
    private static final ResourceLocation WEEDING = new ResourceLocation("chiikawa", "weeding");
    private static final ResourceLocation WEEDING_SLIP = new ResourceLocation("chiikawa", "weeding");
    private static final ResourceLocation OTHER_SLIP = new ResourceLocation("chiikawa", "street_performance");
    private static final ResourceLocation OTHER = new ResourceLocation("chiikawa", "drinking");
    private static GlobalPos board;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        board = GlobalPos.of(Level.OVERWORLD, new BlockPos(1, 64, 2));
    }

    @AfterEach
    void forgetLicences() {
        Qualifications.replaceAll(Map.of());
    }

    /** The exam spends the practice and the book; the result waits, with the board, for the morning. */
    @Test
    void sittingSpendsThePracticeAndTheBookAndKeepsTheResultForTheMorning() {
        Licence sat = Licence.NONE.practised().practised().withBookRead().called(board, 3L).sat(true);

        assertEquals(0, sat.held());
        assertEquals(0, sat.practice());
        assertFalse(sat.read());
        assertEquals(Optional.of(new ExamStage.Sat(board, 3L, true)), sat.paper());
    }

    /** Only a pet called to an exam can sit it, and only one called can be let off it. */
    @Test
    void onlyACalledPetSitsOrIsLetOff() {
        Licence practised = Licence.NONE.practised();

        assertEquals(practised, practised.sat(true), "sat an exam nobody called it to");
        assertEquals(practised, practised.called(board, 3L).excused());
        assertEquals(practised.called(board, 3L).sat(false), practised.called(board, 3L).sat(false).excused(),
            "let off an exam it has sat");
    }

    @Test
    void aPassGoesUpAGradeAndForgetsTheFailsAndAFailAddsOne() {
        Licence failedTwice = Licence.NONE.practised().called(board, 1L).sat(false).announced()
            .practised().called(board, 2L).sat(false).announced();
        assertEquals(0, failedTwice.held());
        assertEquals(2, failedTwice.fails());

        Licence passed = failedTwice.practised().called(board, 3L).sat(true).announced();
        assertEquals(1, passed.held());
        assertEquals(0, passed.fails());
        assertEquals(ExamStage.NONE, passed.exam());
    }

    @Test
    void finishingASlipIsPracticeOnlyForTheLicencesItIsPracticeFor() {
        Qualifications.replaceAll(Map.of(WEEDING, weeding()));
        PetLicences licences = new PetLicences();

        licences.practised(WEEDING_SLIP);
        licences.practised(OTHER_SLIP);

        assertEquals(1, licences.get(WEEDING).practice());
    }

    /** "Grade 3 or better" is grade 3, 2 or 1; nothing counts while no data pack has the licence. */
    @Test
    void aRequirementIsMetByTheGradeNamedOrABetterOne() {
        Qualifications.replaceAll(Map.of(WEEDING, weeding()));
        LicenceRequirement gradeThree = new LicenceRequirement(WEEDING, 3);
        PetLicences licences = new PetLicences();

        assertFalse(gradeThree.metBy(licences), "no grade at all");
        licences.set(WEEDING, Licence.holding(2));
        assertFalse(gradeThree.metBy(licences), "grade 4");
        licences.set(WEEDING, Licence.holding(3));
        assertTrue(gradeThree.metBy(licences), "grade 3");
        licences.set(WEEDING, Licence.holding(5));
        assertTrue(gradeThree.metBy(licences), "grade 1");

        Qualifications.replaceAll(Map.of());
        assertFalse(gradeThree.metBy(licences), "a licence no pack has");
    }

    /** Saved with the pet, and a licence it never had anything to do with is not saved at all. */
    @Test
    void licencesAreSavedAndLoadedWithThePet() {
        PetLicences licences = new PetLicences();
        licences.set(WEEDING, new Licence(2, 3, 1, true, new ExamStage.Sat(board, 13L, false)));
        licences.set(OTHER, Licence.NONE.practised().called(board, 14L));
        CompoundTag tag = new CompoundTag();
        licences.save(tag);

        PetLicences loaded = new PetLicences();
        loaded.load(tag);
        assertEquals(licences.get(WEEDING), loaded.get(WEEDING));
        assertEquals(licences.get(OTHER), loaded.get(OTHER));

        CompoundTag none = new CompoundTag();
        new PetLicences().save(none);
        assertTrue(none.isEmpty());
    }

    private static Qualification weeding() {
        return new Qualification(5, new ExamFee(Items.DIAMOND, 1), WEEDING_SLIP, 1, List.of(0.40F, 0.30F, 0.22F, 0.15F, 0.08F), 0.03F, 0.30F,
            0.05F, 0.20F, Items.BOOK, 0.25F, 0.95F, new InclusiveRange<>(480, 560), List.of(70, 18, 8, 3, 1, 0));
    }
}
