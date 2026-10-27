package com.dwinovo.chiikawa.entity.brain.errand.remark;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.LackKey;
import com.dwinovo.chiikawa.entity.brain.errand.decide.WhyNot;
import com.dwinovo.chiikawa.entity.brain.errand.run.Aside;
import com.dwinovo.chiikawa.entity.brain.errand.view.DayPhase;
import com.dwinovo.chiikawa.init.InitMemory;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/**
 * A pet making the remarks that are due, beside whatever it is doing: the one place that decides one is, and the only
 * writer of what a pet has remarked on, see {@link RemarkRests}. A remark is an aside, so it says and gestures and
 * nothing more: it never takes the pet's body from its errand. Whether the pet speaks at all is its voice's, which the
 * world's chatter setting turns down or off for remarks as for every other line; a remark that was not said is not
 * rested, and is tried again.
 */
public final class Remarks {
    /** How often a pet looks over its remarks: they are asides, and a couple of seconds late is not late. */
    public static final int INTERVAL = 20;

    private Remarks() {
    }

    /**
     * Makes the remark that is due, if one is. What a remark is about is for its cues to say: one about being short of
     * something is not made by a pet that is at work, or has none to do.
     *
     * @param aside what the pet may do beside its errand
     * @param whyNot why the pet is not at work, if it is not
     */
    public static void tick(AbstractPet pet, Aside aside, Optional<WhyNot> whyNot) {
        if (!(pet.level() instanceof ServerLevel level)) {
            return;
        }
        RemarkCue.Situation situation = new RemarkCue.Situation(whyNot, level.getGameTime(), ownerDistance(pet),
            DayPhase.in(level));
        RemarkRests rests = pet.getBrain().getMemory(InitMemory.REMARK_RESTS.get()).orElse(RemarkRests.NONE);
        remark(PetRemarks.all(), BuiltInRegistries.ENTITY_TYPE.getKey(pet.getType()), situation, rests, aside)
            .ifPresent(next -> pet.getBrain().setMemory(InitMemory.REMARK_RESTS.get(), next));
    }

    /**
     * Makes the first remark, by id, that is not resting, that holds and that the character has a part in, if it says it.
     *
     * @param remarks every remark there is
     * @param character the pet's entity type id
     * @return the rests with this remark's set, or nothing when none was made
     */
    static Optional<RemarkRests> remark(Map<ResourceLocation, PetRemark> remarks, ResourceLocation character,
            RemarkCue.Situation situation, RemarkRests rests, Aside aside) {
        Optional<String> subject = situation.whyNot().flatMap(WhyNot::lack).map(LackKey::translationKey);
        for (Map.Entry<ResourceLocation, PetRemark> entry : remarks.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString))).toList()) {
            PetRemark remark = entry.getValue();
            if (rests.resting(entry.getKey(), situation.now()) || !remark.holds(situation)) {
                continue;
            }
            Optional<PetRemark.Part> part = remark.partOf(character);
            if (part.isPresent() && aside.say(part.get().moment(), subject)) {
                part.get().gesture().ifPresent(aside::gesture);
                return Optional.of(rests.with(entry.getKey(), remark.cooldownTicks(), situation.now()));
            }
        }
        return Optional.empty();
    }

    /** @return how far off the pet's owner is, while the owner is a player in the pet's level */
    private static OptionalDouble ownerDistance(AbstractPet pet) {
        return pet.getOwner() instanceof Player owner && owner.level() == pet.level()
            ? OptionalDouble.of(owner.distanceTo(pet)) : OptionalDouble.empty();
    }
}
