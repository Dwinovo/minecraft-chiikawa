package com.dwinovo.chiikawa.whistle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.whistle.WhistleHearing.Hearing;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.Map;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Who a whistle is for, how an order is cycled and saved, and the settings a pack gives it. */
class WhistleTest {
    private static final UUID ME = UUID.randomUUID();
    private static final UUID SOMEONE_ELSE = UUID.randomUUID();
    private static final double RANGE = 32.0;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static Hearing hear(PetDirective order, PetOwnership ownership, boolean sameLevel, double distance,
                                boolean seatedAtExam) {
        return WhistleHearing.of(ME, order, ownership, sameLevel, distance * distance, RANGE, seatedAtExam);
    }

    @Test
    void myPetsHearItWithinRangeWhateverTheOrder() {
        for (PetDirective order : PetDirective.values()) {
            assertEquals(Hearing.OWNED, hear(order, new PetOwnership.Owned(ME), true, 10.0, false), order.name());
        }
    }

    @Test
    void aPetBeyondRangeOrInAnotherWorldDoesNotHearIt() {
        assertEquals(Hearing.DEAF, hear(PetDirective.FOLLOW, new PetOwnership.Owned(ME), true, RANGE + 1, false));
        assertEquals(Hearing.OWNED, hear(PetDirective.FOLLOW, new PetOwnership.Owned(ME), true, RANGE, false));
        assertEquals(Hearing.DEAF, hear(PetDirective.FOLLOW, new PetOwnership.Owned(ME), false, 1.0, false));
        assertEquals(Hearing.DEAF, hear(PetDirective.FOLLOW, PetOwnership.WILD, false, 1.0, false));
    }

    @Test
    void someoneElsesPetNeverHearsIt() {
        for (PetDirective order : PetDirective.values()) {
            assertEquals(Hearing.DEAF, hear(order, new PetOwnership.Owned(SOMEONE_ELSE), true, 3.0, false), order.name());
        }
    }

    @Test
    void aWildPetIsCuriousOnlyAboutACallToFollow() {
        assertEquals(Hearing.WILD, hear(PetDirective.FOLLOW, PetOwnership.WILD, true, 10.0, false));
        assertEquals(Hearing.DEAF, hear(PetDirective.STAY, PetOwnership.WILD, true, 10.0, false));
        assertEquals(Hearing.DEAF, hear(PetDirective.FREE, PetOwnership.WILD, true, 10.0, false));
    }

    @Test
    void aPetAtItsExamDeskKeepsWriting() {
        assertEquals(Hearing.DEAF, hear(PetDirective.FOLLOW, new PetOwnership.Owned(ME), true, 3.0, true));
        assertEquals(Hearing.DEAF, hear(PetDirective.FOLLOW, PetOwnership.WILD, true, 3.0, true));
    }

    @Test
    void theOrderCyclesFollowStayFreeAndBack() {
        PetDirective order = PetDirective.FOLLOW;
        order = order.next();
        assertEquals(PetDirective.STAY, order);
        order = order.next();
        assertEquals(PetDirective.FREE, order);
        assertEquals(PetDirective.FOLLOW, order.next());
    }

    @Test
    void anOrderIsSavedAsItsOrdinal() {
        for (PetDirective order : PetDirective.values()) {
            JsonElement saved = PetDirective.CODEC.encodeStart(JsonOps.INSTANCE, order).getOrThrow();
            assertEquals(order, PetDirective.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow());
        }
    }

    @Test
    void settingsParseFromTheGeneratedShape() {
        WhistleSettings settings = WhistleSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
            { "range": 32.0, "blow_ticks": 10, "cooldown_ticks": 40, "react_delay_ticks": 10,
              "curious_ticks": 200, "sound": "minecraft:block.note_block.flute", "pitch": 2.0 }""")).getOrThrow();

        assertEquals(new WhistleSettings(32.0, 10, 40, 10, 200,
            new ResourceLocation("block.note_block.flute"), 2.0F), settings);
    }

    @Test
    void settingsWithoutARangeOrWithNoBlowTimeAreRejected() {
        assertTrue(WhistleSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
            { "blow_ticks": 10, "cooldown_ticks": 40, "react_delay_ticks": 10,
              "curious_ticks": 200, "sound": "minecraft:block.note_block.flute", "pitch": 2.0 }""")).error().isPresent());
        assertTrue(WhistleSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
            { "range": 32.0, "blow_ticks": 0, "cooldown_ticks": 40, "react_delay_ticks": 10,
              "curious_ticks": 200, "sound": "minecraft:block.note_block.flute", "pitch": 2.0 }""")).error().isPresent());
    }

    @Test
    void aBrokenFileIsSkippedRatherThanTakingTheRestWithIt() {
        ResourceLocation good = new ResourceLocation("chiikawa", "whistle_candy");
        ResourceLocation broken = new ResourceLocation("chiikawa", "broken");
        WhistleSettingsLoader.Loaded loaded = WhistleSettingsLoader.load(Map.of(
            good, JsonParser.parseString("""
                { "range": 8.0, "blow_ticks": 5, "cooldown_ticks": 0, "react_delay_ticks": 0,
                  "curious_ticks": 20, "sound": "minecraft:block.note_block.flute", "pitch": 1.0 }"""),
            broken, JsonParser.parseString("{ \"range\": \"far\" }")));

        assertEquals(1, loaded.settings().size());
        assertTrue(loaded.settings().containsKey(good));
        assertEquals(1, loaded.errors().size());
    }
}
