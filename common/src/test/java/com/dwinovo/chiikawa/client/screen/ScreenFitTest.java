package com.dwinovo.chiikawa.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.data.LaborBoardLevelData;
import com.dwinovo.chiikawa.data.LanguageData;
import com.dwinovo.chiikawa.testing.MinecraftFont;
import com.dwinovo.chiikawa.ui.TextWrap;
import com.dwinovo.chiikawa.ui.widget.Tabs;
import com.dwinovo.chiikawa.ui.widget.TitledPanel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Every screen, at its fullest, fits the smallest window the game picks a scale for: its panel, and the board hung over its
 * top edge, are inside the window's 240 scaled pixels, top to bottom. The fullest is the most rows it will list, the most
 * slips a board is given, the longest language's notes broken into lines with the game's own font, and a tab row when there
 * is one; and the screens that give up rows to the window are asked in windows of every height from the smallest up.
 */
class ScreenFitTest {
    private static final int SMALLEST = PanelFit.SMALLEST_WINDOW;
    /** Wide enough that a screen's width is never what limits it. */
    private static final int WIDE = 427;

    /** Where a panel is and how tall, in a window this high: the y its board starts at and the y its bottom edge is at. */
    private record Fit(String screen, int windowHeight, int top, int height) {
        int sign() {
            return top - TitledPanel.SIGN_RISE;
        }

        int bottom() {
            return top + height;
        }

        boolean inside() {
            return sign() >= 0 && bottom() <= windowHeight;
        }

        @Override
        public String toString() {
            return screen + " in a window " + windowHeight + " high: board at " + sign() + ", bottom edge at " + bottom();
        }
    }

    private static Fit placed(String screen, int windowHeight, int panelHeight) {
        return new Fit(screen, windowHeight, PanelFit.top(windowHeight, panelHeight), panelHeight);
    }

    @Test
    void everyScreenFitsTheSmallestWindowAtItsFullest() {
        List<Fit> fits = new ArrayList<>();
        fits.add(placed("handbook", SMALLEST, Math.min(HandbookLayout.MAX_HEIGHT, PanelFit.room(SMALLEST))));
        fits.add(placed("shop", SMALLEST, ShopLayout.panelHeight()));
        fits.add(placed("labor board", SMALLEST,
            BoardLayout.panelHeight(LaborBoardLevelData.LEVELS.slipsAt(LaborBoardLevelData.LEVELS.top()))));
        fits.add(placed("album", SMALLEST, AlbumLayout.panelHeight(
            AlbumLayout.pictureHeight(AlbumLayout.pictureWidth(WIDE, SMALLEST)))));
        for (boolean hint : new boolean[] {false, true}) {
            fits.add(placed("music box" + (hint ? " with the format hint" : ""), SMALLEST,
                MusicBoxLayout.panelHeight(MusicBoxLayout.rows(SMALLEST, hint), hint)));
        }
        for (boolean tabs : new boolean[] {false, true}) {
            fits.add(placed("exam desk" + (tabs ? " with tabs" : ""), SMALLEST,
                ExamDeskLayout.panelHeight(ExamDeskLayout.rows(SMALLEST, tabs, 2 * ExamDeskLayout.MOST_ROWS), tabs)));
        }
        fits.add(placed("settings, longest language", SMALLEST, longestSettings()));
        List<String> outside = new ArrayList<>();
        for (Fit fit : fits) {
            if (!fit.inside()) {
                outside.add(fit.toString());
            }
        }
        assertEquals(List.of(), outside);
    }

    /** The pet screen has its tabs over the panel where the others have a board, and every page is under its tallest. */
    @Test
    void thePetScreenAndItsTabsFitTheSmallestWindowOnEveryPage() {
        int top = PetScreenLayout.top(SMALLEST);
        assertTrue(top - Tabs.HEIGHT >= 0, "the tabs of the pet screen start above the window: " + (top - Tabs.HEIGHT));
        assertTrue(top + PetScreenLayout.TALLEST <= SMALLEST, "the pet screen ends at " + (top + PetScreenLayout.TALLEST));
        for (int page : new int[] {PetScreenLayout.BACKPACK_HEIGHT, PetScreenLayout.ORDERS_HEIGHT,
                PetScreenLayout.statusHeight(true), PetScreenLayout.statusHeight(false)}) {
            assertTrue(page <= PetScreenLayout.TALLEST, "a page of the pet screen is " + page + " high, past the tallest it is placed for");
        }
    }

    @Test
    void theScreensThatGiveUpRowsFitEveryWindowFromTheSmallestUp() {
        List<String> outside = new ArrayList<>();
        for (int height = SMALLEST; height <= 2 * SMALLEST; height++) {
            for (boolean hint : new boolean[] {false, true}) {
                int rows = MusicBoxLayout.rows(height, hint);
                Fit fit = placed("music box with " + rows + " rows", height, MusicBoxLayout.panelHeight(rows, hint));
                if (!fit.inside() || rows < 1) {
                    outside.add(fit.toString());
                }
            }
            for (boolean tabs : new boolean[] {false, true}) {
                int rows = ExamDeskLayout.rows(height, tabs, 2 * ExamDeskLayout.MOST_ROWS);
                Fit fit = placed("exam desk with " + rows + " rows", height, ExamDeskLayout.panelHeight(rows, tabs));
                if (!fit.inside() || rows < 1) {
                    outside.add(fit.toString());
                }
            }
            for (int width : new int[] {320, WIDE, 1000}) {
                Fit fit = placed("album in a window " + width + " wide", height,
                    AlbumLayout.panelHeight(AlbumLayout.pictureHeight(AlbumLayout.pictureWidth(width, height))));
                if (!fit.inside()) {
                    outside.add(fit.toString());
                }
            }
        }
        assertEquals(List.of(), outside);
    }

    @Test
    void theMusicBoxShowsAllItsRowsWhenTheWindowHasTheRoom() {
        int room = MusicBoxLayout.panelHeight(MusicBoxLayout.MOST_ROWS, true) + TitledPanel.SIGN_RISE + 2 * PanelFit.MARGIN;
        assertEquals(MusicBoxLayout.MOST_ROWS, MusicBoxLayout.rows(room, true));
        assertEquals(MusicBoxLayout.MOST_ROWS - 1, MusicBoxLayout.rows(room - 1, true));
        assertEquals(MusicBoxLayout.MOST_ROWS, MusicBoxLayout.rows(2 * room, false));
    }

    /** The tallest the settings panel gets in any language: both of its notes at the most lines the words take. */
    private static int longestSettings() {
        int hintLines = 0;
        int volumeLines = 0;
        for (String locale : LanguageData.LOCALES) {
            Map<String, String> words = new HashMap<>();
            LanguageData.addTranslations(locale, words::put);
            for (String key : List.of("screen.chiikawa.settings.world_only", "screen.chiikawa.settings.read_only")) {
                hintLines = Math.max(hintLines, lines(words.get(key)));
            }
            volumeLines = Math.max(volumeLines, lines(words.get("screen.chiikawa.settings.volume")));
        }
        return SettingsLayout.panelHeight(hintLines, volumeLines);
    }

    private static int lines(String text) {
        return TextWrap.wrap(text, SettingsScreen.CONTENT_W, MinecraftFont::width).size();
    }
}
