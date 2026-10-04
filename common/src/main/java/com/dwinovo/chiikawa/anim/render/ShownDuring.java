package com.dwinovo.chiikawa.anim.render;

import com.dwinovo.chiikawa.anim.state.PetAnimContext;
import java.util.List;

/**
 * A bone that is there only while one of the named animations plays, such as a part of a
 * face that an expression brings out: Chiikawa's tears while it cries, its happy eyes while
 * it is pleased. The animation hides whatever the part stands in for by scaling it to
 * nothing; this brings the part itself out, since a bone cannot start hidden in a Bedrock
 * model. Which parts a pet has, and for which of its faces, is up to each pet's renderer,
 * as each face is its own.
 *
 * @param animations the animations that show the bone, by short name
 * @param unless animations that put it away again while they play over one of those
 */
public record ShownDuring(List<String> animations, List<String> unless) implements BoneVisibilityRule {
    public ShownDuring {
        animations = List.copyOf(animations);
        unless = List.copyOf(unless);
    }

    public static ShownDuring any(String... animations) {
        return new ShownDuring(List.of(animations), List.of());
    }

    /** @return the same, put away while any of {@code animations} plays */
    public ShownDuring unless(String... animations) {
        return new ShownDuring(this.animations, List.of(animations));
    }

    /** @return whether the bone shows in a pose made of {@code animation} alone */
    public boolean shownIn(String animation) {
        return animations.contains(animation) && !unless.contains(animation);
    }

    @Override
    public boolean isVisible(ChiikawaRenderState state, PetAnimContext ctx) {
        for (String animation : unless) {
            if (ChiikawaEntityRenderer.isAnyControllerPlaying(state, animation)) {
                return false;
            }
        }
        for (String animation : animations) {
            if (ChiikawaEntityRenderer.isAnyControllerPlaying(state, animation)) {
                return true;
            }
        }
        return false;
    }
}
