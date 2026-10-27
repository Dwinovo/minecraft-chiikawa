package com.dwinovo.chiikawa.entity.brain.errand.remark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.errand.LackKey;
import com.dwinovo.chiikawa.entity.brain.errand.decide.WhyNot;
import com.dwinovo.chiikawa.entity.brain.errand.view.DayPhase;
import com.dwinovo.chiikawa.entity.brain.errand.view.Weather;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** A remark's file: what it says, and that a broken one leaves that remark unmade and the rest as they were. */
class PetRemarkLoaderTest {
    private static final ResourceLocation MISSING = ResourceLocation.fromNamespaceAndPath("chiikawa", "missing");
    private static final ResourceLocation BROKEN = ResourceLocation.fromNamespaceAndPath("chiikawa", "broken");

    private static final String FILE = """
        { "when": [ { "type": "lacked_for", "ticks": 600 }, { "type": "owner_within", "blocks": 8.0 } ],
          "cast": { "*": { "moment": "missing" },
                    "chiikawa:hachiware": { "moment": "trouble", "gesture": "scratch_head" } },
          "cooldown_ticks": 12000 }""";

    @Test
    void aFileSaysWhenAndWhatEachCharacterDoes() {
        PetRemark remark = PetRemark.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(FILE)).getOrThrow();

        assertEquals(List.of(new RemarkCue.LackedFor(600), new RemarkCue.OwnerWithin(8.0)), remark.when());
        assertEquals(12000, remark.cooldownTicks());
        assertEquals(Optional.of(new PetRemark.Part(VoiceMoment.TROUBLE, Optional.of("scratch_head"))),
            remark.partOf(ResourceLocation.fromNamespaceAndPath("chiikawa", "hachiware")));
        assertEquals(Optional.of(new PetRemark.Part(VoiceMoment.MISSING, Optional.empty())),
            remark.partOf(ResourceLocation.fromNamespaceAndPath("chiikawa", "usagi")));
    }

    @Test
    void aRemarkSurvivesTheTripThroughItsFile() {
        PetRemark remark = PetRemark.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(FILE)).getOrThrow();

        assertEquals(remark, PetRemark.CODEC.parse(JsonOps.INSTANCE,
            PetRemark.CODEC.encodeStart(JsonOps.INSTANCE, remark).getOrThrow()).getOrThrow());
    }

    @Test
    void aFileWithACueNobodyKnowsACastNoCharacterIsInOrNoRestIsNotARemark() {
        for (String broken : List.of(
                """
                { "when": [ { "type": "sulking" } ], "cast": { "*": { "moment": "missing" } }, "cooldown_ticks": 100 }""",
                """
                { "when": [], "cast": { "not a character": { "moment": "missing" } }, "cooldown_ticks": 100 }""",
                """
                { "when": [], "cast": { "*": { "moment": "yodelling" } }, "cooldown_ticks": 100 }""",
                """
                { "when": [], "cast": { "*": { "moment": "missing" } }, "cooldown_ticks": 0 }""",
                """
                { "when": [], "cast": { "*": { "moment": "missing" } } }""")) {
            assertTrue(PetRemark.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(broken)).error().isPresent(), broken);
        }
    }

    @Test
    void aBrokenFileLeavesThatRemarkUnmadeAndTheRestAsTheyWere() {
        PetRemarkLoader.Loaded loaded = PetRemarkLoader.load(Map.of(MISSING, JsonParser.parseString(FILE),
            BROKEN, JsonParser.parseString("{ \"when\": [ { \"type\": \"sulking\" } ] }")));

        assertEquals(Set.of(MISSING), loaded.remarks().keySet());
        assertEquals(1, loaded.errors().size());
        assertTrue(loaded.errors().get(0).contains(BROKEN.toString()), loaded.errors()::toString);
    }

    @Test
    void aRemarkIsMadeOnlyWhenEveryCueOfItsWhenHolds() {
        PetRemark remark = PetRemark.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(FILE)).getOrThrow();
        WhyNot lacking = new WhyNot(MISSING, "intent.chiikawa.fail.no_arrows", Optional.of(LackKey.item(MISSING)), 0L);

        assertTrue(remark.holds(new RemarkCue.Situation(Optional.of(lacking), 600, OptionalDouble.of(3.0), DayPhase.DAY, Weather.CLEAR)));
        assertFalse(remark.holds(new RemarkCue.Situation(Optional.of(lacking), 599, OptionalDouble.of(3.0), DayPhase.DAY, Weather.CLEAR)));
        assertFalse(remark.holds(new RemarkCue.Situation(Optional.of(lacking), 600, OptionalDouble.of(9.0), DayPhase.DAY, Weather.CLEAR)));
    }
}
