package com.dwinovo.chiikawa.client.ui.mc;

import com.dwinovo.chiikawa.ui.DrawSurface;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Draws the {@code chiikawa-ui} library onto a Minecraft screen — the whole of the
 * library's contact with the game's drawing API, and the only part of it a new Minecraft
 * version can break.
 */
public record GuiSurface(GuiGraphics graphics, Font font) implements DrawSurface {
    @Override
    public void fillRect(int x, int y, int width, int height, int argb) {
        graphics.fill(x, y, x + width, y + height, argb);
    }

    @Override
    public void drawText(String text, int x, int y, int argb) {
        graphics.drawString(font, text, x, y, argb, false);
    }

    @Override
    public int textWidth(String text) {
        return font.width(text);
    }

    @Override
    public int lineHeight() {
        return font.lineHeight;
    }
}
