package com.dwinovo.chiikawa.entity.brain.errand.pastime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.data.CharacterPastimeData;
import com.dwinovo.chiikawa.entity.PetCharacters;
import com.dwinovo.chiikawa.entity.brain.errand.view.DayPhase;
import com.dwinovo.chiikawa.entity.brain.errand.view.Surroundings;
import com.dwinovo.chiikawa.entity.brain.errand.view.Surroundings.Sighting;
import com.dwinovo.chiikawa.entity.brain.errand.view.Weather;
import com.dwinovo.chiikawa.entity.brain.errand.step.Goal;
import com.dwinovo.chiikawa.entity.brain.errand.work.action.AskPraiseAction;
import com.dwinovo.chiikawa.entity.brain.errand.work.action.PerformAction;
import com.dwinovo.chiikawa.entity.brain.errand.work.action.VisitAction;
import com.dwinovo.chiikawa.init.InitTag;
import com.dwinovo.chiikawa.testing.TestAnchors;
import com.dwinovo.chiikawa.testing.TestErrands;
import com.dwinovo.chiikawa.testing.TestTags;
import com.dwinovo.chiikawa.testing.TestView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * What a pet offers to do with the pastimes that are one character's own, for the reason that makes them its own: that each is
 * offered to the character it is for, when its reason is there, and not otherwise, and what each sets out to do.
 */
class CharacterPastimeOfferTest {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
        ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("dimension")), ResourceLocation.withDefaultNamespace("overworld"));
    private static final GlobalPos HOME = GlobalPos.of(OVERWORLD, new BlockPos(0, 64, 0));
    private static final UUID SOMEONE = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID A_CAKE = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

    /** An allay is a pet to a pastime that looks for one, and a cake is something sweet, as the errand fixtures have them. */
    @BeforeAll
    static void load() {
        TestErrands.load();
        TestTags.bind(EntityType.ALLAY.builtInRegistryHolder(), InitTag.PETS);
        TestTags.bind(Items.CAKE.builtInRegistryHolder(), InitTag.SWEETS);
    }

    private static TestView pet(ResourceLocation character) {
        return TestView.at(HOME, TestAnchors.free(HOME)).species(character);
    }

    private static GlobalPos east(int blocks) {
        return GlobalPos.of(OVERWORLD, HOME.pos().east(blocks));
    }

    private static Surroundings withAPetNear() {
        return new Surroundings(Optional.empty(), List.of(new Sighting<>(SOMEONE, east(3).pos(), EntityType.ALLAY)), List.of(), false);
    }

    private static Surroundings withACakeAbout() {
        return new Surroundings(Optional.empty(), List.of(), List.of(new Sighting<>(A_CAKE, east(4).pos(), Items.CAKE)), false);
    }

    private static String whyNot(ResourceLocation errand, TestView view) {
        return TestErrands.whyNoOffer(errand, view).reasonKey();
    }

    @Test
    void chiikawaCowersInAStormOrNearACreeperAboutToGoOffAndNotInAShower() {
        assertInstanceOf(PerformAction.Performing.class,
            TestErrands.planOf(CharacterPastimeData.COWER, pet(PetCharacters.CHIIKAWA).weather(Weather.THUNDER, true)));
        assertInstanceOf(PerformAction.Performing.class, TestErrands.planOf(CharacterPastimeData.COWER, pet(PetCharacters.CHIIKAWA)
            .surroundings(new Surroundings(Optional.empty(), List.of(), List.of(), true))));
        assertEquals("intent.chiikawa.fail.no_occasion",
            whyNot(CharacterPastimeData.COWER, pet(PetCharacters.CHIIKAWA).weather(Weather.RAIN, true)));
        assertEquals("intent.chiikawa.fail.no_occasion", whyNot(CharacterPastimeData.COWER, pet(PetCharacters.CHIIKAWA)));
    }

    @Test
    void onlyChiikawaCowers() {
        for (ResourceLocation character : PetCharacters.all()) {
            if (!character.equals(PetCharacters.CHIIKAWA)) {
                assertEquals("intent.chiikawa.fail.no_occasion",
                    whyNot(CharacterPastimeData.COWER, pet(character).weather(Weather.THUNDER, true)), character.toString());
            }
        }
    }

    @Test
    void usagiHasItsFitsForNoReasonAndNoOneElseHasThem() {
        assertInstanceOf(PerformAction.Performing.class, TestErrands.planOf(CharacterPastimeData.FRENZY, pet(PetCharacters.USAGI)));
        assertEquals("intent.chiikawa.fail.no_occasion", whyNot(CharacterPastimeData.FRENZY, pet(PetCharacters.CHIIKAWA)));
    }

    @Test
    void momongaAsksForPraiseWhenThereIsAPetToCopyAndAnOwnerToAsk() {
        TestView ready = pet(PetCharacters.MOMONGA).surroundings(withAPetNear()).ownerPos(east(6));

        assertInstanceOf(AskPraiseAction.Asking.class, TestErrands.planOf(CharacterPastimeData.ASK_PRAISE, ready));
        assertEquals("intent.chiikawa.fail.no_occasion", whyNot(CharacterPastimeData.ASK_PRAISE,
            pet(PetCharacters.MOMONGA).ownerPos(east(6))), "no pet to copy");
        assertEquals("intent.chiikawa.fail.no_occasion", whyNot(CharacterPastimeData.ASK_PRAISE,
            pet(PetCharacters.MOMONGA).surroundings(withAPetNear())), "no owner to ask");
        assertEquals("intent.chiikawa.fail.no_occasion", whyNot(CharacterPastimeData.ASK_PRAISE,
            pet(PetCharacters.MOMONGA).surroundings(withAPetNear()).ownerPos(east(20))), "the owner is too far to ask");
    }

    @Test
    void momongaCopiesWhatTheNearestPetIsDoingAndFallsBackOnBeingCute() {
        TestView ready = pet(PetCharacters.MOMONGA).surroundings(withAPetNear()).ownerPos(east(6));

        AskPraiseAction.Asking alone = (AskPraiseAction.Asking) TestErrands.planOf(CharacterPastimeData.ASK_PRAISE, ready);
        AskPraiseAction.Asking copying = (AskPraiseAction.Asking) TestErrands.planOf(CharacterPastimeData.ASK_PRAISE,
            ready.neighbourDoing("clap"));

        assertEquals("cute", alone.imitate());
        assertEquals("clap,cute", copying.imitate());
        assertEquals(Optional.of(east(6)), copying.destination(ready));
    }

    @Test
    void momongaStopsAskingWhenItsOwnerIsGone() {
        TestView ready = pet(PetCharacters.MOMONGA).surroundings(withAPetNear()).ownerPos(east(6));
        var plan = TestErrands.planOf(CharacterPastimeData.ASK_PRAISE, ready);

        assertTrue(TestErrands.stillWanted(CharacterPastimeData.ASK_PRAISE, plan, ready).ok());
        assertEquals("intent.chiikawa.fail.owner_unavailable", TestErrands.stillWanted(CharacterPastimeData.ASK_PRAISE, plan,
            pet(PetCharacters.MOMONGA)).reasonKey());
    }

    @Test
    void kurimanjuHasItsDrinkAsTheDayEndsAndAtNoOtherTime() {
        assertInstanceOf(PerformAction.Performing.class,
            TestErrands.planOf(CharacterPastimeData.EVENING_DRINK, pet(PetCharacters.KURIMANJU).phase(DayPhase.EVENING)));
        for (DayPhase phase : new DayPhase[] {DayPhase.MORNING, DayPhase.DAY, DayPhase.NIGHT}) {
            assertEquals("intent.chiikawa.fail.no_occasion",
                whyNot(CharacterPastimeData.EVENING_DRINK, pet(PetCharacters.KURIMANJU).phase(phase)), phase::name);
        }
    }

    @Test
    void kurimanjuBringsAnOwnerWhoHasFinishedTheirWorkACoffee() {
        TestView done = pet(PetCharacters.KURIMANJU).ownerPos(east(6)).ownerDoneWithWork();

        VisitAction.Visiting plan = (VisitAction.Visiting) TestErrands.planOf(CharacterPastimeData.COFFEE_FOR_A_WORKER, done);

        assertInstanceOf(Goal.Owner.class, plan.goal());
        assertEquals("intent.chiikawa.fail.no_occasion",
            whyNot(CharacterPastimeData.COFFEE_FOR_A_WORKER, pet(PetCharacters.KURIMANJU).ownerPos(east(6))));
        assertEquals("intent.chiikawa.fail.no_occasion",
            whyNot(CharacterPastimeData.COFFEE_FOR_A_WORKER, pet(PetCharacters.CHIIKAWA).ownerPos(east(6)).ownerDoneWithWork()));
    }

    @Test
    void rakkoGoesToStareAtWhateverSweetItSees() {
        VisitAction.Visiting plan = (VisitAction.Visiting) TestErrands.planOf(CharacterPastimeData.SWEETS_STARE,
            pet(PetCharacters.RAKKO).surroundings(withACakeAbout()));

        assertEquals(new Goal.Place(east(4).pos()), plan.goal());
        assertEquals("intent.chiikawa.fail.no_occasion", whyNot(CharacterPastimeData.SWEETS_STARE, pet(PetCharacters.RAKKO)));
        assertEquals("intent.chiikawa.fail.no_occasion",
            whyNot(CharacterPastimeData.SWEETS_STARE, pet(PetCharacters.KURIMANJU).surroundings(withACakeAbout())));
    }

    @Test
    void rakkoPractisesWithItsSwordForNoReason() {
        assertInstanceOf(PerformAction.Performing.class, TestErrands.planOf(CharacterPastimeData.SWORD_PRACTICE, pet(PetCharacters.RAKKO)));
    }

    @Test
    void shisaGreetsAnOwnerWhoHasComeBackAndNoOneElseDoes() {
        VisitAction.Visiting plan = (VisitAction.Visiting) TestErrands.planOf(CharacterPastimeData.GREET_OWNER,
            pet(PetCharacters.SHISA).ownerPos(east(6)).ownerBackAgain());

        assertInstanceOf(Goal.Owner.class, plan.goal());
        assertEquals("intent.chiikawa.fail.no_occasion", whyNot(CharacterPastimeData.GREET_OWNER, pet(PetCharacters.SHISA).ownerPos(east(6))));
        assertEquals("intent.chiikawa.fail.no_occasion",
            whyNot(CharacterPastimeData.GREET_OWNER, pet(PetCharacters.USAGI).ownerPos(east(6)).ownerBackAgain()));
    }
}
