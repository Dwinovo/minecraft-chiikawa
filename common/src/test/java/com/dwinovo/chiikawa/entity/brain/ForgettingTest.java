package com.dwinovo.chiikawa.entity.brain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dwinovo.chiikawa.data.WorkData;
import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandType;
import com.dwinovo.chiikawa.entity.brain.errand.Offers;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.decide.Permits;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestStore;
import com.dwinovo.chiikawa.entity.brain.errand.run.Routine;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.entity.brain.errand.work.Work;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkAction;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkErrand;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkTarget;
import com.dwinovo.chiikawa.testing.StubErrand;
import com.dwinovo.chiikawa.testing.TestErrands;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * What a pet forgets of its progress: a code errand's memories while it is not offered, an action's while no errand
 * made with it is, and the action's whatever errands happen to be loaded.
 */
class ForgettingTest {
    private static MemoryModuleType<Integer> OF_THE_ACTION;
    private static MemoryModuleType<Integer> OF_THE_ERRAND;
    private static final Permits ALL = category -> true;
    private static final Permits NO_WORK = category -> category != ErrandCategory.WORK;
    private static final ResourceLocation ACTION = ResourceLocation.fromNamespaceAndPath("chiikawa", "stub");
    private static final StubAction STUB = new StubAction();
    /** An errand of the stub action, under an id the shipped settings have. */
    private static final WorkErrand<Plan, String> WORKING = new WorkErrand<>(WorkData.PLAY_MUSIC,
        new Work<>(STUB, ErrandCategory.WORK, Optional.empty(), List.of(), Optional.empty()));
    private static StubErrand CODE;

    /** The memory types need the game's registries, which the shipped settings' load brings up. */
    @BeforeAll
    static void shippedSettings() {
        TestErrands.load();
        OF_THE_ACTION = new MemoryModuleType<>(Optional.empty());
        OF_THE_ERRAND = new MemoryModuleType<>(Optional.empty());
        CODE = new StubErrand(ErrandIds.WANDER, Check.OK, Set.of(OF_THE_ERRAND));
    }

    @Test
    void anActionIsForgottenWhenNoOfferedErrandUsesIt() {
        assertEquals(Set.of(OF_THE_ACTION), forgotten(List.of(), List.of(CODE)));
    }

    @Test
    void anActionIsKeptWhileAnOfferedErrandUsesItWhetherOrNotThatErrandIsAmongThoseLoaded() {
        // The work is offered, but no file made it: the registry of errands has nothing of the action.
        assertEquals(Set.of(), forgotten(List.of(), List.of(CODE, WORKING)));
        assertEquals(Set.of(), forgotten(List.of(CODE, WORKING), List.of(CODE, WORKING)));
    }

    @Test
    void anActionIsForgottenWhileTheDirectiveForbidsItsErrand() {
        assertEquals(Set.of(OF_THE_ACTION),
            Forgetting.of(List.of(), List.of(STUB), List.of(CODE, WORKING), NO_WORK));
    }

    @Test
    void aCodeErrandIsForgottenWhenItIsNotOffered() {
        assertEquals(Set.of(OF_THE_ERRAND, OF_THE_ACTION), forgotten(List.of(CODE), List.of()));
        assertEquals(Set.of(OF_THE_ACTION), forgotten(List.of(CODE), List.of(CODE)));
    }

    private static Set<MemoryModuleType<?>> forgotten(List<ErrandType<?, ?>> there, List<ErrandType<?, ?>> offered) {
        return Forgetting.of(there, List.of(STUB), offered, ALL);
    }

    /** An action that does nothing but hold progress. */
    private static final class StubAction implements WorkAction<Plan, String> {
        @Override
        public ResourceLocation id() {
            return ACTION;
        }

        @Override
        public Codec<String> tuningCodec() {
            return Codec.STRING;
        }

        @Override
        public Optional<String> refuses(Optional<WorkTarget> target) {
            return Optional.empty();
        }

        @Override
        public Offers<Plan> offers(Work<Plan, String> work, PetView view, String tuning) {
            return Offers.none(Check.fail("stub"));
        }

        @Override
        public Routine routine(Work<Plan, String> work, Plan plan, ErrandSettings<String> settings) {
            return Routine.of();
        }

        @Override
        public RestStore.Kind restStore() {
            return RestStore.shared();
        }

        @Override
        public Set<MemoryModuleType<?>> forgetWhenUnavailable() {
            return Set.of(OF_THE_ACTION);
        }
    }
}
