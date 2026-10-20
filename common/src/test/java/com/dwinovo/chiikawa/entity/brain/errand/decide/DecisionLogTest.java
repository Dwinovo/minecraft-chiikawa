package com.dwinovo.chiikawa.entity.brain.errand.decide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.dwinovo.chiikawa.entity.brain.errand.Outcome;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** What the debug command is shown of a pet's last errands. */
class DecisionLogTest {
    private static final ResourceLocation WORK = ResourceLocation.fromNamespaceAndPath("chiikawa", "work");
    private static final ResourceLocation PLAY = ResourceLocation.fromNamespaceAndPath("chiikawa", "play");

    @Test
    void switchesAndEndingsAreKeptInTheOrderTheyHappened() {
        DecisionLog log = new DecisionLog();

        log.switched(10, null, WORK, "NO_ERRAND", List.of(new Scored(WORK, 50, 0.6F, List.of(1))));
        log.ended(40, WORK, Outcome.done(), 30);

        assertEquals(10, log.entries().get(0).gameTime());
        assertEquals(new DecisionLog.Ended(40, WORK, Outcome.done(), 30), log.entries().get(1));
        DecisionLog.Switched switched = (DecisionLog.Switched) log.entries().get(0);
        assertNull(switched.from());
        assertEquals(WORK, switched.to());
    }

    @Test
    void onlyTheBestFewCandidatesOfASwitchAreKept() {
        DecisionLog log = new DecisionLog();
        List<Scored> ranking = List.of(new Scored(WORK, 50, 0.9F, List.of(1)), new Scored(PLAY, 50, 0.8F, List.of(1)),
            new Scored(WORK, 50, 0.7F, List.of(1)), new Scored(PLAY, 50, 0.6F, List.of(1)));

        log.switched(10, null, WORK, "NO_ERRAND", ranking);

        assertEquals(ranking.subList(0, 3), ((DecisionLog.Switched) log.entries().get(0)).top());
    }

    @Test
    void theOldestIsForgottenWhenItIsFull() {
        DecisionLog log = new DecisionLog();

        for (int i = 0; i < DecisionLog.CAPACITY + 5; i++) {
            log.ended(i, WORK, Outcome.done(), 1);
        }

        assertEquals(DecisionLog.CAPACITY, log.entries().size());
        assertEquals(5, log.entries().get(0).gameTime());
        assertEquals(DecisionLog.CAPACITY + 4, log.entries().get(DecisionLog.CAPACITY - 1).gameTime());
    }
}
