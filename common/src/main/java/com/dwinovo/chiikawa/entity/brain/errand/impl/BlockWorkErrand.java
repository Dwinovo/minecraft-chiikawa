package com.dwinovo.chiikawa.entity.brain.errand.impl;

import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandType;
import com.dwinovo.chiikawa.entity.brain.errand.Offer;
import com.dwinovo.chiikawa.entity.brain.errand.Offers;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestStore;
import com.dwinovo.chiikawa.entity.brain.errand.rest.SubjectRests;
import com.dwinovo.chiikawa.entity.brain.errand.run.Routine;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.step.Goal;
import com.dwinovo.chiikawa.entity.brain.errand.step.Harvest;
import com.dwinovo.chiikawa.entity.brain.errand.step.Plant;
import com.dwinovo.chiikawa.entity.brain.errand.step.WalkTo;
import com.dwinovo.chiikawa.entity.brain.errand.view.PerceivedTargets;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.task.PetWorkCounters;
import com.dwinovo.chiikawa.utils.Utils;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Walks to the block the farmer sensor remembers and does one thing to it: reaps a ripe crop, plants a seed in bare
 * soil, pulls up a weed, picks a mushroom. The four are one errand type made four times, differing in which block the
 * sensor remembers for them, what is done once the pet is there, the work it counts towards a slip, and the
 * conditions on top; their numbers are in the same shape and in a file each.
 *
 * <p>The block is where it goes, so it starts only on a block within the anchor's reach and keeps going while the block
 * stays within the leash. It keeps to the block it set off for: if the sensor moves on to another because that one is
 * no longer worth working, the errand is over. A pet that could not get its work done, because it could not find a way
 * to the block or the block was not as it seemed on arrival, leaves that block alone for a while and not other blocks.
 */
public final class BlockWorkErrand implements ErrandType<BlockWorkErrand.Work, ErrandTunings.BlockWork> {
    /** The brain's own mover takes the pet right up to the block; the walk is done when the pet is near enough to work it. */
    private static final int RIGHT_UP_TO_IT = 0;

    /**
     * What the pet has set out to work.
     *
     * @param where the block
     */
    public record Work(GlobalPos where) implements Plan {
        @Override
        public Optional<GlobalPos> destination(PetView view) {
            return Optional.of(where);
        }

        @Override
        public Optional<String> subject() {
            return Optional.of(SubjectRests.subject(where.pos()));
        }
    }

    private final ResourceLocation id;
    private final ErrandCategory category;
    private final Function<PerceivedTargets, Optional<GlobalPos>> remembered;
    private final String missingReason;
    private final List<Requirement> requirements;
    private final Optional<ResourceLocation> workCounter;
    private final BiFunction<BlockPos, ErrandTunings.BlockWork, Step> action;

    /**
     * @param remembered which block the sensor remembers for this work
     * @param missingReason failure reason when there is none
     * @param requirements further conditions, checked in order before the block
     * @param workCounter the work counter its steps report, see {@link ErrandType#workCounter}
     * @param action what is done to the block once the pet is there, given the block and the errand's tuning
     */
    private BlockWorkErrand(ResourceLocation id, ErrandCategory category,
            Function<PerceivedTargets, Optional<GlobalPos>> remembered, String missingReason, List<Requirement> requirements,
            Optional<ResourceLocation> workCounter,
            BiFunction<BlockPos, ErrandTunings.BlockWork, Step> action) {
        this.id = id;
        this.category = category;
        this.remembered = remembered;
        this.missingReason = missingReason;
        this.requirements = List.copyOf(requirements);
        this.workCounter = workCounter;
        this.action = action;
    }

    /** @return reaping the ripe crop the sensor remembers */
    public static BlockWorkErrand harvesting() {
        return new BlockWorkErrand(ErrandIds.HARVEST, ErrandCategory.WORK, PerceivedTargets::harvest, "no_crop", List.of(),
            Optional.empty(), (block, tuning) -> new Harvest(block, Utils::canHarvesr, Optional.empty(), tuning.toolWear()));
    }

    /** @return planting a seed in the bare soil the sensor remembers */
    public static BlockWorkErrand planting() {
        return new BlockWorkErrand(ErrandIds.PLANT, ErrandCategory.WORK, PerceivedTargets::plant, "no_farmland", List.of(),
            Optional.empty(), (block, tuning) -> new Plant(block));
    }

    /** @return pulling up the weed the sensor remembers */
    public static BlockWorkErrand weeding() {
        Optional<ResourceLocation> counter = Optional.of(PetWorkCounters.WEED);
        return new BlockWorkErrand(ErrandIds.WEED, ErrandCategory.FORAGE, PerceivedTargets::weed, "no_weed",
            List.of(Requirement.wildOrCarrying(PetWorkCounters.WEED)), counter,
            (block, tuning) -> new Harvest(block, Utils::isWeed, counter, tuning.toolWear()));
    }

    /** @return picking the mushroom the sensor remembers, at night */
    public static BlockWorkErrand pickingMushrooms() {
        Optional<ResourceLocation> counter = Optional.of(PetWorkCounters.PICK_MUSHROOM);
        return new BlockWorkErrand(ErrandIds.PICK_MUSHROOM, ErrandCategory.FORAGE, PerceivedTargets::mushroom, "no_mushroom",
            List.of(Requirement.wildOrCarrying(PetWorkCounters.PICK_MUSHROOM), Requirement.AT_NIGHT), counter,
            (block, tuning) -> new Harvest(block, Utils::isMushroom, counter, tuning.toolWear()));
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public ErrandCategory category() {
        return category;
    }

    @Override
    public Codec<ErrandTunings.BlockWork> tuningCodec() {
        return ErrandTunings.BlockWork.CODEC;
    }

    @Override
    public Offers<Work> offers(PetView view, ErrandTunings.BlockWork tuning) {
        Check required = required(view);
        if (!required.ok()) {
            return Offers.none(required);
        }
        return remembered.apply(view.targets())
            .map(where -> Offers.of(Offer.of(new Work(where))))
            .orElseGet(() -> Offers.none(Check.fail(missingReason)));
    }

    /** Goes on while the conditions to start hold and the sensor still remembers the block the pet set off for. */
    @Override
    public Check stillWanted(Work plan, PetView view, ErrandTunings.BlockWork tuning) {
        Check required = required(view);
        if (!required.ok()) {
            return required;
        }
        return remembered.apply(view.targets()).filter(plan.where()::equals).isPresent() ? Check.OK : Check.fail(missingReason);
    }

    private Check required(PetView view) {
        for (Requirement requirement : requirements) {
            Check check = requirement.check(view);
            if (!check.ok()) {
                return check;
            }
        }
        return Check.OK;
    }

    @Override
    public Routine routine(Work plan, ErrandSettings<ErrandTunings.BlockWork> settings) {
        ErrandTunings.BlockWork tuning = settings.tuning();
        BlockPos block = plan.where().pos();
        return Routine.of(
            Routine.step(() -> new WalkTo(new Goal.Place(block), tuning.speed(), RIGHT_UP_TO_IT, Math.sqrt(tuning.reachSqr()),
                settings.maxTicks(), tuning.noWayTicks())),
            Routine.step(() -> action.apply(block, tuning)));
    }

    @Override
    public Optional<ResourceLocation> workCounter() {
        return workCounter;
    }

    @Override
    public RestStore.Kind restStore() {
        return RestStore.subjects();
    }
}
