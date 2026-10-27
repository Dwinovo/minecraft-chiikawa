package com.dwinovo.chiikawa.entity.brain.errand;

import com.dwinovo.chiikawa.camera.PhotoOutlook;
import com.dwinovo.chiikawa.data.CharacterPastimeData;
import com.dwinovo.chiikawa.data.PastimeData;
import com.dwinovo.chiikawa.data.WorkData;
import com.dwinovo.chiikawa.entity.PetCharacters;
import com.dwinovo.chiikawa.entity.brain.combat.Arms;
import com.dwinovo.chiikawa.entity.brain.combat.Ground;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.entity.brain.errand.view.DayPhase;
import com.dwinovo.chiikawa.entity.brain.errand.view.PerceivedTargets;
import com.dwinovo.chiikawa.entity.brain.errand.view.PickableItem;
import com.dwinovo.chiikawa.entity.brain.errand.view.Surroundings;
import com.dwinovo.chiikawa.entity.brain.errand.view.Surroundings.Sighting;
import com.dwinovo.chiikawa.entity.brain.errand.view.Weather;
import com.dwinovo.chiikawa.init.InitTag;
import com.dwinovo.chiikawa.photo.PhotoSubject;
import com.dwinovo.chiikawa.testing.TestAnchors;
import com.dwinovo.chiikawa.testing.TestPhotos;
import com.dwinovo.chiikawa.testing.TestTags;
import com.dwinovo.chiikawa.testing.TestTargets;
import com.dwinovo.chiikawa.testing.TestView;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * For each errand, a pet that has something to offer it: the view the contract tests ask every errand
 * about. A new errand without one fails the registry contract, so none is added without saying what
 * it takes for it to offer anything.
 */
public final class ErrandFixtures {
    private static final ResourceKey<Level> OVERWORLD = ResourceKey.create(
        ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("dimension")),
        ResourceLocation.withDefaultNamespace("overworld"));
    private static final GlobalPos HOME = GlobalPos.of(OVERWORLD, new BlockPos(0, 64, 0));
    private static final UUID ITEM = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID FOE = UUID.fromString("00000000-0000-0000-0000-0000000000a3");
    private static final UUID FRIEND = UUID.fromString("00000000-0000-0000-0000-0000000000a2");

    static {
        // The tags of a pet and of something sweet are the game's to bind when a world loads, and there is none here: an allay
        // is a pet to a pastime that looks for one, and a cake is something sweet.
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        TestTags.bind(EntityType.ALLAY.builtInRegistryHolder(), InitTag.PETS);
        TestTags.bind(Items.CAKE.builtInRegistryHolder(), InitTag.SWEETS);
    }

    private static final Map<ResourceLocation, TestView> BY_ERRAND = Map.ofEntries(
        Map.entry(ErrandIds.FOLLOW_OWNER, TestView.at(east(9), TestAnchors.following(HOME))),
        Map.entry(ErrandIds.STAY, free()),
        Map.entry(ErrandIds.WANDER, free()),
        Map.entry(ErrandIds.PICK_UP_ITEM, free().targets(TestTargets.of(Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(new PickableItem(ITEM, east(2)))))),
        Map.entry(WorkData.HARVEST, free().targets(TestTargets.of(Optional.empty(), Optional.of(east(2)), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()))),
        Map.entry(WorkData.PLANT, free().targets(TestTargets.of(Optional.empty(), Optional.empty(), Optional.of(east(2)),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()))),
        Map.entry(WorkData.DELIVER, free().targets(TestTargets.of(Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of(east(2)), Optional.empty(), Optional.empty(), Optional.empty()))),
        Map.entry(WorkData.WEED, free().ownership(PetOwnership.WILD).targets(TestTargets.of(Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.of(east(2)), Optional.empty(), Optional.empty()))),
        Map.entry(WorkData.PICK_MUSHROOM, free().ownership(PetOwnership.WILD).phase(DayPhase.NIGHT).targets(TestTargets.of(
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(east(2)),
            Optional.empty()))),
        Map.entry(WorkData.MELEE, free().targets(foe(Ground.SELF, Ground.OWNER, Ground.NEARBY))),
        Map.entry(WorkData.RANGED, free().armedWithArrows().targets(foe(Ground.SELF, Ground.OWNER, Ground.NEARBY))),
        Map.entry(WorkData.HUNT_MELEE, free().targets(foe(Ground.HUNT))),
        Map.entry(WorkData.HUNT_RANGED, free().armedWithArrows().targets(foe(Ground.HUNT))),
        Map.entry(ErrandIds.DEFEND_SELF, free().targets(foe(Ground.SELF)).arms(new Arms(false, Optional.of(new Arms.Weapon(2, false))))),
        Map.entry(ErrandIds.FLEE, free().attackedBy(FOE, east(3))),
        Map.entry(WorkData.PLAY_MUSIC, free().withSong()),
        Map.entry(ErrandIds.TAKE_TASK, free().offeringBoard(east(2))),
        Map.entry(ErrandIds.SHOP, free().shopWorthVisiting(east(2))),
        Map.entry(ErrandIds.OFFER_GIFT, free().withPresent().ownerPos(east(2))),
        Map.entry(ErrandIds.GIFT_OWNER, free().holdingGift().ownerPos(east(2))),
        Map.entry(ErrandIds.SOCIALIZE, free().socialPartner(east(2))),
        Map.entry(ErrandIds.COOPERATE, free().askedToPlay()),
        Map.entry(ErrandIds.TAKE_EXAM, free().examDesk(east(2))),
        Map.entry(ErrandIds.CHECK_RESULTS, free().resultsDesk(east(2))),
        Map.entry(ErrandIds.ANSWER_WHISTLE, free().ownership(PetOwnership.WILD).whistleHeard(east(2))),
        Map.entry(ErrandIds.TAKE_PHOTO, free().creature(FRIEND, east(4)).photo(readyToShoot())),
        Map.entry(ErrandIds.SLEEP, free().phase(DayPhase.NIGHT)),
        Map.entry(PastimeData.RAIN_SHELTER, free().species(PetCharacters.HACHIWARE).weather(Weather.RAIN, true)
            .surroundings(new Surroundings(Optional.of(east(3).pos()), List.of(), List.of(), false))),
        Map.entry(PastimeData.RAIN_PLAY, free().species(PetCharacters.USAGI).weather(Weather.RAIN, true)),
        Map.entry(PastimeData.WATCH_OWNER_WORK, free().ownerPos(east(5)).ownerAtWork()),
        Map.entry(CharacterPastimeData.COWER, free().species(PetCharacters.CHIIKAWA).weather(Weather.THUNDER, true)),
        Map.entry(CharacterPastimeData.FRENZY, free().species(PetCharacters.USAGI)),
        Map.entry(CharacterPastimeData.ASK_PRAISE, free().species(PetCharacters.MOMONGA).ownerPos(east(5))
            .surroundings(new Surroundings(Optional.empty(), List.of(new Sighting<>(FRIEND, east(3).pos(), EntityType.ALLAY)), List.of(),
                false))),
        Map.entry(CharacterPastimeData.EVENING_DRINK, free().species(PetCharacters.KURIMANJU).phase(DayPhase.EVENING)),
        Map.entry(CharacterPastimeData.COFFEE_FOR_A_WORKER,
            free().species(PetCharacters.KURIMANJU).ownerPos(east(5)).ownerDoneWithWork()),
        Map.entry(CharacterPastimeData.SWEETS_STARE, free().species(PetCharacters.RAKKO)
            .surroundings(new Surroundings(Optional.empty(), List.of(), List.of(new Sighting<>(ITEM, east(3).pos(), Items.CAKE)),
                false))),
        Map.entry(CharacterPastimeData.SWORD_PRACTICE, free().species(PetCharacters.RAKKO)),
        Map.entry(CharacterPastimeData.GREET_OWNER, free().species(PetCharacters.SHISA).ownerPos(east(5)).ownerBackAgain()));

    private ErrandFixtures() {
    }

    /** @return a pet that has something to offer this errand, or nothing if there is no fixture for it */
    public static Optional<TestView> of(ResourceLocation errand) {
        return Optional.ofNullable(BY_ERRAND.get(errand));
    }

    private static GlobalPos east(int blocks) {
        return GlobalPos.of(OVERWORLD, HOME.pos().east(blocks));
    }

    /** A foe two blocks off that the pet fights for these reasons. */
    private static PerceivedTargets foe(Ground... grounds) {
        return TestTargets.of(Optional.of(TestTargets.foe(FOE, east(2), grounds)), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    private static TestView free() {
        return TestView.at(HOME, TestAnchors.free(HOME));
    }

    private static PhotoOutlook readyToShoot() {
        return TestPhotos.ready(TestPhotos.of(PhotoSubject.FRIEND, FRIEND));
    }
}
