package com.dwinovo.chiikawa.entity.brain.errand.decide;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * An errand a pet could start now, as the decision sees it: the priority it has in data and the
 * score the pet gives it, bonuses and personality already weighed in.
 *
 * @param errand the errand's id
 * @param priority its priority
 * @param score what the pet makes of it among errands of its priority
 * @param offerWeights one weight for each way the pet could go about it; the errand is started in one
 *                     of them, drawn in proportion to these. A weight of 0 is never drawn
 */
public record Scored(ResourceLocation errand, int priority, float score, List<Integer> offerWeights) {
    public Scored {
        offerWeights = List.copyOf(offerWeights);
        if (offerWeights.isEmpty() || offerWeights.stream().anyMatch(weight -> weight < 0)
                || offerWeights.stream().mapToInt(Integer::intValue).sum() <= 0) {
            throw new IllegalArgumentException("An errand needs an offer with a weight to be a candidate: " + errand);
        }
    }
}
