package com.dwinovo.chiikawa.qualification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import com.dwinovo.chiikawa.testing.FixedRandom;
import java.util.List;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.InclusiveRange;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class QualificationExamTest {
    private static final float EPSILON = 1.0E-5F;
    private static final long DAY = 3L;
    private static Qualification weeding;
    private static GlobalPos board;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        weeding = new Qualification(5, new ExamFee(Items.DIAMOND, 1), ResourceLocation.fromNamespaceAndPath("chiikawa", "weeding"),
            1, List.of(0.40F, 0.30F, 0.22F, 0.15F, 0.08F), 0.03F, 0.30F, 0.05F, 0.20F, Items.BOOK, 0.25F, 0.95F,
            new InclusiveRange<>(480, 560), List.of(70, 18, 8, 3, 1, 0));
        board = GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO);
    }

    /** From an hour after sunrise until late enough in the afternoon to get to the desk and write. */
    @Test
    void aPetIsSignedUpOnlyInTheWorkingDay() {
        assertFalse(QualificationExam.maySignUp(at(DAY, 500L)), "before the working day");
        assertTrue(QualificationExam.maySignUp(at(DAY, QualificationExam.EXAM_FROM)));
        assertTrue(QualificationExam.maySignUp(at(DAY, QualificationExam.LAST_CALL - 1)));
        assertFalse(QualificationExam.maySignUp(at(DAY, QualificationExam.LAST_CALL)), "too late to sit it");
        assertFalse(QualificationExam.maySignUp(at(DAY, 18000L)), "at night");
    }

    /** The licence says why a pet cannot be signed up: every grade held, no practice, or something on. */
    @Test
    void theLicenceSaysWhyAPetCannotBeSignedUp() {
        assertEquals(Optional.of(Ineligible.UNPRACTISED), QualificationExam.whyNot(weeding, Licence.NONE));
        assertEquals(Optional.empty(), QualificationExam.whyNot(weeding, Licence.NONE.practised()));
        assertEquals(Optional.of(Ineligible.TOP_GRADE), QualificationExam.whyNot(weeding, Licence.holding(5).practised()));
        assertEquals(Optional.of(Ineligible.BUSY),
            QualificationExam.whyNot(weeding, Licence.NONE.practised().called(board, DAY)), "signed up already");
        assertEquals(Optional.of(Ineligible.BUSY),
            QualificationExam.whyNot(weeding, Licence.NONE.practised().called(board, DAY).sat(true)), "waiting for results");
    }

    /** The odds come apart into what the owner is shown, and put back together into the chance. */
    @Test
    void theOddsArePiecesThatMakeTheChance() {
        Personality.Leaning chiikawa = new Personality.Leaning(0.7F, 0.0F, PetReaction.CONFUSED);
        PassOdds odds = QualificationExam.odds(weeding, new Licence(0, 5, 1, true, ExamStage.NONE), chiikawa);

        assertEquals(0.40F, odds.base(), EPSILON);
        assertEquals(0.15F, odds.practice(), EPSILON);
        assertEquals(0.05F, odds.failing(), EPSILON);
        assertEquals(0.25F, odds.book(), EPSILON);
        assertEquals(0.7F, odds.aptitude(), EPSILON);
        assertEquals((0.40F + 0.15F + 0.05F + 0.25F) * 0.7F, odds.chance(), EPSILON);
    }

    /** Called for the day it was opened, until the exam closes; after that it has missed it. */
    @Test
    void aCallIsGoodForTheDayItWasMade() {
        Licence called = Licence.NONE.practised().called(board, DAY);

        assertTrue(QualificationExam.isCalledNow(called, at(DAY, 2000L)));
        assertTrue(QualificationExam.isCalledNow(called, at(DAY, QualificationExam.EXAM_UNTIL - 1)));
        assertTrue(QualificationExam.missedCall(called, at(DAY, QualificationExam.EXAM_UNTIL)));
        assertTrue(QualificationExam.missedCall(called, at(DAY + 1, 2000L)));
        assertFalse(QualificationExam.missedCall(Licence.NONE, at(DAY + 1, 2000L)), "never called");
    }

    /** Out the morning after; seen at the board until noon, and heard wherever it is after. */
    @Test
    void resultsAreOutTheMorningAfter() {
        Licence sat = Licence.NONE.practised().called(board, DAY).sat(true);

        assertFalse(QualificationExam.resultsOut(sat, at(DAY, 10000L)), "on the day");
        assertTrue(QualificationExam.goesToSeeResults(sat, at(DAY + 1, 2000L)));
        assertTrue(QualificationExam.mustHearResults(sat, at(DAY + 1, QualificationExam.RESULTS_UNTIL)));
        assertTrue(QualificationExam.mustHearResults(sat, at(DAY + 2, 2000L)), "missed the morning altogether");
    }

    /** The design's own example: Chiikawa for grade 5, five slips practised, one exam failed, no book. */
    @Test
    void practiceFailingAndTheBookAddUpAndThePetsLeaningScalesThem() {
        Personality.Leaning chiikawa = new Personality.Leaning(0.7F, 0.0F, PetReaction.CONFUSED);
        Licence licence = new Licence(0, 5, 1, false, ExamStage.NONE);

        assertEquals((0.40F + 0.15F + 0.05F) * 0.7F, QualificationExam.passChance(weeding, licence, chiikawa), EPSILON);
        assertEquals((0.40F + 0.15F + 0.05F + 0.25F) * 0.7F,
            QualificationExam.passChance(weeding, licence.withBookRead(), chiikawa), EPSILON);
    }

    @Test
    void practiceAndFailingOnlyAddSoMuchAndNobodyIsSure() {
        Personality.Leaning plain = Personality.Leaning.DEFAULT;
        Licence worn = new Licence(4, 100, 100, false, ExamStage.NONE);

        // Grade 1: 8% plus the most practice and failing add.
        assertEquals(0.08F + 0.30F + 0.20F, QualificationExam.passChance(weeding, worn, plain), EPSILON);
        assertEquals(0.95F, QualificationExam.passChance(weeding, worn.withBookRead(),
            new Personality.Leaning(3.0F, 0.0F, PetReaction.CONFUSED)), EPSILON);
    }

    /** A pet that gets more from a book gets it on top of what the book gives anyone. */
    @Test
    void aBookworkGetsMoreFromTheBook() {
        Personality.Leaning bookworm = new Personality.Leaning(1.0F, 0.1F, PetReaction.CONFUSED);

        assertEquals(0.40F + 0.25F + 0.1F,
            QualificationExam.passChance(weeding, Licence.NONE.withBookRead(), bookworm), EPSILON);
    }

    @Test
    void aWildPetsGradeIsDrawnByTheWeightsScaledByItsAptitude() {
        Personality.Leaning none = new Personality.Leaning(0.0F, 0.0F, PetReaction.CONFUSED);
        Personality.Leaning plain = Personality.Leaning.DEFAULT;

        assertEquals(0, QualificationExam.wildGrade(weeding, none, FixedRandom.floats(0.99F)),
            "a pet with no aptitude never turns up holding a grade");
        // Weights 70, 18, 8, 3, 1, 0 of 100.
        assertEquals(0, QualificationExam.wildGrade(weeding, plain, FixedRandom.floats(0.69F)));
        assertEquals(1, QualificationExam.wildGrade(weeding, plain, FixedRandom.floats(0.71F)));
        assertEquals(4, QualificationExam.wildGrade(weeding, plain, FixedRandom.floats(0.995F)));
    }

    private static long at(long day, long timeOfDay) {
        return day * Level.TICKS_PER_DAY + timeOfDay;
    }
}
