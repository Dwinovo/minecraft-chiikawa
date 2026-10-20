package com.dwinovo.chiikawa.testing;

import com.dwinovo.chiikawa.entity.brain.errand.inbox.InvitationDesk;
import com.dwinovo.chiikawa.entity.brain.errand.view.SocialIdea;
import com.dwinovo.chiikawa.social.PetInteraction;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;

/** A scene for tests that only need there to be one: two pets, one pose each, and a walk of a block to the other. */
public final class TestScenes {
    /** The pet the scene is played with. */
    public static final UUID PARTNER = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    /** The scene's id. */
    public static final ResourceLocation SCENE = ResourceLocation.fromNamespaceAndPath("chiikawa", "test_scene");

    private static final PetInteraction.Side SIDE = new PetInteraction.Side(List.of(), Optional.of("pose"), Optional.empty(),
        Optional.empty(), Optional.empty());

    private TestScenes() {
    }

    /** @return a scene played with a pet that is pottering about, handing nothing over */
    public static PetInteraction scene() {
        return new PetInteraction(List.of(SIDE), List.of(SIDE), PetInteraction.PartnerState.IDLE, Optional.empty(),
            Optional.empty(), 0, 8.0, 1, 300, 80, 12000, 0.05F);
    }

    /** @return the idea of playing it with {@link #PARTNER}, standing there */
    public static SocialIdea idea(GlobalPos partner) {
        return new SocialIdea(SCENE, scene(), SIDE, 0, PARTNER, partner);
    }

    /** @return the invitation of {@link #PARTNER} to play it, which has no end */
    public static InvitationDesk.Engagement asked() {
        return new InvitationDesk.Engagement(PARTNER, SCENE, 0, Long.MAX_VALUE);
    }
}
