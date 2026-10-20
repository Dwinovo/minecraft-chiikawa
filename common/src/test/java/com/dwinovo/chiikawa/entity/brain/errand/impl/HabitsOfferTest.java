package com.dwinovo.chiikawa.entity.brain.errand.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.Offer;
import com.dwinovo.chiikawa.entity.brain.errand.view.Onlooker;
import com.dwinovo.chiikawa.entity.brain.personality.IdleHabits;
import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import com.dwinovo.chiikawa.testing.TestAnchors;
import com.dwinovo.chiikawa.testing.TestErrands;
import com.dwinovo.chiikawa.testing.TestView;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.InclusiveRange;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** What a pet with nothing to do offers to do with itself, wandering or sitting: its habits, by the weights its personality gives them. */
class HabitsOfferTest {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
        ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("dimension")),
        ResourceLocation.withDefaultNamespace("overworld"));
    private static final GlobalPos HOME = GlobalPos.of(OVERWORLD, new BlockPos(0, 64, 0));
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID SHEEP = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final InclusiveRange<Integer> A_WHILE = new InclusiveRange<>(30, 60);

    @BeforeAll
    static void shippedSettings() {
        TestErrands.load();
    }

    @Test
    void aWanderingPetWithNobodyNearStrollsOrPausesByTheWeightsOfItsPersonality() {
        TestView view = TestView.at(HOME, TestAnchors.free(HOME));

        List<? extends Offer<?>> offers = TestErrands.offers(ErrandIds.WANDER, view);

        assertEquals(List.of(new Offer<>(new Habits.Strolling(), IdleHabits.DEFAULT.stroll()),
            new Offer<>(new Habits.Resting(), IdleHabits.DEFAULT.rest().weight())), offers);
    }

    @Test
    void whoIsNearIsOfferedToLookAtInTheOrderPlayerCreatureStrollPause() {
        TestView view = TestView.at(HOME, TestAnchors.free(HOME)).onlooker(Onlooker.CREATURE, SHEEP).onlooker(Onlooker.PLAYER, PLAYER);

        List<? extends Offer<?>> offers = TestErrands.offers(ErrandIds.WANDER, view);

        assertEquals(List.of(new Offer<>(new Habits.Looking(Onlooker.PLAYER, PLAYER), IdleHabits.DEFAULT.lookAtPlayer().weight()),
            new Offer<>(new Habits.Looking(Onlooker.CREATURE, SHEEP), IdleHabits.DEFAULT.lookAtCreature().weight()),
            new Offer<>(new Habits.Strolling(), IdleHabits.DEFAULT.stroll()),
            new Offer<>(new Habits.Resting(), IdleHabits.DEFAULT.rest().weight())), offers);
    }

    @Test
    void aSittingPetHasTheSameHabitsExceptTheOneThatMovesIt() {
        TestView view = TestView.at(HOME, stayingPut()).onlooker(Onlooker.PLAYER, PLAYER);

        assertEquals(List.of(new Offer<>(new Habits.Looking(Onlooker.PLAYER, PLAYER), IdleHabits.DEFAULT.lookAtPlayer().weight()),
            new Offer<>(new Habits.Resting(), IdleHabits.DEFAULT.rest().weight())), TestErrands.offers(ErrandIds.STAY, view));
    }

    @Test
    void aHabitWithAWeightOfNoneIsNeverOffered() {
        IdleHabits homebody = new IdleHabits(new IdleHabits.Glance(0, 5.0F, A_WHILE), new IdleHabits.Glance(0, 5.0F, A_WHILE), 0,
            new IdleHabits.Pause(3, A_WHILE));
        TestView view = TestView.at(HOME, TestAnchors.free(HOME)).personality(withHabits(homebody))
            .onlooker(Onlooker.PLAYER, PLAYER);

        assertEquals(List.of(new Offer<>(new Habits.Resting(), 3)), TestErrands.offers(ErrandIds.WANDER, view));
    }

    @Test
    void aPetWithNoHabitToPracticeSaysSo() {
        IdleHabits none = new IdleHabits(new IdleHabits.Glance(0, 5.0F, A_WHILE), new IdleHabits.Glance(0, 5.0F, A_WHILE), 0,
            new IdleHabits.Pause(0, A_WHILE));
        TestView view = TestView.at(HOME, TestAnchors.free(HOME)).personality(withHabits(none));

        assertTrue(TestErrands.offers(ErrandIds.WANDER, view).isEmpty());
        assertEquals("intent.chiikawa.fail.nothing_to_do", TestErrands.whyNoOffer(ErrandIds.WANDER, view).reasonKey());
        assertEquals("intent.chiikawa.fail.nothing_to_do", TestErrands.whyNoOffer(ErrandIds.STAY, view).reasonKey());
    }

    @Test
    void pastTheLeashTheOnlyOfferIsTheWayBackToTheMiddleOfTheArea() {
        PetAnchor free = TestAnchors.free(HOME);
        GlobalPos strayed = GlobalPos.of(OVERWORLD, HOME.pos().east((int) free.leash() + 1));
        TestView view = TestView.at(strayed, free).onlooker(Onlooker.PLAYER, PLAYER);

        List<? extends Offer<?>> offers = TestErrands.offers(ErrandIds.WANDER, view);

        assertEquals(List.of(new Offer<>(new Habits.Returning(HOME), 1)), offers);
        assertEquals(java.util.Optional.of(HOME), offers.get(0).plan().destination(view));
    }

    @Test
    void justInsideTheLeashItPottersAboutAsUsual() {
        PetAnchor free = TestAnchors.free(HOME);
        GlobalPos near = GlobalPos.of(OVERWORLD, HOME.pos().east((int) free.leash() - 1));

        assertEquals(2, TestErrands.offers(ErrandIds.WANDER, TestView.at(near, free)).size());
    }

    @Test
    void aPetThatCannotMoveIsNeverPulledBackWhereverItIs() {
        GlobalPos far = GlobalPos.of(OVERWORLD, HOME.pos().east(200));
        PetAnchor sitting = new PetAnchor(HOME, 0.0, 5.0, false, false);

        List<? extends Offer<?>> offers = TestErrands.offers(ErrandIds.WANDER, TestView.at(far, sitting));

        assertTrue(offers.stream().noneMatch(offer -> offer.plan() instanceof Habits.Returning));
    }

    private static PetAnchor stayingPut() {
        return new PetAnchor(HOME, 0.0, PetAnchor.UNLEASHED, false, false);
    }

    private static Personality withHabits(IdleHabits habits) {
        return new Personality(Map.of(), Map.of(), 0.0F, List.of(), List.of(), habits, Map.of());
    }
}
