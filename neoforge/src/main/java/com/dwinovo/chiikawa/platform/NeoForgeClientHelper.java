package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.platform.services.IClientHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;

public class NeoForgeClientHelper implements IClientHelper {
    @Override
    public void submitPicture(GuiGraphicsExtractor graphics, PictureInPictureRenderState picture) {
        graphics.submitPictureInPictureRenderState(picture);
    }
}
