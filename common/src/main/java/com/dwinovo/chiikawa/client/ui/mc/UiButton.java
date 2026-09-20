package com.dwinovo.chiikawa.client.ui.mc;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * A button in the mod's own hand: a raised panel that lights up under the cursor and goes
 * quiet when there is nothing to press.
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

    private final Face face;
    private final Runnable action;

    public UiButton(int x, int y, int width, int height, Component narration, Face face, Runnable action) {
        super(x, y, width, height, narration);
        this.face = face;
        this.action = action;
    }

    /** A button with a word on it. */
    public static UiButton text(int x, int y, int width, int height, Component label, Runnable action) {
        return new UiButton(x, y, width, height, label,
            (surface, area, argb) -> Ui.textCentered(surface, label.getString(), area, argb), action);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        DrawSurface surface = new GuiSurface(graphics, net.minecraft.client.Minecraft.getInstance().font);
        Rect area = new Rect(getX(), getY(), getWidth(), getHeight());
        Ui.panel(surface, area.x(), area.y(), area.width(), area.height());
        if (active && isHoveredOrFocused()) {
            Ui.rowHighlight(surface, area);
        }
        face.draw(surface, area, active ? UiTheme.TEXT : UiTheme.TEXT_MUTED);
    }

    @Override
    public void onPress() {
        action.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
