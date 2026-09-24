package com.dwinovo.chiikawa.platform.services;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import org.jetbrains.annotations.Nullable;

/**
 * What a screen needs from the loader to hand the game a picture in picture of its own. The
 * game keeps a screen's render state to itself and lets in only the pictures it draws for
 * itself; each loader opens it its own way. Only ever loaded on a game client, through
 * {@link com.dwinovo.chiikawa.platform.ClientServices}.
 */
public interface IGuiHelper {
    /** Hands the game a picture, to be drawn by the renderer registered for its class. */
    void submitPicture(GuiGraphics graphics, PictureInPictureRenderState picture);

    /** @return the part of the screen drawing is clipped to right now, {@code null} when it is not */
    @Nullable
    ScreenRectangle scissorArea(GuiGraphics graphics);
}
