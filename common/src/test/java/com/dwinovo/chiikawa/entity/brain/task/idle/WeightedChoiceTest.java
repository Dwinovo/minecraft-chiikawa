package com.dwinovo.chiikawa.entity.brain.task.idle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import org.junit.jupiter.api.Test;

class WeightedChoiceTest {
    private static final int PICKS = 2_000;

    @Test
    void somethingWeighedAtNothingIsNeverPicked() {
        Step never = new Step(true, 1);
        Step always = new Step(true, 1);
        WeightedChoice<LivingEntity> choice = choice(option(never, 0), option(always, 1));

        for (int pick = 0; pick < PICKS; pick++) {
            run(choice);
        }
        assertEquals(0, never.started);
        assertEquals(PICKS, always.started);
    }

    @Test
    void picksByWeight() {
        Step often = new Step(true, 1);
        Step seldom = new Step(true, 1);
        WeightedChoice<LivingEntity> choice = choice(option(often, 3), option(seldom, 1));

        for (int pick = 0; pick < PICKS; pick++) {
            run(choice);
        }
        assertEquals(0.75, often.started / (double) PICKS, 0.05);
    }

    /** Nobody about to look at, say: the pet does something else rather than nothing. */
    @Test
    void whatCannotStartGivesWayToWhatCan() {
        Step cannot = new Step(false, 1);
        Step can = new Step(true, 1);
        WeightedChoice<LivingEntity> choice = choice(option(cannot, 1_000), option(can, 1));

        assertTrue(choice.tryStart(null, null, 0L));
        assertEquals(1, can.started);
    }

    /** The weights are asked for at each pick, so data loaded since the pet's brain was made counts. */
    @Test
    void weightsAreAskedForAtEachPick() {
        AtomicInteger weight = new AtomicInteger(0);
        Step step = new Step(true, 1);
        WeightedChoice<LivingEntity> choice = choice(new WeightedChoice.Option<>(step, entity -> weight.get()));

        assertFalse(choice.tryStart(null, null, 0L));
        weight.set(1);
        assertTrue(choice.tryStart(null, null, 0L));
    }

    /** One thing at a time, for as long as it lasts, then free to pick again. */
    @Test
    void keepsAtOneThingUntilItIsDone() {
        Step step = new Step(true, 3);
        WeightedChoice<LivingEntity> choice = choice(option(step, 1));

        assertTrue(choice.tryStart(null, null, 0L));
        for (int tick = 0; tick < 2; tick++) {
            choice.tickOrStop(null, null, tick);
            assertEquals(Behavior.Status.RUNNING, choice.getStatus());
        }
        choice.tickOrStop(null, null, 2L);
        assertEquals(Behavior.Status.STOPPED, choice.getStatus());
        assertEquals(1, step.started);
    }

    @Test
    void stoppingStopsWhatIsRunning() {
        Step step = new Step(true, 100);
        WeightedChoice<LivingEntity> choice = choice(option(step, 1));

        choice.tryStart(null, null, 0L);
        choice.doStop(null, null, 1L);

        assertEquals(Behavior.Status.STOPPED, step.getStatus());
        assertEquals(Behavior.Status.STOPPED, choice.getStatus());
    }

    private static void run(WeightedChoice<LivingEntity> choice) {
        assertTrue(choice.tryStart(null, null, 0L));
        while (choice.getStatus() == Behavior.Status.RUNNING) {
            choice.tickOrStop(null, null, 0L);
        }
    }

    @SafeVarargs
    private static WeightedChoice<LivingEntity> choice(WeightedChoice.Option<LivingEntity>... options) {
        return new WeightedChoice<>(List.of(options));
    }

    private static WeightedChoice.Option<LivingEntity> option(Step step, int weight) {
        return new WeightedChoice.Option<>(step, entity -> weight);
    }

    /** A behaviour that starts or not, and then lasts a number of ticks. */
    private static final class Step implements BehaviorControl<LivingEntity> {
        private final boolean startable;
        private final int ticks;
        private Behavior.Status status = Behavior.Status.STOPPED;
        private int left;
        private int started;

        Step(boolean startable, int ticks) {
            this.startable = startable;
            this.ticks = ticks;
        }

        @Override
        public Behavior.Status getStatus() {
            return status;
        }

        @Override
        public boolean tryStart(ServerLevel level, LivingEntity entity, long gameTime) {
            if (!startable) {
                return false;
            }
            status = Behavior.Status.RUNNING;
            left = ticks;
            started++;
            return true;
        }

        @Override
        public void tickOrStop(ServerLevel level, LivingEntity entity, long gameTime) {
            if (--left <= 0) {
                doStop(level, entity, gameTime);
            }
        }

        @Override
        public void doStop(ServerLevel level, LivingEntity entity, long gameTime) {
            status = Behavior.Status.STOPPED;
        }

        @Override
        public String debugString() {
            return "step";
        }
    }
}
