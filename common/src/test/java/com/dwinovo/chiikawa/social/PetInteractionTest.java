package com.dwinovo.chiikawa.social;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.task.FinishedSlip;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PetInteractionTest {
    private static final Identifier CLING = Identifier.fromNamespaceAndPath("chiikawa", "cling");
    private static final Identifier GREETING = Identifier.fromNamespaceAndPath("chiikawa", "crab_greeting");
    private static final Identifier WEEDING = Identifier.fromNamespaceAndPath("chiikawa", "weeding");

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void eachPetPlaysThePartOfTheFirstEntryNamingIt() {
        PetInteraction interaction = parse("""
            { "initiators": [ { "pets": [ "minecraft:fox" ], "pose": "cling",
                                "begin": { "reaction": "happy", "voice": "cling" } } ],
              "partners": [
                { "pets": [ "minecraft:pig" ], "pose": "clung_to", "begin": { "reaction": "hurt" } },
                { "pets": [ "minecraft:cow", "minecraft:pig" ], "begin": { "reaction": "confused" } } ],
              "notice_distance": 8, "approach_distance": 1, "reservation_ticks": 300, "duration_ticks": 80,
              "cooldown_ticks": 12000, "chance": 0.05 }""");

        assertEquals(Optional.of("cling"), interaction.initiatorSide(type(EntityTypes.FOX)).orElseThrow().pose());
        assertTrue(interaction.initiatorSide(type(EntityTypes.PIG)).isEmpty(), "a pig does not start it");
        assertEquals(OptionalInt.of(0), interaction.partnerRank(type(EntityTypes.PIG)), "the first entry wins");
        assertEquals(OptionalInt.of(1), interaction.partnerRank(type(EntityTypes.COW)));
        assertTrue(interaction.partnerRank(type(EntityTypes.FOX)).isEmpty());
        assertEquals(Optional.of(PetReaction.HURT), interaction.partners().get(0).begin().orElseThrow().reaction());
        assertEquals(Optional.empty(), interaction.partners().get(1).pose(), "a part may be only a face");
        assertEquals(Optional.of(VoiceMoment.CLING), interaction.initiators().get(0).begin().orElseThrow().voice());
    }

    @Test
    void aPartMayHaveABeatNowAndThenAndOneAtTheEnd() {
        PetInteraction interaction = parse(minimal().replace("{ \"pets\": [ \"minecraft:fox\" ] }", """
            { "pets": [ "minecraft:fox" ], "pose": "sit",
              "now_and_then": { "every_ticks": { "type": "minecraft:uniform", "min_inclusive": 60, "max_inclusive": 160 },
                                "animation": "clap", "voice": "listen" },
              "end": { "reaction": "hurt", "voice": "turned_down" } }"""));
        PetInteraction.Side part = interaction.initiators().get(0);

        Beat.Recurring clapping = part.nowAndThen().orElseThrow();
        assertEquals(Optional.of("clap"), clapping.beat().animation(), "a beat may be a move of its own");
        assertEquals(Optional.of(VoiceMoment.LISTEN), clapping.beat().voice());
        RandomSource random = RandomSource.create(1);
        for (int i = 0; i < 100; i++) {
            int wait = clapping.nextWait(random);
            assertTrue(wait >= 60 && wait <= 160, () -> "waited " + wait);
        }
        assertEquals(Optional.of(PetReaction.HURT), part.end().orElseThrow().reaction());
        assertEquals(Optional.of(VoiceMoment.TURNED_DOWN), part.end().orElseThrow().voice());
        assertTrue(part.begin().isEmpty(), "a part need not do anything as it begins");
    }

    @Test
    void aBeatThatComesRoundEveryTickOrNeverIsRejected() {
        assertTrue(PetInteraction.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(minimal().replace(
            "{ \"pets\": [ \"minecraft:fox\" ] }",
            "{ \"pets\": [ \"minecraft:fox\" ], \"now_and_then\": { \"every_ticks\": 0, \"animation\": \"clap\" } }"))).isError());
        assertTrue(PetInteraction.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(minimal().replace(
            "{ \"pets\": [ \"minecraft:fox\" ] }",
            "{ \"pets\": [ \"minecraft:fox\" ], \"now_and_then\": { \"animation\": \"clap\" } }"))).isError());
        assertTrue(PetInteraction.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(minimal().replace(
            "{ \"pets\": [ \"minecraft:fox\" ] }",
            "{ \"pets\": [ \"minecraft:fox\" ], \"now_and_then\": { \"every_ticks\": 100, \"animation\": \"clap\" } }")))
            .isSuccess(), "a steady beat is a plain number");
    }

    @Test
    void anIdlePartnerAndNothingHandedOverUnlessTheFileSaysSo() {
        PetInteraction interaction = parse(minimal());

        assertEquals(PetInteraction.PartnerState.IDLE, interaction.partnerState());
        assertTrue(interaction.partnerFinished().isEmpty());
        assertTrue(interaction.handsOver().isEmpty());
        assertEquals(0, interaction.partnerEagerTicks());
        assertFalse(interaction.handsOver(new ItemStack(Items.COOKIE)));
    }

    @Test
    void whatIsHandedOverIsNamedByItemOrTag() {
        PetInteraction interaction = parse(minimal().replace("\"chance\"", "\"hands_over\": \"minecraft:cookie\", \"chance\""));

        assertTrue(interaction.handsOver(new ItemStack(Items.COOKIE)));
        assertFalse(interaction.handsOver(new ItemStack(Items.BREAD)));
    }

    @Test
    void aSceneAboutJustFinishedWorkOnlyTakesAPartnerFreshOffThatSlip() {
        PetInteraction.SlipCondition justWeeded = new PetInteraction.SlipCondition(WEEDING, 2400);

        assertTrue(justWeeded.metBy(Optional.of(new FinishedSlip(WEEDING, 1000)), 3400));
        assertFalse(justWeeded.metBy(Optional.of(new FinishedSlip(WEEDING, 1000)), 3401), "too long ago");
        assertFalse(justWeeded.metBy(Optional.of(new FinishedSlip(Identifier.fromNamespaceAndPath("chiikawa",
            "street_performance"), 1000)), 1200), "another job");
        assertFalse(justWeeded.metBy(Optional.empty(), 1200), "never finished one");
    }

    @Test
    void aFileWithoutAPartnerOrWithAnImpossibleChanceIsRejected() {
        assertTrue(PetInteraction.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
            minimal().replace("[ { \"pets\": [ \"minecraft:cow\" ] } ]", "[]"))).isError());
        assertTrue(PetInteraction.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
            minimal().replace("0.5", "1.5"))).isError());
        assertTrue(PetInteraction.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
            minimal().replace("{ \"pets\": [ \"minecraft:cow\" ] }",
                "{ \"pets\": [ \"minecraft:cow\" ], \"begin\": { \"reaction\": \"sulk\" } }"))).isError());
    }

    @Test
    void theLoaderSkipsWhatDoesNotParseAndWarnsAboutPetsThatDoNotExist() {
        PetInteractionLoader.Loaded loaded = PetInteractionLoader.load(Map.of(
            CLING, JsonParser.parseString(minimal().replace("minecraft:cow", "chiikawa:nobody")),
            GREETING, JsonParser.parseString("{ \"initiators\": [] }")),
            id -> id.getNamespace().equals("minecraft"));

        assertEquals(java.util.Set.of(CLING), loaded.interactions().keySet());
        assertEquals(1, loaded.errors().size(), loaded.errors()::toString);
        assertTrue(loaded.errors().get(0).contains(GREETING.toString()), loaded.errors()::toString);
        assertEquals(1, loaded.warnings().size(), loaded.warnings()::toString);
        assertTrue(loaded.warnings().get(0).contains("chiikawa:nobody"), loaded.warnings()::toString);
    }

    @Test
    void thePairThatPlayedASceneLeavesThatSceneAloneUntilItsCooldownIsOver() {
        UUID other = UUID.randomUUID();
        SocialCooldowns cooldowns = SocialCooldowns.NONE.with(CLING, other, 1200, 0);

        assertTrue(cooldowns.coolingDown(CLING, other, 1199));
        assertFalse(cooldowns.coolingDown(CLING, other, 1200), "over");
        assertFalse(cooldowns.coolingDown(GREETING, other, 600), "another scene with the same pet");
        assertFalse(cooldowns.coolingDown(CLING, UUID.randomUUID(), 600), "the same scene with another pet");
    }

    @Test
    void cooldownsThatAreOverAreForgotten() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        SocialCooldowns cooldowns = SocialCooldowns.NONE.with(CLING, first, 100, 0).with(CLING, second, 2000, 500);

        assertEquals(1, cooldowns.until().size());
        assertTrue(cooldowns.coolingDown(CLING, second, 1999));
    }

    private static String minimal() {
        return """
            { "initiators": [ { "pets": [ "minecraft:fox" ] } ],
              "partners": [ { "pets": [ "minecraft:cow" ] } ],
              "notice_distance": 8, "approach_distance": 1, "reservation_ticks": 300, "duration_ticks": 80,
              "cooldown_ticks": 12000, "chance": 0.5 }""";
    }

    private static PetInteraction parse(String json) {
        return PetInteraction.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    private static Holder<EntityType<?>> type(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type);
    }
}
