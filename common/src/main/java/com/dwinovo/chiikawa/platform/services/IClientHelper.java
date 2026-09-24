package com.dwinovo.chiikawa.platform.services;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;

/** What a player's own game does that each loader does its own way. Only ever called there. */
public interface IClientHelper {
    /**
     * Adds a picture of the mod's own to what a screen draws this frame: something in 3D,
     * drawn off to the side and pasted in, as the game pastes a pet into the inventory
     * screen. The game only takes the kinds it knows of; each loader opens the way for more.
     */
    void submitPicture(GuiGraphicsExtractor graphics, PictureInPictureRenderState picture);
}
