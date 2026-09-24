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
 */
public record ShownDuring(List<String> animations) implements BoneVisibilityRule {
    public ShownDuring {
        animations = List.copyOf(animations);
    }

    public static ShownDuring any(String... animations) {
        return new ShownDuring(List.of(animations));
    }

    @Override
    public boolean isVisible(ChiikawaRenderState state, PetAnimContext ctx) {
        for (String animation : animations) {
            if (ChiikawaEntityRenderer.isAnyControllerPlaying(state, animation)) {
                return true;
            }
        }
        return false;
    }
}
