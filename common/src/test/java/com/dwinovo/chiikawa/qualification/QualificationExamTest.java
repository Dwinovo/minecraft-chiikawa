package com.dwinovo.chiikawa.qualification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import com.dwinovo.chiikawa.testing.FixedRandom;
import java.util.List;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.InclusiveRange;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class QualificationExamTest {
    private static final float EPSILON = 1.0E-5F;
    private static Qualification weeding;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        weeding = new Qualification(5, 7, ResourceLocation.fromNamespaceAndPath("chiikawa", "weeding"), 1,
            List.of(0.40F, 0.30F, 0.22F, 0.15F, 0.08F), 0.03F, 0.30F, 0.05F, 0.20F, Items.BOOK, 0.25F, 0.95F,
            new InclusiveRange<>(480, 560), List.of(70, 18, 8, 3, 1, 0));
    }

    /** Every seventh day, the same days everywhere; never on the first day of the world. */
    @Test
    void theExamIsEverySeventhDay() {
        assertFalse(QualificationExam.isExamDay(weeding, 0));
        assertTrue(QualificationExam.isExamDay(weeding, 6));
        assertFalse(QualificationExam.isExamDay(weeding, 7));
        assertTrue(QualificationExam.isExamDay(weeding, 13));
    }

    @Test
    void aPetSitsOnlyWithAGradeLeftPracticeDoneAndNoResultOutstanding() {
        assertFalse(QualificationExam.maySit(weeding, Licence.NONE), "has not practised");
        assertTrue(QualificationExam.maySit(weeding, Licence.NONE.practised()));
        assertFalse(QualificationExam.maySit(weeding, Licence.holding(5).practised()), "holds every grade");
        assertFalse(QualificationExam.maySit(weeding, Licence.NONE.practised().sat(true)), "still waiting for results");
    }

    /** Not every pet wants to go every time; one that has read the book always does. */
    @Test
    void wantingToGoIsUpToThePetUnlessItHasReadTheBook() {
        Personality.Leaning keen = new Personality.Leaning(0.6F, 1.0F, 0.0F);

        assertTrue(QualificationExam.wantsToSit(keen, Licence.NONE, FixedRandom.floats(0.59F)));
        assertFalse(QualificationExam.wantsToSit(keen, Licence.NONE, FixedRandom.floats(0.61F)));
        assertTrue(QualificationExam.wantsToSit(new Personality.Leaning(0.0F, 1.0F, 0.0F),
            Licence.NONE.withBookRead(), FixedRandom.floats(0.99F)));
    }

    /** The design's own example: Chiikawa for grade 5, five slips practised, one exam failed, no book. */
    @Test
    void practiceFailingAndTheBookAddUpAndThePetsLeaningScalesThem() {
        Personality.Leaning chiikawa = new Personality.Leaning(0.6F, 0.7F, 0.0F);
        Licence licence = new Licence(0, 5, 1, false, Optional.empty(), -1L, false);

        assertEquals((0.40F + 0.15F + 0.05F) * 0.7F, QualificationExam.passChance(weeding, licence, chiikawa), EPSILON);
        assertEquals((0.40F + 0.15F + 0.05F + 0.25F) * 0.7F,
            QualificationExam.passChance(weeding, licence.withBookRead(), chiikawa), EPSILON);
    }

    @Test
    void practiceAndFailingOnlyAddSoMuchAndNobodyIsSure() {
        Personality.Leaning plain = Personality.Leaning.DEFAULT;
        Licence worn = new Licence(4, 100, 100, false, Optional.empty(), -1L, false);

        // Grade 1: 8% plus the most practice and failing add.
        assertEquals(0.08F + 0.30F + 0.20F, QualificationExam.passChance(weeding, worn, plain), EPSILON);
        assertEquals(0.95F, QualificationExam.passChance(weeding, worn.withBookRead(),
            new Personality.Leaning(0.6F, 3.0F, 0.0F)), EPSILON);
    }

    /** A pet that gets more from a book gets it on top of what the book gives anyone. */
    @Test
    void aBookworkGetsMoreFromTheBook() {
        Personality.Leaning bookworm = new Personality.Leaning(0.6F, 1.0F, 0.1F);

        assertEquals(0.40F + 0.25F + 0.1F,
            QualificationExam.passChance(weeding, Licence.NONE.withBookRead(), bookworm), EPSILON);
    }

    @Test
    void aWildPetsGradeIsDrawnByTheWeightsScaledByItsAptitude() {
        Personality.Leaning none = new Personality.Leaning(0.6F, 0.0F, 0.0F);
        Personality.Leaning plain = Personality.Leaning.DEFAULT;

        assertEquals(0, QualificationExam.wildGrade(weeding, none, FixedRandom.floats(0.99F)),
            "a pet with no aptitude never turns up holding a grade");
        // Weights 70, 18, 8, 3, 1, 0 of 100.
        assertEquals(0, QualificationExam.wildGrade(weeding, plain, FixedRandom.floats(0.69F)));
        assertEquals(1, QualificationExam.wildGrade(weeding, plain, FixedRandom.floats(0.71F)));
        assertEquals(4, QualificationExam.wildGrade(weeding, plain, FixedRandom.floats(0.995F)));
    }
}
