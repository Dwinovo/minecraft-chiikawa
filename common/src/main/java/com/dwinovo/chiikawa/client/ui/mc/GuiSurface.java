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
