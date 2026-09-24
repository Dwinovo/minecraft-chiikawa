package com.dwinovo.chiikawa.client.ui.mc;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Icon;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Draws the {@code chiikawa-ui} library onto a Minecraft screen — the whole of the
 * library's contact with the game's drawing API, and the only part of it a new Minecraft
 * version can break.
 */
public record GuiSurface(GuiGraphics graphics, Font font) implements DrawSurface {
    /**
     * Draws above everything already on the screen — where a tooltip belongs. The game puts
     * a screen together a layer at a time, item icons over the flat drawing of their layer,
     * so a card drawn in the same layer would have those icons poke straight through it;
     * this starts the next layer, as the game's own tooltips do.
     */
    public void onTop(Runnable drawing) {
        graphics.nextStratum();
        drawing.run();
    }

    @Override
    public void fillRect(int x, int y, int width, int height, int argb) {
        graphics.fill(x, y, x + width, y + height, argb);
    }

    @Override
    public void drawText(String text, int x, int y, int argb) {
        graphics.drawString(font, text, x, y, argb, false);
    }

    @Override
    public void drawIcon(Icon icon, int x, int y) {
        if (icon instanceof ItemIcon item) {
            graphics.renderItem(item.stack(), x, y);
        }
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
