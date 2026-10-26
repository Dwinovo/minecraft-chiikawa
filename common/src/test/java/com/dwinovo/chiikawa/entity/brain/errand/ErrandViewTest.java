package com.dwinovo.chiikawa.entity.brain.errand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.entity.brain.errand.decide.DecisionLog;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.InvitationDesk;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestLedger;
import com.dwinovo.chiikawa.entity.brain.errand.run.Executor;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.PetIcon;
import com.dwinovo.chiikawa.entity.brain.errand.run.PetIcons;
import com.dwinovo.chiikawa.entity.brain.errand.run.Projection;
import com.dwinovo.chiikawa.entity.brain.errand.run.Routine;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepTag;
import com.dwinovo.chiikawa.testing.FakeHandle;
import com.dwinovo.chiikawa.testing.StubErrand;
import com.dwinovo.chiikawa.testing.TestRests;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

/** What the rest of the game is told of a pet's errand. */
class ErrandViewTest {
    private static final ResourceLocation EXAM = ResourceLocation.fromNamespaceAndPath("chiikawa", "take_exam");
    private static final ResourceLocation SCENE = ResourceLocation.fromNamespaceAndPath("chiikawa", "share_a_snack");
    private static final UUID FRIEND = UUID.fromString("00000000-0000-0000-0000-0000000000f1");

    private final InvitationDesk desk = new InvitationDesk();
    private final Executor executor = new Executor(
        new FakeHandle(),
        new Projection() {
            @Override
            public void intent(@Nullable ResourceLocation errand) {
            }

            @Override
            public void activity(PetActivity activity) {
            }

            @Override
            public void performance(String performance) {
            }

            @Override
            public void icon(PetIcon icon) {
            }
        },
        new RestLedger(errand -> new TestRests.Store(), errand -> Optional.empty()), new DecisionLog(), () -> PetIcons.Signs.NONE);
    private final ErrandView view = ErrandView.of(executor, desk);

    @Test
    void aPetOnNoErrandIsOnNone() {
        assertEquals(Optional.empty(), view.errand());
        assertEquals(OptionalInt.empty(), view.priority());
        assertFalse(view.isOn(EXAM));
        assertFalse(view.seated());
    }

    @Test
    void itSaysWhichErrandHowMuchItMattersAndWhatTheStepMakesOfThePet() {
        executor.start(new StubErrand(EXAM), new Plan() {
        }, TestRests.settings(100, TestRests.ticks(0, 10, 0)), Routine.single(new Seated()), 0);

        assertEquals(Optional.of(EXAM), view.errand());
        assertEquals(OptionalInt.of(50), view.priority());
        assertTrue(view.isOn(EXAM));
        assertTrue(view.seated());
        assertFalse(view.inScene());
        assertFalse(view.playingMusic());
    }

    @Test
    void itSaysWhoThePetHasAgreedToPlayWith() {
        assertEquals(Optional.empty(), view.engagedWith(10));

        desk.consider(new Message.Invitation(FRIEND, SCENE, 0, 30, 60), true, false, OptionalInt.empty(), 10);

        assertEquals(Optional.of(FRIEND), view.engagedWith(10));
        assertEquals(Optional.empty(), view.engagedWith(60), "until the friend stops waiting");
    }

    /** A step that sits the pet down and stays so. */
    private static final class Seated implements Step {
        @Override
        public void begin(Handle handle) {
        }

        @Override
        public StepStatus tick(Handle handle) {
            return StepStatus.RUNNING;
        }

        @Override
        public Set<StepTag> tags() {
            return Set.of(StepTag.SEATED);
        }
    }
}
