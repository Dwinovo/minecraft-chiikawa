package com.dwinovo.chiikawa.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.ui.widget.Badge;
import com.dwinovo.chiikawa.ui.widget.Bar;
import com.dwinovo.chiikawa.ui.widget.Chip;
import com.dwinovo.chiikawa.ui.widget.Hearts;
import com.dwinovo.chiikawa.ui.widget.Sign;
import com.dwinovo.chiikawa.ui.widget.Tabs;
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
        public void drawIcon(Icon icon, int x, int y) {
            fillRect(x + 2, y + 2, 12, 12, 0x6089A86B);
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
        List<Tabs.Face> faces = List.of(
            (s, x, y) -> s.drawIcon(Icon.NONE, x, y),
            (s, x, y) -> PixelArt.SLIP.drawCentered(s, x, y, UiStyle.ICON),
            (s, x, y) -> PixelArt.SPEECH.drawCentered(s, x, y, UiStyle.ICON));
        int[] tints = {UiTheme.ACCENT_PALE, UiTheme.SKY_PALE, UiTheme.LEAF_PALE};

        Tabs.drawBehind(surface, px, py, faces, tints, 1);
        Ui.card(surface, px, py, 200, 140);
        Tabs.drawFront(surface, px, py, faces.get(1), 1);
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
        Chip.of("Idle").draw(surface, px + 150, py + 110);
        Ui.divider(surface, px, py + 120, 200);
        Ui.sticker(surface, px + 8, py + 124, 184, 12, Ui.CARD_RADIUS, UiTheme.ACCENT_SOFT);
        PixelArt.CHECK.draw(surface, px + 175, py + 126);

        File out = new File("build/ui-preview.png");
        out.getParentFile().mkdirs();
        ImageIO.write(surface.image, "png", out);
        assertTrue(out.isFile());
    }
}
