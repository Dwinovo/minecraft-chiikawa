package com.dwinovo.chiikawa.testing;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * How wide the game's own font draws a string, for tests of text that has to fit a box, worked out from the font files
 * the game ships rather than guessed: {@code font/include/default.json} names the bitmaps, and a bitmap glyph is as wide
 * as its rightmost opaque column plus a pixel of air, as {@code BitmapProvider} sets it. The glyphs the game takes from
 * Unifont, which are not in the game's jar, are sized as the ranges in {@code font/include/unifont.json} size them: nine
 * wide for kana, ideographs and full-width forms, eight for Hangul syllables.
 *
 * <p>A character with no width here is an error and not a guess: measure it in the game and give it a range.
 */
public final class MinecraftFont {
    private static final String ASSETS = "/assets/minecraft/";
    private static final String BITMAPS = ASSETS + "font/include/default.json";
    private static final String SPACES = ASSETS + "font/include/space.json";

    /** Advance in pixels by code point. Later providers win a glyph that two of them have, as the game's font set does. */
    private static final Map<Integer, Integer> ADVANCE = new HashMap<>();

    static {
        try {
            for (JsonElement element : providers(BITMAPS)) {
                JsonObject provider = element.getAsJsonObject();
                if ("bitmap".equals(provider.get("type").getAsString())) {
                    bitmap(provider);
                }
            }
            for (JsonElement element : providers(SPACES)) {
                JsonObject provider = element.getAsJsonObject();
                provider.getAsJsonObject("advances").entrySet().forEach(
                    advance -> ADVANCE.put(advance.getKey().codePointAt(0), advance.getValue().getAsInt()));
            }
        } catch (IOException e) {
            throw new IllegalStateException("The game's font files are not on the test classpath", e);
        }
    }

    private MinecraftFont() {
    }

    /** @return how wide the game draws the text, in pixels, shadow not counted */
    public static int width(String text) {
        return text.codePoints().map(MinecraftFont::advance).sum();
    }

    private static int advance(int codePoint) {
        Integer known = ADVANCE.get(codePoint);
        if (known != null) {
            return known;
        }
        if (unifont(codePoint, 0x3001, 0x30FF) || unifont(codePoint, 0x3200, 0x9FFF) || unifont(codePoint, 0x1100, 0x11FF)
                || unifont(codePoint, 0x3130, 0x318F) || unifont(codePoint, 0xA960, 0xA97F) || unifont(codePoint, 0xD7B0, 0xD7FF)
                || unifont(codePoint, 0xF900, 0xFAFF) || unifont(codePoint, 0xFF01, 0xFF5E)) {
            return 9;
        }
        if (unifont(codePoint, 0xAC00, 0xD7AF)) {
            return 8;
        }
        throw new IllegalArgumentException("No width for U+" + Integer.toHexString(codePoint).toUpperCase() + " ("
            + new String(Character.toChars(codePoint)) + "): measure it in the game and give it a range");
    }

    private static boolean unifont(int codePoint, int from, int to) {
        return codePoint >= from && codePoint <= to;
    }

    private static JsonArray providers(String path) throws IOException {
        try (InputStream stream = MinecraftFont.class.getResourceAsStream(path);
                Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("providers");
        }
    }

    /** As {@code BitmapProvider.Definition} reads a bitmap: a grid of characters over a picture cut into equal cells. */
    private static void bitmap(JsonObject provider) throws IOException {
        String file = provider.get("file").getAsString().replace("minecraft:", "textures/");
        int height = provider.has("height") ? provider.get("height").getAsInt() : 8;
        JsonArray rows = provider.getAsJsonArray("chars");
        BufferedImage image;
        try (InputStream stream = MinecraftFont.class.getResourceAsStream(ASSETS + file)) {
            image = ImageIO.read(stream);
        }
        int columns = rows.get(0).getAsString().codePointCount(0, rows.get(0).getAsString().length());
        int cellWidth = image.getWidth() / columns;
        int cellHeight = image.getHeight() / rows.size();
        float scale = (float) height / (float) cellHeight;
        for (int row = 0; row < rows.size(); row++) {
            int[] codePoints = rows.get(row).getAsString().codePoints().toArray();
            for (int column = 0; column < codePoints.length; column++) {
                if (codePoints[column] != 0) {
                    int drawn = drawnWidth(image, cellWidth, cellHeight, column, row);
                    ADVANCE.put(codePoints[column], (int) (0.5 + (double) ((float) drawn * scale)) + 1);
                }
            }
        }
    }

    /** The rightmost column of the cell that has anything in it, counted from 1; 0 for a cell with nothing. */
    private static int drawnWidth(BufferedImage image, int cellWidth, int cellHeight, int column, int row) {
        for (int x = cellWidth - 1; x >= 0; x--) {
            for (int y = 0; y < cellHeight; y++) {
                if ((image.getRGB(column * cellWidth + x, row * cellHeight + y) >>> 24) != 0) {
                    return x + 1;
                }
            }
        }
        return 0;
    }
}
