package com.dwinovo.chiikawa.client.ui.mc;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

/**
 * A button in the mod's own hand: a small sticker that blushes pink under the cursor and
 * goes flat and quiet when there is nothing to press.
 *
 * <p>Built on the game's own button rather than hand-rolled, so clicking, the keyboard,
 * focus and narration all keep working; only the drawing is ours.
 */
public final class UiButton extends AbstractButton {
    /** What goes on the face — a word, an arrow, whatever the button is for. */
    @FunctionalInterface
    public interface Face {
        void draw(DrawSurface surface, Rect area, int argb);
    }

    /** The whole button, for one that is more than a face on the usual sticker. */
    @FunctionalInterface
    public interface Painter {
        void paint(DrawSurface surface, Rect area, boolean hovered, boolean active);
    }

    private final Painter painter;
    private final Runnable action;

    public UiButton(int x, int y, int width, int height, Component narration, Face face, Runnable action) {
        this(x, y, width, height, narration, standard(face), action);
    }

    private UiButton(int x, int y, int width, int height, Component narration, Painter painter, Runnable action) {
        super(x, y, width, height, narration);
        this.painter = painter;
        this.action = action;
    }

    /** A button with a word on it. */
    public static UiButton text(int x, int y, int width, int height, Component label, Runnable action) {
        return new UiButton(x, y, width, height, label,
            (surface, area, argb) -> Ui.textCentered(surface, label.getString(), area, argb), action);
    }

    /** A button that draws all of itself: a card to pick, say, rather than a word to press. */
    public static UiButton painted(int x, int y, int width, int height, Component narration, Painter painter,
            Runnable action) {
        return new UiButton(x, y, width, height, narration, painter, action);
    }

    /** The usual button: paper, pink under the cursor, flat and quiet when it cannot be pressed. */
    private static Painter standard(Face face) {
        return (surface, area, hovered, active) -> {
            int fill = !active ? UiTheme.SURFACE : hovered ? UiTheme.ACCENT_PALE : UiTheme.PANEL;
            Ui.sticker(surface, area.x(), area.y(), area.width(), area.height(), Ui.CARD_RADIUS, fill);
            face.draw(surface, area, active ? UiTheme.TEXT : UiTheme.TEXT_MUTED);
        };
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        DrawSurface surface = new GuiSurface(graphics, net.minecraft.client.Minecraft.getInstance().font);
        painter.paint(surface, new Rect(getX(), getY(), getWidth(), getHeight()), isHoveredOrFocused(), active);
    }

    @Override
    public void onPress(InputWithModifiers input) {
        action.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
