package com.dwinovo.chiikawa.anim.render;

import com.dwinovo.chiikawa.anim.state.PetAnimContext;
import java.util.List;

/**
 * A bone that is there for as long as the pet has the thing with it, such as the camera that
 * hangs at Hachiware's chest from the strap round its neck, and that is put away while one of
 * the named animations plays over it, which has the pet's hands or its chest for something else.
 *
 * @param carried the extra of the render state that says whether the pet has the thing, a {@link Boolean}
 * @param unless animations that put the bone away while they play
 */
public record ShownWhileCarried(PetData carried, List<String> unless) implements BoneVisibilityRule {
    public ShownWhileCarried {
        unless = List.copyOf(unless);
    }

    @Override
    public boolean isVisible(ChiikawaRenderState state, PetAnimContext ctx) {
        if (!Boolean.TRUE.equals(state.get(carried))) {
            return false;
        }
        for (String animation : unless) {
            if (ChiikawaEntityRenderer.isAnyControllerPlaying(state, animation)) {
                return false;
            }
        }
        return true;
    }
}
