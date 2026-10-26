package com.dwinovo.chiikawa.entity.brain.errand.decide;

import com.dwinovo.chiikawa.entity.brain.errand.LackKey;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;

/**
 * Why a pet is not at work, as its last decision found: the work of its job that had nothing to offer, and what it
 * said was the matter, when none of that work could start. The one account the pet's head icon, the remark it makes of
 * what it is short of and its screen are all made from, so they cannot tell different stories. Immutable; a pet that
 * is at work, or has no work to do, has none.
 *
 * @param errand the work that has nothing to offer; of the work there is, the first that names what the pet is short of,
 *               and otherwise the first
 * @param reasonKey the translation key of what it said was the matter
 * @param lack what the pet is short of, if that is the matter
 * @param since the game time of the first decision that found this to be the matter, and not another
 */
public record WhyNot(ResourceLocation errand, String reasonKey, Optional<LackKey> lack, long since) {
    /**
     * @param trace how a decision went, over the priorities it looked at
     * @param work whether an errand is work of the pet's job
     * @param now the game time of the decision
     * @return why none of the work could start, or nothing when some could, when it was not looked at, or when there is none
     */
    public static Optional<WhyNot> of(DecisionTrace trace, Predicate<ResourceLocation> work, long now) {
        boolean startable = trace.lines().stream()
            .anyMatch(line -> line.verdict().equals(DecisionTrace.STARTABLE) && work.test(line.errand()));
        if (startable) {
            return Optional.empty();
        }
        List<DecisionTrace.Unmet> unmet = trace.unmetErrands().stream().filter(each -> work.test(each.errand())).toList();
        return unmet.stream().filter(each -> each.why().lack().isPresent()).findFirst()
            .or(() -> unmet.stream().findFirst())
            .map(each -> new WhyNot(each.errand(), each.why().reasonKey(), each.why().lack(), now));
    }

    /**
     * @param before what the pet's last decision found
     * @return what was found before when it was the same matter, so that how long it has lasted is not lost, and
     *         otherwise this
     */
    public WhyNot lastingFrom(Optional<WhyNot> before) {
        return before.filter(kept -> kept.errand.equals(errand) && kept.reasonKey.equals(reasonKey) && kept.lack.equals(lack))
            .orElse(this);
    }

    /** @return for how long, as of this game time, it has been the matter */
    public long lasted(long now) {
        return now - since;
    }
}
