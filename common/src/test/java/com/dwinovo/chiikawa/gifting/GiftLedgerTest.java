package com.dwinovo.chiikawa.gifting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** How many presents an owner is given in a day, whichever of their pets gives them. */
class GiftLedgerTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final int LIMIT = 3;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private final GiftLedger ledger = new GiftLedger();

    @Test
    void anOwnerWhoWasGivenNothingMayBeGivenTheLimit() {
        assertEquals(LIMIT, ledger.left(OWNER, 4, LIMIT));
    }

    @Test
    void everyPresentOfTheDayCountsWhicheverPetGaveIt() {
        ledger.record(OWNER, 4);
        assertEquals(2, ledger.left(OWNER, 4, LIMIT));
        ledger.record(OWNER, 4);
        ledger.record(OWNER, 4);

        assertEquals(0, ledger.left(OWNER, 4, LIMIT), "three presents from three pets, or one pet three times, are the day's");
    }

    @Test
    void anOwnerWhoWasGivenMoreThanTheLimitIsLeftWithNone() {
        for (int given = 0; given < LIMIT + 2; given++) {
            ledger.record(OWNER, 4);
        }

        assertEquals(0, ledger.left(OWNER, 4, LIMIT));
    }

    @Test
    void theNextDayStartsAgain() {
        for (int given = 0; given < LIMIT; given++) {
            ledger.record(OWNER, 4);
        }

        assertEquals(LIMIT, ledger.left(OWNER, 5, LIMIT));
        ledger.record(OWNER, 5);
        assertEquals(LIMIT - 1, ledger.left(OWNER, 5, LIMIT), "and the count of the day before is not carried over");
    }

    @Test
    void eachOwnerCountsTheirOwn() {
        ledger.record(OWNER, 4);
        ledger.record(OWNER, 4);

        assertEquals(LIMIT, ledger.left(OTHER, 4, LIMIT));
    }

    @Test
    void aLimitOfNoneIsNone() {
        assertEquals(0, ledger.left(OWNER, 4, 0));
    }

    @Test
    void theCountsAreSavedWithTheWorldAndReadBack() {
        ledger.record(OWNER, 4);
        ledger.record(OWNER, 4);
        ledger.record(OTHER, 3);

        GiftLedger loaded = GiftLedger.load(ledger.save(new CompoundTag(), VanillaRegistries.createLookup()), VanillaRegistries.createLookup());

        assertEquals(1, loaded.left(OWNER, 4, LIMIT));
        assertEquals(2, loaded.left(OTHER, 3, LIMIT));
        assertEquals(LIMIT, loaded.left(OTHER, 4, LIMIT));
    }

    @Test
    void aWorldThatNeverHadOneStartsEmpty() {
        assertTrue(GiftLedger.load(new CompoundTag(), VanillaRegistries.createLookup()).left(OWNER, 0, LIMIT) == LIMIT);
    }
}
