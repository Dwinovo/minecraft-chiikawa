package com.dwinovo.chiikawa.entity.brain.errand.pastime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.errand.LackKey;
import com.dwinovo.chiikawa.entity.brain.errand.view.DayPhase;
import com.dwinovo.chiikawa.entity.brain.errand.view.Surroundings;
import com.dwinovo.chiikawa.entity.brain.errand.view.Surroundings.Sighting;
import com.dwinovo.chiikawa.entity.brain.errand.view.Weather;
import com.dwinovo.chiikawa.testing.TestAnchors;
import com.dwinovo.chiikawa.testing.TestView;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.JsonOps;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The cues a pastime waits for, each read off the pet only through its view: that each holds when what it is about is so and
 * not when it is not, and that the files that name them read back as they were written and are refused when they name one that
 * is not.
 */
class PastimeCueTest {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
        ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("dimension")), ResourceLocation.withDefaultNamespace("overworld"));
    private static final GlobalPos HOME = GlobalPos.of(OVERWORLD, new BlockPos(0, 64, 0));
    private static final TagKey<EntityType<?>> BONES = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("chiikawa", "bones"));
    private static final TagKey<Item> SWEETS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("chiikawa", "sweets"));

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static TestView pet() {
        return TestView.at(HOME, TestAnchors.free(HOME));
    }

    private static GlobalPos east(int blocks) {
        return GlobalPos.of(OVERWORLD, HOME.pos().east(blocks));
    }

    @Test
    void rainHoldsInAShowerAndInAStormAndThunderOnlyInAStorm() {
        PastimeCue rain = new PastimeCue.WeatherIs(Weather.RAIN);
        PastimeCue thunder = new PastimeCue.WeatherIs(Weather.THUNDER);

        assertFalse(rain.holds(pet()));
        assertTrue(rain.holds(pet().weather(Weather.RAIN, true)));
        assertTrue(rain.holds(pet().weather(Weather.THUNDER, true)));
        assertFalse(thunder.holds(pet().weather(Weather.RAIN, true)));
        assertTrue(thunder.holds(pet().weather(Weather.THUNDER, true)));
    }

    @Test
    void clearWeatherIsNoWeatherToWaitFor() {
        assertTrue(PastimeCue.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{ \"type\": \"weather\", \"is\": \"clear\" }")).isError());
    }

    @Test
    void underTheSkyHoldsOnlyWhereTheRainFalls() {
        assertTrue(new PastimeCue.UnderSky().holds(pet().weather(Weather.RAIN, true)));
        assertFalse(new PastimeCue.UnderSky().holds(pet().weather(Weather.RAIN, false)));
    }

    @Test
    void aPartOfTheDayHoldsInAnyOfThePartsItNames() {
        PastimeCue dusk = new PastimeCue.InPhase(Set.of(DayPhase.EVENING, DayPhase.NIGHT));

        assertTrue(dusk.holds(pet().phase(DayPhase.EVENING)));
        assertTrue(dusk.holds(pet().phase(DayPhase.NIGHT)));
        assertFalse(dusk.holds(pet().phase(DayPhase.DAY)));
        assertFalse(dusk.holds(pet().phase(DayPhase.MORNING)));
    }

    @Test
    void aPetIsScaredByAStormByACreeperAboutToGoOffAndByWhatHurtItAndByNothingElse() {
        PastimeCue thunder = new PastimeCue.ScaredBy(PastimeCue.Fright.THUNDER);
        PastimeCue creeper = new PastimeCue.ScaredBy(PastimeCue.Fright.CREEPER);
        PastimeCue hurt = new PastimeCue.ScaredBy(PastimeCue.Fright.HURT);
        TestView calm = pet().weather(Weather.RAIN, true);

        assertFalse(thunder.holds(calm) || creeper.holds(calm) || hurt.holds(calm));
        assertTrue(thunder.holds(pet().weather(Weather.THUNDER, true)));
        assertTrue(creeper.holds(pet().surroundings(new Surroundings(Optional.empty(), List.of(), List.of(), true))));
        assertTrue(hurt.holds(pet().attackedBy(UUID.randomUUID(), east(3))));
    }

    @Test
    void theOwnerHoldsOnlyWhileTheyAreNearAndDoingWhatTheCueSays() {
        TestView near = pet().ownerPos(east(4));
        PastimeCue there = new PastimeCue.OwnerIs(PastimeCue.OwnerState.NEAR, 8.0);
        PastimeCue working = new PastimeCue.OwnerIs(PastimeCue.OwnerState.WORKING, 8.0);
        PastimeCue back = new PastimeCue.OwnerIs(PastimeCue.OwnerState.RETURNED, 8.0);
        PastimeCue asleep = new PastimeCue.OwnerIs(PastimeCue.OwnerState.SLEEPING, 8.0);

        assertFalse(there.holds(pet()), "no owner is no one near");
        assertFalse(there.holds(pet().ownerPos(east(9))), "too far off");
        assertTrue(there.holds(near));
        assertFalse(working.holds(near));
        assertTrue(working.holds(near.ownerAtWork()));
        assertFalse(working.holds(pet().ownerAtWork()), "an owner at work who is nowhere near is not watched");
        assertFalse(back.holds(near));
        assertTrue(back.holds(near.ownerBackAgain()));
        assertFalse(asleep.holds(near));
        assertTrue(asleep.holds(near.ownerAsleepBeside(east(5))));
    }

    @Test
    void theOwnerInAnotherWorldIsNotNear() {
        GlobalPos nether = GlobalPos.of(ResourceKey.create(ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("dimension")),
            ResourceLocation.withDefaultNamespace("the_nether")), HOME.pos());

        assertFalse(new PastimeCue.OwnerIs(PastimeCue.OwnerState.NEAR, 8.0).holds(pet().ownerPos(nether)));
    }

    @Test
    void aPetSeesACreatureOrAThingOfAKindOnlyWithinTheRange() {
        bind(EntityType.SKELETON.builtInRegistryHolder(), BONES);
        bind(Items.COOKIE.builtInRegistryHolder(), SWEETS);
        try {
            Surroundings around = new Surroundings(Optional.empty(),
                List.of(new Sighting<EntityType<?>>(UUID.randomUUID(), east(6).pos(), EntityType.SKELETON),
                    new Sighting<EntityType<?>>(UUID.randomUUID(), east(2).pos(), EntityType.PIG)),
                List.of(new Sighting<Item>(UUID.randomUUID(), east(3).pos(), Items.COOKIE)), false);
            PastimeCue bones = new PastimeCue.Sees(Either.left(BONES), 8.0);
            PastimeCue bonesNear = new PastimeCue.Sees(Either.left(BONES), 4.0);
            PastimeCue sweets = new PastimeCue.Sees(Either.right(SWEETS), 8.0);
            PastimeCue sweetsFar = new PastimeCue.Sees(Either.right(SWEETS), 2.0);

            assertFalse(bones.holds(pet()), "nothing seen");
            assertTrue(bones.holds(pet().surroundings(around)));
            assertFalse(bonesNear.holds(pet().surroundings(around)), "the skeleton is farther than that, and the pig is no skeleton");
            assertTrue(sweets.holds(pet().surroundings(around)));
            assertFalse(sweetsFar.holds(pet().surroundings(around)));
        } finally {
            bind(EntityType.SKELETON.builtInRegistryHolder(), null);
            bind(Items.COOKIE.builtInRegistryHolder(), null);
        }
    }

    /**
     * Makes a tag the game does not have outside a game one of the tags of a thing, or takes it away: the tags of the game's
     * registries are bound when a world loads, and a case here has no world.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void bind(Holder.Reference holder, TagKey tag) {
        try {
            Method bindTags = Holder.Reference.class.getDeclaredMethod("bindTags", Collection.class);
            bindTags.setAccessible(true);
            bindTags.invoke(holder, tag == null ? Set.of() : Set.of(tag));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void lackingHoldsWhileThePetIsShortOfSomething() {
        assertFalse(new PastimeCue.Lacking().holds(pet()));
        assertTrue(new PastimeCue.Lacking().holds(pet().lacking(LackKey.tag(ItemTags.ARROWS))));
    }

    @Test
    void everyCueIsWrittenAsItIsRead() {
        List<PastimeCue> cues = List.of(
            new PastimeCue.WeatherIs(Weather.RAIN), new PastimeCue.WeatherIs(Weather.THUNDER), new PastimeCue.UnderSky(),
            new PastimeCue.InPhase(Set.of(DayPhase.EVENING, DayPhase.NIGHT)),
            new PastimeCue.ScaredBy(PastimeCue.Fright.CREEPER), new PastimeCue.OwnerIs(PastimeCue.OwnerState.WORKING, 16.0),
            new PastimeCue.Sees(Either.left(EntityTypeTags.SKELETONS), 8.0),
            new PastimeCue.Sees(Either.right(ItemTags.FISHES), 6.0), new PastimeCue.Lacking());

        for (PastimeCue cue : cues) {
            var json = PastimeCue.CODEC.encodeStart(JsonOps.INSTANCE, cue).getOrThrow();

            assertEquals(cue, PastimeCue.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(), json::toString);
        }
    }

    @Test
    void aCueWithATypeNobodyHasIsRefused() {
        assertTrue(PastimeCue.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{ \"type\": \"full_moon\" }")).isError());
        assertTrue(PastimeCue.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{ \"is\": \"rain\" }")).isError(), "and one with none");
    }

    @Test
    void aCueWithoutWhatItNeedsIsRefused() {
        assertTrue(PastimeCue.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{ \"type\": \"owner\", \"is\": \"working\" }")).isError(),
            "an owner cue says how near");
        assertTrue(PastimeCue.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{ \"type\": \"scared\", \"by\": \"mondays\" }")).isError());
        assertTrue(PastimeCue.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{ \"type\": \"sees\", \"range\": 8 }")).isError(),
            "seeing is of something");
    }
}
