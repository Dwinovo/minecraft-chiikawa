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
import net.minecraft.world.entity.EntityType;
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
            { "initiators": [ { "pets": [ "minecraft:fox" ], "animation": "cling", "reaction": "happy", "voice": "cling" } ],
              "partners": [
                { "pets": [ "minecraft:pig" ], "animation": "clung_to", "reaction": "hurt" },
                { "pets": [ "minecraft:cow", "minecraft:pig" ], "reaction": "confused" } ],
              "notice_distance": 8, "approach_distance": 1, "reservation_ticks": 300, "duration_ticks": 80,
              "cooldown_ticks": 12000, "chance": 0.05 }""");

        assertEquals(Optional.of("cling"), interaction.initiatorSide(type(EntityType.FOX)).orElseThrow().animation());
        assertTrue(interaction.initiatorSide(type(EntityType.PIG)).isEmpty(), "a pig does not start it");
        assertEquals(OptionalInt.of(0), interaction.partnerRank(type(EntityType.PIG)), "the first entry wins");
        assertEquals(OptionalInt.of(1), interaction.partnerRank(type(EntityType.COW)));
        assertTrue(interaction.partnerRank(type(EntityType.FOX)).isEmpty());
        assertEquals(Optional.of(PetReaction.HURT), interaction.partners().get(0).reaction());
        assertEquals(Optional.empty(), interaction.partners().get(1).animation(), "a part may be only a face");
        assertEquals(Optional.of(VoiceMoment.CLING), interaction.initiators().get(0).voice());
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
                "{ \"pets\": [ \"minecraft:cow\" ], \"reaction\": \"sulk\" }"))).isError());
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
