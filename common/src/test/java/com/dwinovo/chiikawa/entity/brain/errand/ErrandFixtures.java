package com.dwinovo.chiikawa.entity.brain.errand;

import com.dwinovo.chiikawa.camera.PhotoOutlook;
import com.dwinovo.chiikawa.data.WorkData;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.entity.brain.errand.view.DayPhase;
import com.dwinovo.chiikawa.entity.brain.errand.view.AttackTarget;
import com.dwinovo.chiikawa.entity.brain.errand.view.PerceivedTargets;
import com.dwinovo.chiikawa.entity.brain.errand.view.PickableItem;
import com.dwinovo.chiikawa.photo.PhotoSubject;
import com.dwinovo.chiikawa.testing.TestAnchors;
import com.dwinovo.chiikawa.testing.TestPhotos;
import com.dwinovo.chiikawa.testing.TestView;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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

    private static final Map<ResourceLocation, TestView> BY_ERRAND = Map.ofEntries(
        Map.entry(ErrandIds.FOLLOW_OWNER, TestView.at(east(9), TestAnchors.following(HOME))),
        Map.entry(ErrandIds.STAY, free()),
        Map.entry(ErrandIds.WANDER, free()),
        Map.entry(ErrandIds.PICK_UP_ITEM, free().targets(targets(Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(new PickableItem(ITEM, east(2)))))),
        Map.entry(ErrandIds.HARVEST, free().targets(targets(Optional.empty(), Optional.of(east(2)), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()))),
        Map.entry(ErrandIds.PLANT, free().targets(targets(Optional.empty(), Optional.empty(), Optional.of(east(2)),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()))),
        Map.entry(ErrandIds.DELIVER, free().targets(targets(Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of(east(2)), Optional.empty(), Optional.empty(), Optional.empty()))),
        Map.entry(ErrandIds.WEED, free().ownership(PetOwnership.WILD).targets(targets(Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.of(east(2)), Optional.empty(), Optional.empty()))),
        Map.entry(ErrandIds.PICK_MUSHROOM, free().ownership(PetOwnership.WILD).phase(DayPhase.NIGHT).targets(targets(
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(east(2)),
            Optional.empty()))),
        Map.entry(WorkData.MELEE, free().targets(targets(Optional.of(new AttackTarget(FOE, east(2))), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()))),
        Map.entry(WorkData.RANGED, free().armedWithArrows().targets(targets(Optional.of(new AttackTarget(FOE, east(2))), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()))),
        Map.entry(WorkData.PLAY_MUSIC, free().withSong()),
        Map.entry(ErrandIds.TAKE_TASK, free().offeringBoard(east(2))),
        Map.entry(ErrandIds.SHOP, free().shopWorthVisiting(east(2))),
        Map.entry(ErrandIds.GIFT_OWNER, free().withPresent().ownerPos(east(2))),
        Map.entry(ErrandIds.SOCIALIZE, free().socialPartner(east(2))),
        Map.entry(ErrandIds.COOPERATE, free().askedToPlay()),
        Map.entry(ErrandIds.TAKE_EXAM, free().examDesk(east(2))),
        Map.entry(ErrandIds.CHECK_RESULTS, free().resultsDesk(east(2))),
        Map.entry(ErrandIds.ANSWER_WHISTLE, free().ownership(PetOwnership.WILD).whistleHeard(east(2))),
        Map.entry(ErrandIds.TAKE_PHOTO, free().creature(FRIEND, east(4)).photo(readyToShoot())));

    private ErrandFixtures() {
    }

    /** @return a pet that has something to offer this errand, or nothing if there is no fixture for it */
    public static Optional<TestView> of(ResourceLocation errand) {
        return Optional.ofNullable(BY_ERRAND.get(errand));
    }

    private static GlobalPos east(int blocks) {
        return GlobalPos.of(OVERWORLD, HOME.pos().east(blocks));
    }

    private static TestView free() {
        return TestView.at(HOME, TestAnchors.free(HOME));
    }

    private static PerceivedTargets targets(Optional<AttackTarget> attack, Optional<GlobalPos> harvest, Optional<GlobalPos> plant,
            Optional<GlobalPos> container, Optional<GlobalPos> weed, Optional<GlobalPos> mushroom, Optional<PickableItem> item) {
        return new PerceivedTargets(attack, harvest, plant, container, weed, mushroom, item);
    }

    private static PhotoOutlook readyToShoot() {
        return TestPhotos.ready(TestPhotos.of(PhotoSubject.FRIEND, FRIEND));
    }
}
