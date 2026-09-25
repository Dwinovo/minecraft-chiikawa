package com.dwinovo.chiikawa.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalInt;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.LongStream;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.storage.loot.LootTable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BoardSlipsTest {
    private static final Identifier FARMER = id("farmer");
    private static final Identifier MUSICIAN = id("musician");
    private static final UUID PET = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final long NOON = 6000L;
    private static final long NOW = 100_000L;
    private static final BoardSlot.Claim TAKER = new BoardSlot.Claim("Usagi", "Dwinovo");
    /** Three slips a day, one more a level: the rolls below count on nothing else. */
    private static final BoardLevels LEVELS = new BoardLevels(List.of(
        new BoardLevels.Level(3, 0), new BoardLevels.Level(4, 16), new BoardLevels.Level(5, 32)));

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    // ---- rolling ---------------------------------------------------------------

    @Test
    void sameBoardAndDayRollsTheSameSlipsForEveryone() {
        long seed = BoardSlips.seed(42L, 7L, new BlockPos(10, 64, -3));
        List<BoardSlot> first = BoardSlips.roll(seed, types(), LEVELS, BoardLevels.FIRST_LEVEL);

        assertEquals(LEVELS.slipsAt(BoardLevels.FIRST_LEVEL), first.size());
        assertEquals(first, BoardSlips.roll(seed, types(), LEVELS, BoardLevels.FIRST_LEVEL));
        first.forEach(slot -> assertTrue(slot.openTo(PET, NOW)));
    }

    @Test
    void anotherDayOrBoardRollsOtherwise() {
        BlockPos pos = new BlockPos(10, 64, -3);
        List<BoardSlot> today = BoardSlips.roll(BoardSlips.seed(42L, 7L, pos), types(), LEVELS, BoardLevels.FIRST_LEVEL);

        assertTrue(LongStream.rangeClosed(8L, 20L)
            .anyMatch(day -> !BoardSlips.roll(BoardSlips.seed(42L, day, pos), types(), LEVELS, BoardLevels.FIRST_LEVEL)
                .equals(today)));
        assertNotEquals(BoardSlips.seed(42L, 7L, pos), BoardSlips.seed(42L, 7L, pos.east()));
    }

    /**
     * A board's slips say nothing about its neighbours'. Among a patch of boards, a day
     * that comes up on one board in sixteen — every slip mushroom picking — is missing from
     * the whole patch about as seldom as it would be from boards far apart. While alike
     * seeds went into the game's random numbers unstirred, boards near one another rolled
     * alike, and whole patches went without it four times as often.
     */
    @Test
    void aPatchOfBoardsRollsAsBoardsFarApartWould() {
        Identifier rare = id("mushroom_picking");
        int worlds = 2_000;
        int wide = 7;
        int deep = 13;
        double oneBoard = Math.pow(2.0 / 5.0, LEVELS.slipsAt(BoardLevels.FIRST_LEVEL));
        double expected = worlds * Math.pow(1 - oneBoard, wide * deep);
        // Worlds and places as a game's are: any seed, anywhere.
        Random worldsAndPlaces = new Random(1L);
        int without = 0;
        for (int w = 0; w < worlds; w++) {
            long world = worldsAndPlaces.nextLong();
            BlockPos corner = new BlockPos(worldsAndPlaces.nextInt(20_000) - 10_000, -59,
                worldsAndPlaces.nextInt(20_000) - 10_000);
            boolean found = false;
            for (int x = 0; x < wide && !found; x++) {
                for (int z = 0; z < deep && !found; z++) {
                    found = BoardSlips.roll(BoardSlips.seed(world, 0L, corner.offset(x, 0, z)), types(), LEVELS,
                            BoardLevels.FIRST_LEVEL).stream()
                        .allMatch(slot -> slot.slip().type().equals(rare));
                }
            }
            if (!found) {
                without++;
            }
        }
        assertTrue(without < 2 * expected,
            without + " patches of " + worlds + " never had an all-mushroom day, where about " + Math.round(expected)
                + " would");
    }

    @Test
    void rolledTargetsStayWithinTheTypeAmount() {
        for (long day = 0; day < 50; day++) {
            for (BoardSlot slot : BoardSlips.roll(BoardSlips.seed(1L, day, BlockPos.ZERO), types(), LEVELS,
                    BoardLevels.FIRST_LEVEL)) {
                assertTrue(slot.slip().target() >= 8 && slot.slip().target() <= 16, () -> "target " + slot.slip().target());
                assertEquals(0, slot.slip().progress());
            }
        }
    }

    @Test
    void noLoadedTypesMeansNoSlips() {
        assertEquals(List.of(), BoardSlips.roll(1L, new TreeMap<>(), LEVELS, BoardLevels.FIRST_LEVEL));
    }

    // ---- levels ----------------------------------------------------------------

    @Test
    void aBoardPutsUpAsManySlipsAsItsLevelSays() {
        long seed = BoardSlips.seed(42L, 7L, new BlockPos(10, 64, -3));

        assertEquals(3, BoardSlips.roll(seed, types(), LEVELS, BoardLevels.FIRST_LEVEL).size());
        assertEquals(4, BoardSlips.roll(seed, types(), LEVELS, 2).size());
        assertEquals(5, BoardSlips.roll(seed, types(), LEVELS, LEVELS.top()).size());
        assertEquals(5, BoardSlips.roll(seed, types(), LEVELS, LEVELS.top() + 4).size(),
            "a board saved at a level the pack no longer has goes by the top one");
    }

    @Test
    void upgradingKeepsTheSlipsTheBoardAlreadyHad() {
        long seed = BoardSlips.seed(42L, 7L, new BlockPos(10, 64, -3));
        List<BoardSlot> before = BoardSlips.roll(seed, types(), LEVELS, BoardLevels.FIRST_LEVEL);
        List<BoardSlot> taken = List.of(before.get(0).claim(TAKER), before.get(1), before.get(2));

        List<BoardSlot> after = BoardSlips.topUp(taken, seed, types(), LEVELS, 2);

        assertEquals(4, after.size());
        assertEquals(taken, after.subList(0, 3));
        assertEquals(after, BoardSlips.topUp(taken, seed, types(), LEVELS, 2));
    }

    @Test
    void aTopUpThatIsNotNeededChangesNothing() {
        long seed = BoardSlips.seed(42L, 7L, BlockPos.ZERO);
        List<BoardSlot> slots = BoardSlips.roll(seed, types(), LEVELS, 2);

        assertSame(slots, BoardSlips.topUp(slots, seed, types(), LEVELS, BoardLevels.FIRST_LEVEL));
    }

    @Test
    void workATooLowBoardCannotPutUpStaysOff() {
        SortedMap<Identifier, PetTaskType> types = types();
        types.put(id("melee_hunting"), new PetTaskType(id("fencer"), PetWorkCounters.SLAY, PetTask.NO_ICON,
            UniformInt.of(3, 6), reward("melee_hunting"), 50, 2));

        for (long day = 0; day < 50; day++) {
            long seed = BoardSlips.seed(1L, day, BlockPos.ZERO);
            assertTrue(BoardSlips.roll(seed, types, LEVELS, BoardLevels.FIRST_LEVEL).stream()
                .noneMatch(slot -> slot.slip().counter().equals(PetWorkCounters.SLAY)));
        }
        assertTrue(LongStream.range(0, 50)
            .anyMatch(day -> BoardSlips.roll(BoardSlips.seed(1L, day, BlockPos.ZERO), types, LEVELS, 2).stream()
                .anyMatch(slot -> slot.slip().counter().equals(PetWorkCounters.SLAY))));
    }

    // ---- finding ---------------------------------------------------------------

    @Test
    void findsTheFirstOpenSlipForThePetsCapability() {
        List<BoardSlot> slots = List.of(slot(MUSICIAN), slot(FARMER), slot(FARMER));

        assertEquals(OptionalInt.of(1), BoardSlips.find(slots, FARMER, PET, false, NOON, NOW));
        assertEquals(OptionalInt.of(0), BoardSlips.find(slots, MUSICIAN, PET, false, NOON, NOW));
        assertEquals(OptionalInt.empty(), BoardSlips.find(slots, id("fencer"), PET, false, NOON, NOW));
    }

    @Test
    void skipsTakenSlipsAndSlipsHeldByAnotherPet() {
        List<BoardSlot> slots = List.of(taken(FARMER), slot(FARMER).reserve(OTHER, NOW + 10), slot(FARMER));

        assertEquals(OptionalInt.of(2), BoardSlips.find(slots, FARMER, PET, false, NOON, NOW));
    }

    @Test
    void aLapsedReservationFreesTheSlip() {
        List<BoardSlot> slots = List.of(slot(FARMER).reserve(OTHER, NOW));

        assertEquals(OptionalInt.of(0), BoardSlips.find(slots, FARMER, PET, false, NOON, NOW));
        assertFalse(slots.get(0).reservedBy(OTHER, NOW));
    }

    @Test
    void aPetFindsTheSlipItHoldsFirst() {
        List<BoardSlot> slots = List.of(slot(FARMER), slot(FARMER).reserve(PET, NOW + 10));

        assertEquals(OptionalInt.of(1), BoardSlips.find(slots, FARMER, PET, false, NOON, NOW));
    }

    @Test
    void wildPetsOnlyTakeWhatIsLeftLaterInTheDay() {
        List<BoardSlot> slots = List.of(slot(FARMER));

        assertEquals(OptionalInt.empty(), BoardSlips.find(slots, FARMER, PET, true, BoardSlips.WILD_CLAIM_DELAY - 1, NOW));
        assertEquals(OptionalInt.of(0), BoardSlips.find(slots, FARMER, PET, true, BoardSlips.WILD_CLAIM_DELAY, NOW));
        assertEquals(OptionalInt.of(0), BoardSlips.find(slots, FARMER, PET, false, 0L, NOW));
    }

    @Test
    void aWildPetKeepsTheSlipItAlreadyHolds() {
        List<BoardSlot> slots = List.of(slot(FARMER).reserve(PET, NOW + 10));

        assertEquals(OptionalInt.of(0), BoardSlips.find(slots, FARMER, PET, true, 0L, NOW));
    }

    @Test
    void claimingAndReleasingClearTheReservation() {
        BoardSlot held = slot(FARMER).reserve(PET, NOW + 10);

        assertTrue(held.reservedBy(PET, NOW));
        assertTrue(held.release().openTo(OTHER, NOW));
        assertTrue(held.claim(TAKER).claimed());
        assertFalse(held.claim(TAKER).openTo(PET, NOW));
        assertEquals("Usagi (Dwinovo)", TAKER.describe());
        assertEquals("Usagi", new BoardSlot.Claim("Usagi", "").describe());
    }

    // ---- helpers ---------------------------------------------------------------

    @Test
    void theSlipsStillHangingAreTheOnesNobodyTookDown() {
        List<BoardSlot> slots = List.of(slot(FARMER), taken(FARMER), slot(MUSICIAN).reserve(OTHER, NOW + 100));

        assertEquals(0b101, BoardSlips.hanging(slots), "a taken slip still hangs, or a held one came down early");
        assertEquals(0, BoardSlips.hanging(List.of()), "an empty board hangs something");
    }

    private static SortedMap<Identifier, PetTaskType> types() {
        SortedMap<Identifier, PetTaskType> types = new TreeMap<>();
        types.put(id("weeding"), new PetTaskType(FARMER, PetWorkCounters.WEED, PetTask.NO_ICON,
            UniformInt.of(8, 16), reward("weeding"), 3, BoardLevels.FIRST_LEVEL));
        types.put(id("mushroom_picking"), new PetTaskType(FARMER, PetWorkCounters.PICK_MUSHROOM, PetTask.NO_ICON,
            UniformInt.of(8, 16), reward("mushroom_picking"), 2, BoardLevels.FIRST_LEVEL));
        return types;
    }

    private static BoardSlot taken(Identifier capability) {
        return slot(capability).claim(TAKER);
    }

    private static BoardSlot slot(Identifier capability) {
        return BoardSlot.open(new PetTask(id("test"), capability, PetWorkCounters.WEED, PetTask.NO_ICON, 10,
            reward("test"), 0));
    }

    private static ResourceKey<LootTable> reward(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, id("pet_task/" + path));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("chiikawa", path);
    }
}
