package com.dwinovo.chiikawa.qualification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.InclusiveRange;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LicenceViewTest {
    private static final Identifier WEEDING = Identifier.fromNamespaceAndPath("chiikawa", "weeding");
    private static final long MORNING = 2000L;
    private static Qualification weeding;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        weeding = new Qualification(5, new ExamFee(Items.DIAMOND, 1), WEEDING, 1, List.of(0.40F, 0.30F, 0.22F, 0.15F, 0.08F), 0.03F, 0.30F,
            0.05F, 0.20F, Items.BOOK, 0.25F, 0.95F, new InclusiveRange<>(480, 560), List.of(70, 18, 8, 3, 1, 0));
    }

    @Test
    void theGradeShownIsTheOneTheSeriesWouldName() {
        LicenceView none = LicenceView.of(WEEDING, weeding, Licence.NONE, at(3, MORNING));
        LicenceView gradeFour = LicenceView.of(WEEDING, weeding, Licence.holding(2), at(3, MORNING));
        LicenceView gradeOne = LicenceView.of(WEEDING, weeding, Licence.holding(5), at(3, MORNING));

        assertEquals(0, none.rank());
        assertEquals(4, gradeFour.rank());
        assertFalse(gradeFour.top());
        assertTrue(gradeOne.top());
        assertEquals(Identifier.withDefaultNamespace("book"), none.book());
    }

    @Test
    void itShowsWhetherThePetHasPractisedIsCalledTodayOrAwaitsResults() {
        GlobalPos board = GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO);
        Licence called = Licence.NONE.practised().called(board, 3L);

        assertFalse(LicenceView.of(WEEDING, weeding, Licence.NONE, at(3, MORNING)).practised());
        assertTrue(LicenceView.of(WEEDING, weeding, Licence.NONE.practised(), at(3, MORNING)).practised());
        assertTrue(LicenceView.of(WEEDING, weeding, called, at(3, MORNING)).called());
        assertFalse(LicenceView.of(WEEDING, weeding, called, at(4, MORNING)).called(), "yesterday's call");
        assertTrue(LicenceView.of(WEEDING, weeding, called.sat(true), at(3, MORNING)).awaitingResults());
    }

    private static long at(long day, long timeOfDay) {
        return day * Level.TICKS_PER_DAY + timeOfDay;
    }
}
