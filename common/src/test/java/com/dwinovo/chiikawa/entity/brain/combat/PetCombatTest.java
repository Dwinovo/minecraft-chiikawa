package com.dwinovo.chiikawa.entity.brain.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.combat.PetCombat.Band;
import com.dwinovo.chiikawa.entity.brain.combat.PetCombat.Foe;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class PetCombatTest {
    private static final double MELEE_REACH = 2.5;

    private static Foe plain(int id, double distance) {
        return new Foe(id, distance, false, false);
    }

    private static Foe creeper(int id, double distance, boolean lit) {
        return new Foe(id, distance, true, lit);
    }

    // ---- where to stand --------------------------------------------------------

    @Test
    void aSwordFightIsFoughtFromInsideReach() {
        Band band = PetCombat.band(false, false, plain(1, 1.0), MELEE_REACH);

        assertFalse(band.tooClose(0.5), "a pet with a sword has no reason to keep its distance");
        assertTrue(band.tooFar(MELEE_REACH + 0.1));
        assertFalse(band.tooFar(MELEE_REACH - 0.1));
    }

    @Test
    void aBowIsFoughtFromABandWithRoomInIt() {
        Band band = PetCombat.band(false, true, plain(1, 1.0), MELEE_REACH);

        assertTrue(band.tooClose(PetCombat.BOW_NEAR - 0.1), "an archer let something walk into its face");
        assertFalse(band.tooClose(PetCombat.BOW_NEAR + 0.1));
        assertTrue(band.tooFar(PetCombat.BOW_FAR + 0.1));
        // The band has room in it, so the pet is not on a line it can shuffle across.
        assertTrue(band.far() - band.near() > 4.0, "the band a bow is fought from is too tight to hold");
    }

    @Test
    void aLitCreeperIsBackedAwayFromWhicheverWeaponIsInHand() {
        assertTrue(PetCombat.band(false, false, creeper(1, 1.0, true), MELEE_REACH).tooClose(PetCombat.BLAST_SPAN - 0.1),
            "a pet stood inside a lit creeper's blast with a sword out");
        assertTrue(PetCombat.band(false, true, creeper(1, 1.0, true), MELEE_REACH).tooClose(PetCombat.BLAST_SPAN - 0.1),
            "an archer stood inside a lit creeper's blast");
    }

    @Test
    void anUnlitCreeperIsKeptOutsideTheDistanceItWouldStartSwellingAt() {
        Band band = PetCombat.band(false, false, creeper(1, 1.0, false), MELEE_REACH);

        assertTrue(band.tooClose(PetCombat.FUSE_RADIUS - 0.1),
            "a pet with a sword walked up to a creeper, which is how a pet stops being a pet");
        // And there is no far edge to walk back in from: a sword has no answer to a creeper.
        assertFalse(band.tooFar(30.0));
    }

    @Test
    void breakingOffMeansGettingRightAway() {
        Band band = PetCombat.band(true, false, plain(1, 1.0), MELEE_REACH);

        assertTrue(band.tooClose(PetCombat.BREAK_OFF_DISTANCE - 1.0));
        assertFalse(band.tooFar(1000.0), "a pet that broke off was told to come back for more");
    }

    // ---- when to break off -----------------------------------------------------

    @Test
    void aPetBreaksOffHurtAndComesBackHealed() {
        assertFalse(PetCombat.breakingOff(1.0F, false));
        assertTrue(PetCombat.breakingOff(PetCombat.BREAK_OFF_HEALTH, false));
        // Having broken off, it takes more than the line it broke at to go back in.
        assertTrue(PetCombat.breakingOff(PetCombat.BREAK_OFF_HEALTH + 0.1F, true));
        assertFalse(PetCombat.breakingOff(PetCombat.RALLY_HEALTH, true));
    }

    @Test
    void theTwoHealthLinesAreFarEnoughApartToBeTwoLines() {
        assertTrue(PetCombat.RALLY_HEALTH - PetCombat.BREAK_OFF_HEALTH > 0.15F,
            "breaking off and rallying are so close together they are one line");
    }

    // ---- who to fight ----------------------------------------------------------

    @Test
    void theNearestIsPickedAndThenKept() {
        List<Foe> foes = List.of(plain(1, 8.0), plain(2, 3.0));

        assertEquals(OptionalInt.of(2), PetCombat.pick(foes, PetCombat.NO_FOE, false));
        // The far one is already being fought: it stays the target even with a nearer one about.
        assertEquals(OptionalInt.of(1), PetCombat.pick(foes, 1, false));
    }

    @Test
    void aKeptTargetThatIsNoLongerWorthFightingIsDropped() {
        List<Foe> foes = List.of(creeper(1, 3.0, true), plain(2, 9.0));

        assertEquals(OptionalInt.of(2), PetCombat.pick(foes, 1, false));
    }

    @Test
    void anythingElseIsPreferredToALitCreeper() {
        List<Foe> both = List.of(creeper(1, 4.0, true), plain(2, 9.0));

        assertEquals(OptionalInt.of(2), PetCombat.pick(both, PetCombat.NO_FOE, false),
            "a pet with a sword picked a fight with a lit creeper");
        assertEquals(OptionalInt.of(1), PetCombat.pick(both, PetCombat.NO_FOE, true),
            "a pet with a bow left the lit creeper to walk up to it");
    }

    @Test
    void aLoneLitCreeperIsStillTheTargetSoTheresSomethingToBackAwayFrom() {
        List<Foe> lit = List.of(creeper(1, 4.0, true));

        assertEquals(OptionalInt.of(1), PetCombat.pick(lit, PetCombat.NO_FOE, false));
        assertEquals(OptionalInt.of(1), PetCombat.pick(lit, PetCombat.NO_FOE, true));
    }

    @Test
    void nothingToFightIsAnAnswerToo() {
        assertEquals(OptionalInt.empty(), PetCombat.pick(List.of(), PetCombat.NO_FOE, true));
    }

    // ---- how often to swing ----------------------------------------------------

    @Test
    void aWeaponSetsThePaceWithinReason() {
        // An iron sword is 1.6 swings a second in vanilla hands.
        assertEquals(13, PetCombat.swingCooldown(1.6));
        // A pet whose attribute says nothing useful keeps the old once-a-second pace.
        assertEquals(PetCombat.SLOWEST_SWING, PetCombat.swingCooldown(1.0));
        assertEquals(PetCombat.SLOWEST_SWING, PetCombat.swingCooldown(0.0));
        // And nothing swings faster than the floor, whatever a modded weapon claims.
        assertEquals(PetCombat.FASTEST_SWING, PetCombat.swingCooldown(20.0));
    }
}
