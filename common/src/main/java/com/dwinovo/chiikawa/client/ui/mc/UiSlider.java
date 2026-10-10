package com.dwinovo.chiikawa.client.ui.mc;

import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.widget.Slider;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * A slider in the mod's own hand: a value from 0 to 1 dragged along a groove, with what it
 * stands for written across it.
 *
 * <p>Built on the game's own slider so dragging, the keyboard, focus and narration all keep
 * working; only the drawing is ours. It says when the value changes, which during a drag is
 * every pixel: whoever listens decides when a change has been settled on.
 */
public final class UiSlider extends AbstractSliderButton {
    /** How far an arrow key moves it: one percent, the finest the settings are shown in. */
    private static final double KEY_STEP = 0.01;

    private final DoubleFunction<Component> label;
    private final DoubleConsumer onChange;

    /**
     * @param value where it starts, from 0 to 1
     * @param label what is written across it for a value
     * @param onChange called with the new value each time it moves
     */
    public UiSlider(int x, int y, int width, int height, double value, DoubleFunction<Component> label,
            DoubleConsumer onChange) {
        super(x, y, width, height, label.apply(value), value);
        this.label = label;
        this.onChange = onChange;
    }

    @Override
    protected void updateMessage() {
        setMessage(label.apply(value));
    }

    @Override
    protected void applyValue() {
        onChange.accept(value);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputConstants.KEY_LEFT || keyCode == InputConstants.KEY_RIGHT) {
            // The game's own arrow step is a pixel's worth, which is not a step anyone can count.
            double moved = Mth.clamp(value + (keyCode == InputConstants.KEY_LEFT ? -KEY_STEP : KEY_STEP), 0.0, 1.0);
            if (moved != value) {
                value = moved;
                applyValue();
                updateMessage();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        DrawSurface surface = new GuiSurface(graphics, Minecraft.getInstance().font);
        Slider.draw(surface, new Rect(getX(), getY(), getWidth(), getHeight()), value, getMessage().getString(),
            isHoveredOrFocused(), active);
    }
}
