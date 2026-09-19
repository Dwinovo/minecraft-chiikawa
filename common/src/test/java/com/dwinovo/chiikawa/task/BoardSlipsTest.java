package com.dwinovo.chiikawa.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalInt;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.LongStream;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.storage.loot.LootTable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BoardSlipsTest {
    private static final ResourceLocation FARMER = id("farmer");
    private static final ResourceLocation MUSICIAN = id("musician");
    private static final UUID PET = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final long NOON = 6000L;
    private static final long NOW = 100_000L;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    // ---- rolling ---------------------------------------------------------------

    @Test
    void sameBoardAndDayRollsTheSameSlipsForEveryone() {
        long seed = BoardSlips.seed(42L, 7L, new BlockPos(10, 64, -3));
        List<BoardSlot> first = BoardSlips.roll(seed, types());

        assertEquals(BoardSlips.SLIPS_PER_DAY, first.size());
        assertEquals(first, BoardSlips.roll(seed, types()));
        first.forEach(slot -> assertTrue(slot.openTo(PET, NOW)));
    }

    @Test
    void anotherDayOrBoardRollsOtherwise() {
        BlockPos pos = new BlockPos(10, 64, -3);
        List<BoardSlot> today = BoardSlips.roll(BoardSlips.seed(42L, 7L, pos), types());

        assertTrue(LongStream.rangeClosed(8L, 20L)
            .anyMatch(day -> !BoardSlips.roll(BoardSlips.seed(42L, day, pos), types()).equals(today)));
        assertNotEquals(BoardSlips.seed(42L, 7L, pos), BoardSlips.seed(42L, 7L, pos.east()));
    }

    @Test
    void rolledTargetsStayWithinTheTypeAmount() {
        for (long day = 0; day < 50; day++) {
            for (BoardSlot slot : BoardSlips.roll(BoardSlips.seed(1L, day, BlockPos.ZERO), types())) {
                assertTrue(slot.slip().target() >= 8 && slot.slip().target() <= 16, () -> "target " + slot.slip().target());
                assertEquals(0, slot.slip().progress());
            }
        }
    }

    @Test
    void noLoadedTypesMeansNoSlips() {
        assertEquals(List.of(), BoardSlips.roll(1L, new TreeMap<>()));
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
        List<BoardSlot> slots = List.of(slot(FARMER).claim(), slot(FARMER).reserve(OTHER, NOW + 10), slot(FARMER));

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
        assertTrue(held.claim().claimed());
        assertFalse(held.claim().openTo(PET, NOW));
    }

    // ---- helpers ---------------------------------------------------------------

    private static SortedMap<ResourceLocation, PetTaskType> types() {
        SortedMap<ResourceLocation, PetTaskType> types = new TreeMap<>();
        types.put(id("weeding"), new PetTaskType(FARMER, PetWorkCounters.WEED, UniformInt.of(8, 16), reward("weeding"), 3));
        types.put(id("mushroom_picking"), new PetTaskType(FARMER, PetWorkCounters.PICK_MUSHROOM, UniformInt.of(8, 16),
            reward("mushroom_picking"), 2));
        return types;
    }

    private static BoardSlot slot(ResourceLocation capability) {
        return BoardSlot.open(new PetTask(id("test"), capability, PetWorkCounters.WEED, 10, reward("test"), 0));
    }

    private static ResourceKey<LootTable> reward(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, id("pet_task/" + path));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("chiikawa", path);
    }
}
