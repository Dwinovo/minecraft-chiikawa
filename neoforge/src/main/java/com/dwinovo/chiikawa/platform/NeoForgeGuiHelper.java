package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.platform.services.IGuiHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import org.jetbrains.annotations.Nullable;

public class NeoForgeGuiHelper implements IGuiHelper {
    @Override
    public void submitPicture(GuiGraphics graphics, PictureInPictureRenderState picture) {
        graphics.submitPictureInPictureRenderState(picture);
    }

    @Override
    public @Nullable ScreenRectangle scissorArea(GuiGraphics graphics) {
        return graphics.peekScissorStack();
    }
}
