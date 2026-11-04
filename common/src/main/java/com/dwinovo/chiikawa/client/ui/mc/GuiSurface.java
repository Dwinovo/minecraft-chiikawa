package com.dwinovo.chiikawa.client.ui.mc;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Icon;
import com.dwinovo.chiikawa.ui.UiStyle;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Draws the {@code chiikawa-ui} library onto a Minecraft screen — the whole of the
 * library's contact with the game's drawing API, and the only part of it a new Minecraft
 * version can break.
 */
public record GuiSurface(GuiGraphics graphics, Font font) implements DrawSurface {
    /**
     * How far in front of the screen a tooltip is drawn. The game puts item icons a long
     * way forward so they stand off their slots, and a card drawn flat would have those
     * icons poke straight through it; this is the depth the game's own tooltips use.
     */
    private static final int TOOLTIP_Z = 400;

    /** Draws above everything already on the screen — where a tooltip belongs. */
    public void onTop(Runnable drawing) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, TOOLTIP_Z);
        drawing.run();
        graphics.pose().popPose();
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
    public void drawIcon(Icon icon, int x, int y, int size) {
        if (!(icon instanceof ItemIcon item)) {
            return;
        }
        if (size == UiStyle.ICON) {
            graphics.renderItem(item.stack(), x, y);
            return;
        }
        // The game draws an item sixteen wide wherever it is told to; a smaller one is the same item under a smaller scale.
        float scale = (float) size / UiStyle.ICON;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.renderItem(item.stack(), 0, 0);
        graphics.pose().popPose();
    }

    /** Draws text {@code scale} times as large with its top-left corner at x, y, for a name that is the heading of a card. */
    public void drawTextScaled(String text, int x, int y, int scale, int argb) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, 0, 0, argb, false);
        graphics.pose().popPose();
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
