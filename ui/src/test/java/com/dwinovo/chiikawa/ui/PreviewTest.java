package com.dwinovo.chiikawa.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.ui.widget.Badge;
import com.dwinovo.chiikawa.ui.widget.Banner;
import com.dwinovo.chiikawa.ui.widget.Bar;
import com.dwinovo.chiikawa.ui.widget.Bubble;
import com.dwinovo.chiikawa.ui.widget.Chip;
import com.dwinovo.chiikawa.ui.widget.DashedWell;
import com.dwinovo.chiikawa.ui.widget.Hearts;
import com.dwinovo.chiikawa.ui.widget.Price;
import com.dwinovo.chiikawa.ui.widget.Segments;
import com.dwinovo.chiikawa.ui.widget.Sign;
import com.dwinovo.chiikawa.ui.widget.Switch;
import com.dwinovo.chiikawa.ui.widget.TabStrip;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * Draws a sample of every surface and widget into {@code ui/build/ui-preview.png}, so the
 * shapes the library actually makes can be looked at without starting the game. Words
 * come out as ink bars the width the stand-in font says they are: this is a picture of the
 * geometry, and the game's own font is not on this module's classpath.
 */
class PreviewTest {
    private static final int SCALE = 3;

    /** Pixels on a canvas, the last colour over a pixel winning, alpha blended. */
    static final class RasterSurface implements DrawSurface {
        final BufferedImage image;

        RasterSurface(int width, int height, int background) {
            image = new BufferedImage(width * SCALE, height * SCALE, BufferedImage.TYPE_INT_ARGB);
            fillRect(0, 0, width, height, background);
        }

        @Override
        public void fillRect(int x, int y, int width, int height, int argb) {
            int alpha = argb >>> 24;
            for (int py = y * SCALE; py < (y + height) * SCALE; py++) {
                for (int px = x * SCALE; px < (x + width) * SCALE; px++) {
                    if (px < 0 || py < 0 || px >= image.getWidth() || py >= image.getHeight()) {
                        continue;
                    }
                    image.setRGB(px, py, alpha == 255 ? argb : blend(image.getRGB(px, py), argb, alpha));
                }
            }
        }

        private static int blend(int under, int over, int alpha) {
            int r = (((under >> 16) & 0xFF) * (255 - alpha) + ((over >> 16) & 0xFF) * alpha) / 255;
            int g = (((under >> 8) & 0xFF) * (255 - alpha) + ((over >> 8) & 0xFF) * alpha) / 255;
            int b = ((under & 0xFF) * (255 - alpha) + (over & 0xFF) * alpha) / 255;
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        }

        @Override
        public void drawText(String text, int x, int y, int argb) {
            fillRect(x, y + 3, textWidth(text) - 1, 3, argb);
        }

        @Override
        public void drawIcon(Icon icon, int x, int y, int size) {
            fillRect(x + size / 8, y + size / 8, size * 3 / 4, size * 3 / 4, 0x6089A86B);
        }

        @Override
        public int textWidth(String text) {
            return text.length() * 5;
        }

        @Override
        public int lineHeight() {
            return 9;
        }
    }

    @Test
    void drawsTheLibraryForALook() throws Exception {
        RasterSurface surface = new RasterSurface(240, 200, 0xFF33402F);
        int px = 16;
        int py = 40;
        TabStrip tabs = new TabStrip(new Rect(px, py, 200, 140), List.of(
            new TabStrip.Tab((s, x, y) -> s.drawIcon(Icon.NONE, x, y), "Bag"),
            new TabStrip.Tab((s, x, y) -> PixelArt.GEAR.drawCentered(s, x, y, UiStyle.ICON), "Settings")), surface::textWidth);

        tabs.drawBehind(surface, 0);
        Ui.card(surface, px, py, 200, 140);
        tabs.drawFront(surface, 0, UiTheme.PANEL);
        Sign.draw(surface, px + 200 - 10 - Sign.width(surface, "Usagi"), py - 9, "Usagi");
        Hearts.draw(surface, px + 200 - 10 - Hearts.width(20), py + 12, 15, 20);

        Ui.sticker(surface, px + 8, py + 24, 56, 64, Ui.CARD_RADIUS, UiTheme.SKY_PALE);
        for (int col = 0; col < 5; col++) {
            Ui.well(surface, px + 70 + col * 18, py + 24, UiStyle.SLOT, UiStyle.SLOT);
        }
        Ui.sticker(surface, px + 70, py + 46, 120, 30, Ui.CARD_RADIUS, UiTheme.LEAF_PALE);
        Bar.draw(surface, px + 76, py + 64, 80, UiStyle.BAR_H, 5, 12);
        Badge.draw(surface, "open", px + 8, py + 96, UiTheme.TEXT_MUTED);
        Badge.draw(surface, "taken", px + 50, py + 96, UiTheme.SUCCESS);
        Ui.divider(surface, px, py + 120, 200);
        Ui.sticker(surface, px + 8, py + 124, 184, 12, Ui.CARD_RADIUS, UiTheme.ACCENT_SOFT);
        Switch.draw(surface, px + 170, py + 125, true, false);
        Price.draw(surface, Icon.NONE, "Buy 3", px + 12, py + 124, 12, UiTheme.TEXT);

        File out = new File("build/ui-preview.png");
        out.getParentFile().mkdirs();
        ImageIO.write(surface.image, "png", out);
        assertTrue(out.isFile());
    }

    /** The pet screen's pieces together, on a panel as wide as the screen's: a look at how the new widgets sit. */
    @Test
    void drawsThePetScreenWidgetsForALook() throws Exception {
        RasterSurface surface = new RasterSurface(320, 240, 0xFF33402F);
        Rect panel = new Rect(10, 50, 300, 166);
        List<TabStrip.Tab> tabs = List.of(
            new TabStrip.Tab((s, x, y) -> s.drawIcon(Icon.NONE, x, y), "Backpack"),
            new TabStrip.Tab((s, x, y) -> PixelArt.GEAR.drawCentered(s, x, y, UiStyle.ICON), "Settings"));
        TabStrip strip = new TabStrip(panel, tabs, surface::textWidth);

        strip.drawBehind(surface, 0);
        Ui.card(surface, panel.x(), panel.y(), panel.width(), panel.height());
        Banner.draw(surface, panel.x() + 2, panel.y() + 2, panel.width() - 4, 68, UiTheme.ACCENT_PALE);
        strip.drawFront(surface, 0, UiTheme.ACCENT_PALE);

        int x = panel.x() + 94;
        int y = panel.y();
        Chip.draw(surface, (s, cx, cy) -> s.drawIcon(Icon.NONE, cx, cy, Chip.FACE), "Farmer", x + 40, y + 7, UiTheme.SURFACE, false);
        Chip.draw(surface, (s, cx, cy) -> s.drawIcon(Icon.NONE, cx, cy, Chip.FACE), "Grade 4", x + 90, y + 7, UiTheme.LEAF_PALE, true);
        Hearts.draw(surface, x, y + 30, 16, 20);
        Segments.draw(surface, new Rect(panel.x() + 184, y + 27, 106, Segments.HEIGHT), List.of("Follow", "Sit", "Free"), 0, 2);
        Rect bubble = new Rect(panel.x() + 96, y + 47, 194, 19);
        Bubble.frame(surface, bubble, y + 56);
        Bar.draw(surface, bubble.x() + 80, bubble.y() + 6, 60, UiStyle.BAR_H, 12, 32);

        Rect card = new Rect(panel.x() + 4, y + 74, 120, 84);
        Ui.sticker(surface, card.x(), card.y(), card.width(), card.height(), Ui.CARD_RADIUS, UiTheme.ACCENT_PALE);
        Ui.well(surface, card.x() + 4, card.y() + 13, UiStyle.SLOT, UiStyle.SLOT);
        for (int col = 0; col < 5; col++) {
            Ui.well(surface, card.x() + 25 + col * 18, card.y() + 4, UiStyle.SLOT, UiStyle.SLOT);
            DashedWell.draw(surface, card.x() + 25 + col * 18, card.y() + 44, UiStyle.SLOT, UiStyle.SLOT);
        }
        Ui.divider(surface, card.x() - 4, card.y() + 41, 128);
        PixelArt.BAG.drawCentered(surface, card.x() + 4, card.y() + 53, UiStyle.SLOT);
        Switch.draw(surface, panel.x() + 250, y + 100, true, false);
        Switch.draw(surface, panel.x() + 250, y + 120, false, false);

        File out = new File("build/ui-preview-pet.png");
        out.getParentFile().mkdirs();
        ImageIO.write(surface.image, "png", out);
        assertTrue(out.isFile());
    }
}
