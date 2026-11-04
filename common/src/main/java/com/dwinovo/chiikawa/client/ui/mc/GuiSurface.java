package com.dwinovo.chiikawa.client.ui.mc;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Icon;
import com.dwinovo.chiikawa.ui.UiStyle;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Draws the {@code chiikawa-ui} library onto a Minecraft screen — the whole of the
 * library's contact with the game's drawing API, and the only part of it a new Minecraft
 * version can break.
 */
public record GuiSurface(GuiGraphicsExtractor graphics, Font font) implements DrawSurface {
    /**
     * Draws above everything already on the screen — where a tooltip belongs. The game
     * draws an item icon over any card drawn beside it on the same layer, and a card drawn
     * there would have those icons poke straight through it; so this goes on a layer of its
     * own, as the game's own tooltips do.
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
        graphics.text(font, text, x, y, argb, false);
    }

    @Override
    public void drawIcon(Icon icon, int x, int y, int size) {
        if (!(icon instanceof ItemIcon item)) {
            return;
        }
        if (size == UiStyle.ICON) {
            graphics.item(item.stack(), x, y);
            return;
        }
        // The game draws an item sixteen wide wherever it is told to; a smaller one is the same item under a smaller scale.
        float scale = (float) size / UiStyle.ICON;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.item(item.stack(), 0, 0);
        graphics.pose().popMatrix();
    }

    /** Draws text {@code scale} times as large with its top-left corner at x, y, for a name that is the heading of a card. */
    public void drawTextScaled(String text, int x, int y, int scale, int argb) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.text(font, text, 0, 0, argb, false);
        graphics.pose().popMatrix();
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
