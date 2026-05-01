package com.dwinovo.chiikawa.anim.state;

import java.util.List;

/** Short-lived emotional reactions layered above the base loop. */
public enum PetReaction {
    NONE(0),
    HAPPY(1, "happy"),
    HURT(2, "hurt"),
    CONFUSED(3, "scratch_head", "confused"),
    REVIVE(4, "revive", "happy");

    private final int networkId;
    private final List<String> animationCandidates;

    PetReaction(int networkId, String... animationCandidates) {
        this.networkId = networkId;
        this.animationCandidates = List.of(animationCandidates);
    }

    public int networkId() {
        return networkId;
    }

    public List<String> animationCandidates() {
        return animationCandidates;
    }

    public static PetReaction fromNetworkId(int id) {
        for (PetReaction reaction : values()) {
            if (reaction.networkId == id) {
                return reaction;
            }
        }
        return NONE;
    }
}
