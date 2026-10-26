package com.dwinovo.chiikawa.entity.brain.errand.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.combat.Arms;
import com.dwinovo.chiikawa.entity.brain.combat.Ground;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.testing.TestAnchors;
import com.dwinovo.chiikawa.testing.TestErrands;
import com.dwinovo.chiikawa.testing.TestView;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** When a pet runs: hurt, and not to answer it. */
class FleeOfferTest {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
        ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("dimension")),
        ResourceLocation.withDefaultNamespace("overworld"));
    private static final GlobalPos HOME = GlobalPos.of(OVERWORLD, new BlockPos(0, 64, 0));
    private static final UUID ATTACKER = UUID.fromString("00000000-0000-0000-0000-0000000000a4");
    private static final Arms WIELDING = new Arms(true, Optional.empty());
    private static final Arms SPARE_SWORD = new Arms(false, Optional.of(new Arms.Weapon(2, false)));

    @BeforeAll
    static void shippedSettings() {
        TestErrands.load();
    }

    /** A pet by its home that something has hurt, with the orders of a pet that is told nothing. */
    private static TestView hurt() {
        return TestView.at(HOME, TestAnchors.free(HOME)).attackedBy(ATTACKER, east(4));
    }

    private static GlobalPos east(int blocks) {
        return GlobalPos.of(OVERWORLD, HOME.pos().east(blocks));
    }

    @Test
    void aPetNothingHasHurtDoesNotRun() {
        TestView unhurt = TestView.at(HOME, TestAnchors.free(HOME));

        assertEquals("intent.chiikawa.fail.no_attacker", TestErrands.whyNoOffer(ErrandIds.FLEE, unhurt).reasonKey());
    }

    @Test
    void aPetWithNothingToFightWithRuns() {
        assertEquals(new FleeErrand.Flight(Optional.empty()), TestErrands.planOf(ErrandIds.FLEE, hurt()));
    }

    @Test
    void aPetThatHasAWeaponAndMayUseItFightsBackAndDoesNotRun() {
        for (Arms arms : new Arms[] {WIELDING, SPARE_SWORD}) {
            assertEquals("intent.chiikawa.fail.fights_back", TestErrands.whyNoOffer(ErrandIds.FLEE, hurt().arms(arms)).reasonKey(),
                arms::toString);
        }
    }

    @Test
    void aPetToldNotToFightRunsWhateverItHolds() {
        TestView told = hurt().arms(WIELDING).fightingOn();

        assertEquals(new FleeErrand.Flight(Optional.empty()), TestErrands.planOf(ErrandIds.FLEE, told));
        assertEquals(new FleeErrand.Flight(Optional.empty()),
            TestErrands.planOf(ErrandIds.FLEE, hurt().arms(SPARE_SWORD).fightingOn(Ground.OWNER)),
            "answering what is about its owner is not answering what hurts it");
    }

    @Test
    void aPetToldToSitRunsAndKnowsWhereItSatToComeBackToIt() {
        TestView sitting = TestView.at(HOME, TestAnchors.sitting(HOME)).attackedBy(ATTACKER, east(4))
            .arms(WIELDING).fightingOn();

        assertEquals(new FleeErrand.Flight(Optional.of(HOME.pos())), TestErrands.planOf(ErrandIds.FLEE, sitting));
    }

    @Test
    void theErrandHasNoPlaceToGoToSoThePetItselfIsWhatTheLeashHolds() {
        TestView view = hurt();

        assertEquals(Optional.empty(), TestErrands.planOf(ErrandIds.FLEE, view).destination(view));
    }

    @Test
    void aPetKeepsHidingUntilItCouldFightBackWhetherOrNotSomethingHurtItJustNow() {
        FleeErrand.Flight flight = new FleeErrand.Flight(Optional.empty());

        assertTrue(TestErrands.stillWanted(ErrandIds.FLEE, flight, TestView.at(HOME, TestAnchors.free(HOME))).ok());
        assertFalse(TestErrands.stillWanted(ErrandIds.FLEE, flight, hurt().arms(WIELDING)).ok());
    }

    @Test
    void itIsTheMostPressingThingAPetDoesAndTheOnlyOneNoOrderForbids() {
        ErrandSettings<?> settings = ErrandSettings.of(ErrandIds.FLEE).orElseThrow();

        assertEquals(100, settings.priority());
        assertEquals(ErrandCategory.SAFETY, TestErrands.errand(ErrandIds.FLEE).category());
        assertInstanceOf(ErrandTunings.Flee.class, settings.tuning());
    }
}
