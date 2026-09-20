package com.dwinovo.chiikawa.client.ui;

import net.minecraft.network.chat.Component;

/**
 * Everything the mod's UI needs from Minecraft's drawing API.
 *
 * <p>Screens and world labels draw through this, so a Minecraft version that moves
 * {@code GuiGraphics} or the font API around is a change to one adapter
 * ({@code ui.mc.GuiSurface}) instead of every screen. Same isolation as
 * {@code platform.Services}, one layer up.
 *
 * <p>Keep the method set small: every method added here is another thing each version
 * branch has to port.
 */
public interface Surface {
    /** Square fill. The UI draws no rounded corners: depth comes from colour and spacing. */
    void fillRect(int x, int y, int width, int height, int argb);

    void drawText(Component text, int x, int y, int argb);

    /** Text width in pixels, measured with the same font {@link #drawText} uses. */
    int textWidth(Component text);

    /** Height of one line of text, including its leading. */
    int lineHeight();
}
