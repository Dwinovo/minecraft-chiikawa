package com.dwinovo.chiikawa.client.ui.mc;

import com.dwinovo.chiikawa.client.ui.Surface;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Draws the mod's UI onto a screen. The only place that touches Minecraft's drawing API,
 * so a version that changes it is a change here.
 */
public record GuiSurface(GuiGraphics graphics, Font font) implements Surface {
    @Override
    public void fillRect(int x, int y, int width, int height, int argb) {
        graphics.fill(x, y, x + width, y + height, argb);
    }

    @Override
    public void drawText(Component text, int x, int y, int argb) {
        graphics.drawString(font, text, x, y, argb, false);
    }

    @Override
    public int textWidth(Component text) {
        return font.width(text);
    }

    @Override
    public int lineHeight() {
        return font.lineHeight;
    }
}
