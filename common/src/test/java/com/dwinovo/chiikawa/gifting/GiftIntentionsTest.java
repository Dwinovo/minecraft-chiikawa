package com.dwinovo.chiikawa.gifting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.init.InitTag;
import com.dwinovo.chiikawa.testing.FixedRandom;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** When a pet thinks of a present: one at a time, by chance, after the delay, never while it rests, and not for long. */
class GiftIntentionsTest {
    private static final long NOW = 5000L;
    private static final int FADE = 24000;
    private static final int DELAY = 1200;
    private static final GiftingSettings SETTINGS = new GiftingSettings(24000, 3, FADE, Map.of(
        GiftOccasion.WAS_TREATED, new GiftingSettings.OccasionSettings(0.3F, DELAY),
        GiftOccasion.FILM_FULL, new GiftingSettings.OccasionSettings(1.0F, 0),
        GiftOccasion.GOT_PAID, new GiftingSettings.OccasionSettings(0.0F, 0)), List.of(), InitTag.NEVER_GIFTED);

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private final GiftIntentions intentions = new GiftIntentions();

    private boolean note(GiftOccasion occasion, long now, long restLeft, float roll) {
        return intentions.note(occasion, Optional.empty(), now, restLeft, FixedRandom.floats(roll), SETTINGS);
    }

    @Test
    void aPetThinksOfAPresentByTheChanceTheOccasionHas() {
        assertTrue(note(GiftOccasion.WAS_TREATED, NOW, 0, 0.29F));
        assertEquals(GiftOccasion.WAS_TREATED, intentions.held().orElseThrow().occasion());

        GiftIntentions other = new GiftIntentions();
        assertFalse(other.note(GiftOccasion.WAS_TREATED, Optional.empty(), NOW, 0, FixedRandom.floats(0.3F), SETTINGS),
            "a chance of three in ten is not met by a roll of three in ten");
        assertTrue(other.held().isEmpty());
    }

    @Test
    void anOccasionOfCertainChanceAlwaysDoesAndOneOfNoneNever() {
        assertTrue(note(GiftOccasion.FILM_FULL, NOW, 0, 0.999F));
        assertFalse(new GiftIntentions().note(GiftOccasion.GOT_PAID, Optional.empty(), NOW, 0, FixedRandom.floats(0.0F), SETTINGS));
    }

    @Test
    void anOccasionTheSettingsDoNotNameNeverDoesAndRollsNothing() {
        // A random that throws if it is asked: the roll is not made for an occasion that cannot count.
        assertFalse(intentions.note(GiftOccasion.PASSED_EXAM, Optional.empty(), NOW, 0, FixedRandom.floats(), SETTINGS));
        assertTrue(intentions.held().isEmpty());
    }

    @Test
    void aPetThinksOfOnePresentAtATime() {
        assertTrue(note(GiftOccasion.FILM_FULL, NOW, 0, 0.0F));

        assertFalse(note(GiftOccasion.FILM_FULL, NOW + 10, 0, 0.0F), "one in mind already");
        assertEquals(NOW, intentions.held().orElseThrow().readyAt());
    }

    @Test
    void aPetThatHasForgottenOneThinksOfAnother() {
        assertTrue(note(GiftOccasion.FILM_FULL, NOW, 0, 0.0F));
        long forgotten = intentions.held().orElseThrow().fadesAt();

        assertFalse(note(GiftOccasion.FILM_FULL, forgotten - 1, 0, 0.0F));
        assertTrue(note(GiftOccasion.WAS_TREATED, forgotten, 0, 0.0F), "a day on, the first is forgotten and the pet may think of a second");
        assertEquals(GiftOccasion.WAS_TREATED, intentions.held().orElseThrow().occasion());
    }

    @Test
    void aPetIsReadyToGiveAfterTheDelayAndForgetsItAfterTheFadeTime() {
        assertTrue(note(GiftOccasion.WAS_TREATED, NOW, 0, 0.0F));
        GiftIntention intention = intentions.held().orElseThrow();

        assertEquals(NOW + DELAY, intention.readyAt());
        assertEquals(NOW + DELAY + FADE, intention.fadesAt());
        assertTrue(intentions.ready(NOW).isEmpty(), "not as it is fed");
        assertTrue(intentions.ready(NOW + DELAY - 1).isEmpty());
        assertTrue(intentions.ready(NOW + DELAY).isPresent());
        assertTrue(intentions.ready(NOW + DELAY + FADE - 1).isPresent());
        assertTrue(intentions.ready(NOW + DELAY + FADE).isEmpty(), "an owner who stays away is given nothing: the thought fades");
        assertTrue(intentions.held().isPresent(), "forgotten is not taken away: only the pet's giving does that");
    }

    @Test
    void aPetThatWouldStillBeRestingWhenItIsReadyThinksOfNoPresent() {
        assertFalse(note(GiftOccasion.WAS_TREATED, NOW, DELAY + 1, 0.0F), "still resting from the last when it would set off");
        assertTrue(intentions.held().isEmpty());
        assertTrue(note(GiftOccasion.WAS_TREATED, NOW, DELAY, 0.0F), "the rest is over by the time it is ready");
    }

    @Test
    void aPetThatRestsFromAPresentThinksOfNoneThatIsReadyAtOnce() {
        assertFalse(note(GiftOccasion.FILM_FULL, NOW, 1, 0.0F));
        assertTrue(intentions.held().isEmpty());
    }

    @Test
    void thePresentIsGoneOnceItIsTakenOutOfTheBag() {
        note(GiftOccasion.FILM_FULL, NOW, 0, 0.0F);

        intentions.consume();

        assertTrue(intentions.held().isEmpty());
        assertTrue(note(GiftOccasion.FILM_FULL, NOW, 0, 0.0F), "and the pet may think of another");
    }

    @Test
    void whatThePetSetAsideIsKeptAsACopy() {
        ItemStack paper = new ItemStack(Items.PAPER);
        intentions.note(GiftOccasion.FILM_FULL, Optional.of(paper), NOW, 0, FixedRandom.floats(0.0F), SETTINGS);

        paper.setCount(0);

        assertEquals(1, intentions.held().orElseThrow().keepsake().orElseThrow().getCount());
    }

    @Test
    void aPresentInMindIsSavedWithThePetAndReadBack() {
        HolderLookup.Provider registries = VanillaRegistries.createLookup();
        intentions.note(GiftOccasion.FILM_FULL, Optional.of(new ItemStack(Items.PAPER)), NOW, 0, FixedRandom.floats(0.0F), SETTINGS);
        CompoundTag tag = new CompoundTag();
        intentions.save(tag, registries);

        GiftIntentions loaded = new GiftIntentions();
        loaded.load(tag, registries);

        GiftIntention was = intentions.held().orElseThrow();
        GiftIntention is = loaded.held().orElseThrow();
        assertEquals(was.occasion(), is.occasion());
        assertEquals(was.readyAt(), is.readyAt());
        assertEquals(was.fadesAt(), is.fadesAt());
        assertTrue(ItemStack.isSameItemSameComponents(was.keepsake().orElseThrow(), is.keepsake().orElseThrow()));
    }

    @Test
    void aPetWithNoPresentInMindSavesNothingAndASaveWithNoneLoadsNone() {
        CompoundTag tag = new CompoundTag();
        intentions.save(tag, VanillaRegistries.createLookup());
        assertTrue(tag.isEmpty(), "a pet that never thought of one is saved as it was in 0.1.3");

        note(GiftOccasion.FILM_FULL, NOW, 0, 0.0F);
        intentions.load(tag, VanillaRegistries.createLookup());
        assertTrue(intentions.held().isEmpty());
    }

    @Test
    void aSavedPresentOfAnOccasionNobodyKnowsIsLeftOut() {
        CompoundTag saved = new CompoundTag();
        saved.putString("Occasion", "winning_the_lottery");
        CompoundTag tag = new CompoundTag();
        tag.put("GiftIntention", saved);

        intentions.load(tag, VanillaRegistries.createLookup());

        assertTrue(intentions.held().isEmpty());
    }
}
