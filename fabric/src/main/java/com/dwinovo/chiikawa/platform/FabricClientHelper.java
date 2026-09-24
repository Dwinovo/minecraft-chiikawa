package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.platform.services.IClientHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;

public class FabricClientHelper implements IClientHelper {
    @Override
    public void submitPicture(GuiGraphicsExtractor graphics, PictureInPictureRenderState picture) {
        // Fabric opens the screen's own list of what it draws; the picture goes in as the
        // game's own pictures do.
        graphics.guiRenderState.addPicturesInPictureState(picture);
    }
}
