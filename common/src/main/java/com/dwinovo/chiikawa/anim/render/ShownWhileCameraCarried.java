package com.dwinovo.chiikawa.anim.render;

import com.dwinovo.chiikawa.anim.state.PetAnimContext;
import com.dwinovo.chiikawa.camera.CameraCarry;
import java.util.List;

/**
 * The camera bone, there for as long as the pet has a camera with it, wherever in its backpack:
 * it hangs at Hachiware's chest from the strap round its neck. It is put away while one of the
 * named animations plays over it, which has the pet's hands or its chest for something else.
 *
 * @param unless animations that put the bone away while they play
 */
public record ShownWhileCameraCarried(List<String> unless) implements BoneVisibilityRule {
    public ShownWhileCameraCarried {
        unless = List.copyOf(unless);
    }

    @Override
    public boolean isVisible(ChiikawaRenderState state, PetAnimContext ctx) {
        if (!(state.get(PetData.CAMERA_CARRY) instanceof CameraCarry carry) || !carry.onChest()) {
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
