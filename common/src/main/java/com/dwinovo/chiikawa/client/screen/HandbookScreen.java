package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.client.manual.ManualScene;
import com.dwinovo.chiikawa.client.ui.mc.GuiSurface;
import com.dwinovo.chiikawa.client.ui.mc.UiButton;
import com.dwinovo.chiikawa.manual.ManualPage;
import com.dwinovo.chiikawa.manual.ManualPages;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.TextWrap;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import com.dwinovo.chiikawa.ui.widget.Arrow;
import com.dwinovo.chiikawa.ui.widget.TitledPanel;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The handbook: a page at a time, each a four-panel strip read left to right and top to
 * bottom, the way a strip is. A panel is not a picture of the pets but the pets, acting it
 * out — {@link ManualScene} — with a line underneath saying what is going on.
 *
 * <p>The panels are drawn with the background, under the page buttons, as every panel of
 * the mod's is.
 */
public class HandbookScreen extends Screen {
    private static final int PANEL_W = 136;
    /** How tall a panel's scene is when the window has the room, and how short it may get when not. */
    private static final int SCENE_MAX = 62;
    private static final int SCENE_MIN = 36;
    private static final int CAPTION_LINES = 3;
    /** Kept clear between the panel and the window's edges. */
    private static final int MARGIN = 4;
    private static final int COLUMNS = 2;
    private static final int ROWS = ManualPage.PANELS / COLUMNS;
    private static final int PANEL_GAP = 6;
    private static final int WIDTH = COLUMNS * PANEL_W + (COLUMNS - 1) * PANEL_GAP + 2 * UiStyle.PAD;

    private final List<ManualPage> pages = ManualPages.all();
    private List<ManualScene> scenes = List.of();
    private int page;
    private int leftPos, topPos, panelHeight, contentY, footerY, captionH, sceneH;

    public HandbookScreen() {
        super(Component.translatable("item.chiikawa.handbook"));
    }

    @Override
    protected void init() {
        turnTo(page);
    }

    private void turnTo(int wanted) {
        page = pages.isEmpty() ? 0 : Math.max(0, Math.min(wanted, pages.size() - 1));
        scenes = pages.isEmpty() ? List.of() : current().panels().stream().map(ManualScene::of).toList();
        layOut();
        clearWidgets();
        UiButton previous = new UiButton(leftPos + UiStyle.PAD, footerY, UiStyle.CONTROL_H, UiStyle.CONTROL_H,
            Component.translatable("screen.chiikawa.handbook.previous_page"),
            (surface, area, argb) -> Arrow.draw(surface, area, true, argb), () -> turnTo(page - 1));
        previous.active = page > 0;
        addRenderableWidget(previous);
        UiButton next = new UiButton(leftPos + WIDTH - UiStyle.PAD - UiStyle.CONTROL_H, footerY,
            UiStyle.CONTROL_H, UiStyle.CONTROL_H, Component.translatable("screen.chiikawa.handbook.next_page"),
            (surface, area, argb) -> Arrow.draw(surface, area, false, argb), () -> turnTo(page + 1));
        next.active = page + 1 < pages.size();
        addRenderableWidget(next);
    }

    /**
     * Fits the page to the window. The captions take the lines this page's longest one
     * needs, and the scenes take what height is left, down to a floor — so a small window
     * gets smaller pictures rather than a panel run off its bottom edge, and the sign
     * hanging over the top stays in view.
     */
    private void layOut() {
        int lines = 1;
        if (!pages.isEmpty()) {
            for (ManualPage.Panel panel : current().panels()) {
                String caption = Component.translatable(panel.caption()).getString();
                lines = Math.max(lines, TextWrap.wrap(caption, PANEL_W, this.font::width).size());
            }
        }
        this.captionH = Math.min(lines, CAPTION_LINES) * this.font.lineHeight;
        int around = UiStyle.TITLE_H + UiStyle.PAD + ROWS * (UiStyle.TIGHT + captionH) + ROWS * UiStyle.GAP
            + UiStyle.CONTROL_H + UiStyle.PAD;
        int room = this.height - TitledPanel.SIGN_RISE - 2 * MARGIN - around;
        this.sceneH = Math.max(SCENE_MIN, Math.min(SCENE_MAX, room / ROWS));
        this.panelHeight = around + ROWS * sceneH;
        this.leftPos = (this.width - WIDTH) / 2;
        this.topPos = Math.max(TitledPanel.SIGN_RISE + MARGIN, (this.height - panelHeight) / 2);
        this.contentY = TitledPanel.contentY(topPos);
        this.footerY = topPos + panelHeight - UiStyle.PAD - UiStyle.CONTROL_H;
    }

    private ManualPage current() {
        return pages.get(page);
    }

    @Override
    public void tick() {
        super.tick();
        scenes.forEach(ManualScene::tick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        GuiSurface surface = new GuiSurface(graphics, this.font);
        if (pages.isEmpty()) {
            TitledPanel.draw(surface, leftPos, topPos, WIDTH, panelHeight, this.title.getString());
            Ui.emptyState(surface, Component.translatable("screen.chiikawa.handbook.empty").getString(),
                leftPos + WIDTH / 2, contentY + UiStyle.PAD);
            return;
        }
        TitledPanel.draw(surface, leftPos, topPos, WIDTH, panelHeight,
            Component.translatable(current().title()).getString());
        Ui.textRight(surface, (page + 1) + "/" + pages.size(), leftPos + WIDTH - UiStyle.PAD,
            UiStyle.centerIn(topPos, UiStyle.TITLE_H, surface.lineHeight()), UiTheme.TEXT_MUTED);

        // The screen is handed the time since the last frame, not how far into the tick
        // this frame is; the pets move by the tick, so they ask the game's own clock.
        float sceneTick = Minecraft.getInstance().getFrameTime();
        List<ManualPage.Panel> panels = current().panels();
        for (int i = 0; i < panels.size(); i++) {
            Rect scene = sceneAt(i);
            drawFrame(surface, scene);
            scenes.get(i).draw(graphics, surface, scene, sceneTick);
            drawCaption(surface, Component.translatable(panels.get(i).caption()).getString(), scene);
        }
    }

    /** A manga panel: sky above a strip of grass, inside the ink line. */
    private static void drawFrame(GuiSurface surface, Rect scene) {
        Ui.sticker(surface, scene.x(), scene.y(), scene.width(), scene.height(), Ui.WELL_RADIUS, UiTheme.SKY_PALE);
        surface.fillRect(scene.x() + 2, scene.bottom() - ManualScene.GROUND, scene.width() - 4,
            ManualScene.GROUND - 2, UiTheme.LEAF_PALE);
    }

    private void drawCaption(GuiSurface surface, String caption, Rect scene) {
        List<String> lines = TextWrap.wrap(caption, scene.width(), surface::textWidth);
        int y = scene.bottom() + UiStyle.TIGHT;
        for (int line = 0; line < Math.min(lines.size(), CAPTION_LINES); line++) {
            surface.drawText(lines.get(line), scene.x() + (scene.width() - surface.textWidth(lines.get(line))) / 2,
                y + line * surface.lineHeight(), UiTheme.TEXT);
        }
    }

    private Rect sceneAt(int index) {
        int column = index % COLUMNS;
        int row = index / COLUMNS;
        int rowHeight = sceneH + UiStyle.TIGHT + captionH + UiStyle.GAP;
        return new Rect(leftPos + UiStyle.PAD + column * (PANEL_W + PANEL_GAP), contentY + row * rowHeight,
            PANEL_W, sceneH);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
