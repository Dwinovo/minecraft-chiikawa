package com.dwinovo.chiikawa.ui;

/**
 * Everything this UI library needs from the game's drawing API — its whole contract with
 * Minecraft.
 *
 * <p>Minecraft moves its GUI API around between versions, and the mod ships fourteen
 * version branches. The library itself (layout, theme, typography) is written against
 * this interface and imports no Minecraft at all, which the module's classpath enforces;
 * porting a branch is rewriting one small adapter, with the library and the screens
 * copied across untouched. Same isolation as {@code platform.Services}, one layer up.
 *
 * <p>Keep the method set small: every method added here is another thing each branch has
 * to port. Text arrives as a plain string — resolving translations is the host's job.
 */
public interface DrawSurface {
    /**
     * Square fill. The library draws no rounded corners: at this pixel size they only
     * blur the edge, so depth comes from colour, outline and spacing instead.
     */
    void fillRect(int x, int y, int width, int height, int argb);

    void drawText(String text, int x, int y, int argb);

    /** Text width in pixels, measured with the same font {@link #drawText} uses. */
    int textWidth(String text);

    /** Height of one line of text, including its leading. */
    int lineHeight();
}
