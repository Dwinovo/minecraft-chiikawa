package com.dwinovo.chiikawa.anim.state;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/** Short-lived emotional reactions layered above the base loop. */
public enum PetReaction implements StringRepresentable {
    NONE(0),
    HAPPY(1, "happy"),
    HURT(2, "hurt"),
    CONFUSED(3, "scratch_head", "confused"),
    REVIVE(4, "revive", "happy");

    /** By lower-case name, as data names a reaction a pet shows. */
    public static final Codec<PetReaction> CODEC = StringRepresentable.fromEnum(PetReaction::values);

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

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
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
