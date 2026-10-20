package com.dwinovo.chiikawa.entity.brain.errand.decide;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dwinovo.chiikawa.entity.brain.errand.decide.DecisionTrace.Line;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** The account of one decision the debug command gives. */
class DecisionTraceTest {
    private static final ResourceLocation WORK = ResourceLocation.fromNamespaceAndPath("chiikawa", "work");
    private static final ResourceLocation PLAY = ResourceLocation.fromNamespaceAndPath("chiikawa", "play");

    @Test
    void itHasEveryErrandInTheOrderTheDecisionLookedAtThemAndWhatWasPicked() {
        DecisionTrace trace = new DecisionTrace();
        assertEquals(Optional.empty(), trace.pick());

        trace.excluded(PLAY, 80, "resting");
        trace.startable(new Scored(WORK, 50, 0.6F, List.of(1)));
        trace.picked(new Decision.Pick(WORK, 0));

        assertEquals(List.of(new Line(PLAY, 80, Optional.empty(), "resting"),
            new Line(WORK, 50, Optional.of(0.6F), DecisionTrace.STARTABLE)), trace.lines());
        assertEquals(Optional.of(new Decision.Pick(WORK, 0)), trace.pick());
    }
}
