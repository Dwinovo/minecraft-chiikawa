package com.dwinovo.chiikawa.entity.brain.errand;

/**
 * One way a pet could go about an errand now. An errand with several offers has variants, and the
 * pet takes one in proportion to the weights.
 *
 * @param plan what the pet would set out to do
 * @param weight how likely this way is against the errand's other offers; more than 0
 * @param <P> the shape of the errand's plans
 */
public record Offer<P extends Plan>(P plan, int weight) {
    public Offer {
        if (weight <= 0) {
            throw new IllegalArgumentException("An offer needs a weight above 0");
        }
    }

    /** @return the only way to go about it */
    public static <P extends Plan> Offer<P> of(P plan) {
        return new Offer<>(plan, 1);
    }
}
