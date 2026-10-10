package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.client.settings.ClientSettings;
import com.dwinovo.chiikawa.client.settings.ClientSettingsStore;
import com.dwinovo.chiikawa.client.settings.ClientWorldSettings;
import com.dwinovo.chiikawa.client.ui.mc.GuiSurface;
import com.dwinovo.chiikawa.client.ui.mc.UiButton;
import com.dwinovo.chiikawa.client.ui.mc.UiSlider;
import com.dwinovo.chiikawa.network.SettingsPayloads.ChangeWorldSettingsPayload;
import com.dwinovo.chiikawa.platform.Services;
import com.dwinovo.chiikawa.settings.WorldSettings;
import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.TextWrap;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import com.dwinovo.chiikawa.ui.widget.TitledPanel;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * The settings: how many wild pets spawn and how much pets talk, which belong to the world
 * and are the server's to hold, and whether their words are drawn over their heads, which is
 * the player's own. Drawn from the {@code chiikawa-ui} library like every screen of the mod's.
 *
 * <p>The world's two are only editable by a player the server will take a change from, in a
 * world; to anyone else they are shown as they are, with a line saying why they cannot be
 * moved. A change is sent when the player lets go of a slider, and what the screen shows
 * follows what the server last said, so a change someone else makes shows up here too.
 */
public class SettingsScreen extends Screen {
    private static final int PANEL_W = 248;
    private static final int CONTENT_W = PANEL_W - 2 * UiStyle.PAD;
    private static final int DONE_W = 100;
    /** Whole percent, the finest a setting is shown or set in. */
    private static final int STEPS = 100;

    private final Screen parent;
    private ClientWorldSettings.Synced shown = ClientWorldSettings.Synced.NONE;
    private WorldSettings editing = WorldSettings.DEFAULT;
    /** Whether a slider has moved since the server was last told. */
    private boolean changed;
    private int leftPos, topPos, panelHeight, hintLines;
    private int hintY, volumeY;

    /** @param parent the screen to go back to when this one is closed; none when there is nowhere to go back to */
    public SettingsScreen(Screen parent) {
        super(Component.translatable("screen.chiikawa.settings.title"));
        this.parent = parent;
    }

    private boolean inWorld() {
        return this.minecraft != null && this.minecraft.level != null;
    }

    /** Whether the two world settings can be moved: in a world, by somebody the server will listen to. */
    private boolean editable() {
        return inWorld() && shown.canChange();
    }

    @Override
    protected void init() {
        this.shown = ClientWorldSettings.current();
        this.editing = shown.settings();
        this.hintLines = Math.max(wrap(worldOnlyHint()).size(), wrap(readOnlyHint()).size());
        int volumeLines = wrap(volumeHint()).size();

        int contentHeight = 2 * UiStyle.CONTROL_H + UiStyle.GAP
            + UiStyle.GAP + hintLines * UiStyle.LINE
            + UiStyle.GAP_SECTION + UiStyle.CONTROL_H
            + UiStyle.GAP + volumeLines * UiStyle.LINE
            + UiStyle.GAP_SECTION + UiStyle.CONTROL_H;
        this.panelHeight = UiStyle.TITLE_H + UiStyle.PAD + contentHeight + UiStyle.PAD;
        this.leftPos = (this.width - PANEL_W) / 2;
        this.topPos = Math.max(TitledPanel.SIGN_RISE + UiStyle.GAP, (this.height - panelHeight) / 2);

        int x = leftPos + UiStyle.PAD;
        int y = TitledPanel.contentY(topPos);
        clearWidgets();

        UiSlider wild = new UiSlider(x, y, CONTENT_W, UiStyle.CONTROL_H, editing.wildSpawn(),
            value -> Component.translatable("screen.chiikawa.settings.wild_spawn", percent(value)),
            value -> move(new WorldSettings(snap(value), editing.chatter())));
        wild.active = editable();
        addRenderableWidget(wild);
        y += UiStyle.CONTROL_H + UiStyle.GAP;

        UiSlider chatter = new UiSlider(x, y, CONTENT_W, UiStyle.CONTROL_H,
            editing.chatter() / WorldSettings.MAX_CHATTER,
            value -> Component.translatable("screen.chiikawa.settings.chatter",
                percent(value * WorldSettings.MAX_CHATTER)),
            value -> move(new WorldSettings(editing.wildSpawn(), snap(value * WorldSettings.MAX_CHATTER))));
        chatter.active = editable();
        addRenderableWidget(chatter);
        y += UiStyle.CONTROL_H + UiStyle.GAP;

        this.hintY = y;
        y += hintLines * UiStyle.LINE + UiStyle.GAP_SECTION;

        boolean bubbles = ClientSettingsStore.current().speechBubbles();
        addRenderableWidget(UiButton.text(x, y, CONTENT_W, UiStyle.CONTROL_H,
            Component.translatable("screen.chiikawa.settings.speech_bubbles", Component.translatable(
                bubbles ? "screen.chiikawa.settings.on" : "screen.chiikawa.settings.off")),
            this::toggleBubbles));
        y += UiStyle.CONTROL_H + UiStyle.GAP;

        this.volumeY = y;
        y += volumeLines * UiStyle.LINE + UiStyle.GAP_SECTION;

        addRenderableWidget(UiButton.text(leftPos + (PANEL_W - DONE_W) / 2, y, DONE_W, UiStyle.CONTROL_H,
            CommonComponents.GUI_DONE, this::onClose));
    }

    /** A slider has moved: what it says is kept until the player lets go of it. */
    private void move(WorldSettings next) {
        this.editing = next;
        this.changed = true;
    }

    /** Tells the server what the sliders say, once, when the player has settled on it. */
    private void send() {
        if (changed && editable()) {
            Services.NETWORK.sendToServer(new ChangeWorldSettingsPayload(editing));
        }
        changed = false;
    }

    private void toggleBubbles() {
        ClientSettingsStore.set(new ClientSettings(!ClientSettingsStore.current().speechBubbles()));
        init();
    }

    /** A setting as a whole percent: the finest the sliders can set, so what is shown is what is sent. */
    private static float snap(double fraction) {
        return (float) Math.round(fraction * STEPS) / STEPS;
    }

    private static String percent(double fraction) {
        return Math.round(fraction * STEPS) + "%";
    }

    private static Component worldOnlyHint() {
        return Component.translatable("screen.chiikawa.settings.world_only");
    }

    private static Component readOnlyHint() {
        return Component.translatable("screen.chiikawa.settings.read_only");
    }

    private static Component volumeHint() {
        return Component.translatable("screen.chiikawa.settings.volume");
    }

    private List<String> wrap(Component text) {
        return TextWrap.wrap(text.getString(), CONTENT_W, this.font::width);
    }

    @Override
    public void tick() {
        super.tick();
        // The server said something new while this was open: another operator moved a slider,
        // or this player's rights changed. Not while a slider is held, which a rebuild would drop.
        if (!isDragging() && !changed && !ClientWorldSettings.current().equals(shown)) {
            init();
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseReleased(mouseX, mouseY, button);
        // Let go anywhere: a drag that ends outside the slider is still a change.
        send();
        return handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
        send();
        return handled;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Drawn here, right after the blur, so the panel is not smeared by it; see the music box.
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        DrawSurface surface = new GuiSurface(graphics, this.font);
        TitledPanel.draw(surface, leftPos, topPos, PANEL_W, panelHeight, this.title.getString());
        if (!editable()) {
            drawHint(surface, inWorld() ? readOnlyHint() : worldOnlyHint(), hintY);
        }
        drawHint(surface, volumeHint(), volumeY);
    }

    /** A quiet note, wrapped to the panel and centred. */
    private void drawHint(DrawSurface surface, Component text, int y) {
        for (String line : wrap(text)) {
            surface.drawText(line, leftPos + (PANEL_W - surface.textWidth(line)) / 2, y, UiTheme.TEXT_MUTED);
            y += UiStyle.LINE;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        // Escape in the middle of a drag: the change was made, so it is not lost.
        send();
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}
