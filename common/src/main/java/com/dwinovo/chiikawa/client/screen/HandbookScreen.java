package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.client.manual.ManualScene;
import com.dwinovo.chiikawa.client.ui.mc.GuiSurface;
import com.dwinovo.chiikawa.client.ui.mc.UiButton;
import com.dwinovo.chiikawa.manual.ManualLinks;
import com.dwinovo.chiikawa.manual.ManualPage;
import com.dwinovo.chiikawa.manual.ManualPages;
import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.TextWrap;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import com.dwinovo.chiikawa.ui.widget.TitledPanel;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * The handbook: a short guide with its chapters listed down the left and the one that is
 * picked on the right — a little scene the pets act out, {@link ManualScene}, the chapter's
 * text, and a line to the website for the rest.
 *
 * <p>Everything is on one screen. The scene gives up height to the text before the text has
 * to scroll, and the text scrolls rather than run off the bottom of a small window. The
 * panels are drawn with the background, under the buttons, as every panel of the mod's is.
 */
public class HandbookScreen extends Screen {
    private static final int INDEX_W = 142;
    private static final int CONTENT_W = 190;
    private static final int WIDTH = UiStyle.PAD + INDEX_W + UiStyle.PAD + CONTENT_W + UiStyle.PAD;
    private static final int MAX_HEIGHT = 256;
    /** How tall the scene is when the window has the room, and how short it may get when not. */
    private static final int SCENE_MAX = 84;
    private static final int SCENE_MIN = 38;
    /** Kept clear between the panel and the window's edges. */
    private static final int MARGIN = 4;
    private static final int ROW_H = 16;
    private static final int TITLE_LINE = UiStyle.LINE + UiStyle.GAP;
    private static final int LINK_H = 12;
    /** How far the wheel moves the text, in lines. */
    private static final int SCROLL_LINES = 2;
    private static final int BAR_W = 3;
    private static final int MARKER = 3;

    /** The chapter last read, kept for the session. */
    private static int lastChapter;

    private final List<ManualPage> chapters = ManualPages.all();
    private ManualScene scene = ManualScene.of(new ManualPage.Scene(List.of()));
    private List<String> lines = List.of();
    private int chapter;
    private int scroll;
    private int leftPos, topPos, panelHeight, contentY, contentH, sceneH, textH;

    public HandbookScreen() {
        super(Component.translatable("item.chiikawa.handbook"));
    }

    @Override
    protected void init() {
        this.panelHeight = Math.min(MAX_HEIGHT, this.height - TitledPanel.SIGN_RISE - 2 * MARGIN);
        this.leftPos = (this.width - WIDTH) / 2;
        this.topPos = Math.max(TitledPanel.SIGN_RISE + MARGIN, (this.height - panelHeight) / 2);
        this.contentY = TitledPanel.contentY(topPos);
        this.contentH = topPos + panelHeight - UiStyle.PAD - contentY;
        clearWidgets();
        for (int i = 0; i < chapters.size(); i++) {
            addRenderableWidget(chapterRow(i));
        }
        if (!chapters.isEmpty()) {
            addRenderableWidget(linkLine());
            show(lastChapter);
        }
    }

    private UiButton chapterRow(int index) {
        Component title = Component.translatable(chapters.get(index).title());
        return UiButton.painted(leftPos + UiStyle.PAD, contentY + index * ROW_H, INDEX_W, ROW_H - UiStyle.TIGHT, title,
            (surface, area, hovered, active) -> {
                boolean picked = index == chapter;
                if (picked) {
                    Ui.sticker(surface, area.x(), area.y(), area.width(), area.height(), Ui.CARD_RADIUS, UiTheme.ACCENT_PALE);
                    surface.fillRect(area.x() + UiStyle.GAP, UiStyle.centerIn(area.y(), area.height(), MARKER), MARKER,
                        MARKER, UiTheme.ACCENT);
                } else if (hovered) {
                    Ui.rowHighlight(surface, area);
                }
                Ui.textClipped(surface, title.getString(), area.x() + UiStyle.GAP + MARKER + UiStyle.GAP,
                    UiStyle.centerIn(area.y(), area.height(), surface.lineHeight()),
                    area.width() - 2 * UiStyle.GAP - MARKER - UiStyle.GAP, picked ? UiTheme.TEXT : UiTheme.TEXT_MUTED);
            }, () -> show(index));
    }

    /** The line at the bottom: the chapter's page on the website, asked about before it is opened. */
    private UiButton linkLine() {
        Component label = Component.translatable("manual.chiikawa.more");
        int width = this.font.width(label) + 2 * UiStyle.GAP;
        return UiButton.painted(contentLeft() + CONTENT_W - width, contentY + contentH - LINK_H, width, LINK_H, label,
            (surface, area, hovered, active) -> {
                if (hovered) {
                    Ui.rowHighlight(surface, area);
                }
                Ui.textCentered(surface, label.getString(), area, hovered ? UiTheme.TEXT : UiTheme.TEXT_MUTED);
            }, this::openWebsite);
    }

    private void openWebsite() {
        String url = ManualLinks.url(this.minecraft.getLanguageManager().getSelected(), current().link());
        // Vanilla's own way: asked first, the browser opened on yes, and back to this screen either way.
        ConfirmLinkScreen.confirmLinkNow(this, url);
    }

    private int contentLeft() {
        return leftPos + UiStyle.PAD + INDEX_W + UiStyle.PAD;
    }

    private ManualPage current() {
        return chapters.get(chapter);
    }

    /**
     * Picks a chapter and fits it to the window. The text takes the lines it needs, and the
     * scene takes what height is left, down to a floor: past that the text scrolls, so a
     * small window gets a smaller picture and a longer read rather than a panel run off its
     * bottom edge.
     */
    private void show(int wanted) {
        chapter = Mth.clamp(wanted, 0, chapters.size() - 1);
        lastChapter = chapter;
        scroll = 0;
        scene = ManualScene.of(current().scene());
        lines = TextWrap.wrap(Component.translatable(current().text()).getString(), CONTENT_W - BAR_W - UiStyle.GAP,
            this.font::width);
        int room = contentH - TITLE_LINE - 2 * UiStyle.GAP - LINK_H;
        this.sceneH = Mth.clamp(room - lines.size() * UiStyle.LINE, SCENE_MIN, SCENE_MAX);
        this.textH = room - sceneH;
    }

    private Rect sceneArea() {
        return new Rect(contentLeft(), contentY + TITLE_LINE, CONTENT_W, sceneH);
    }

    private Rect textArea() {
        return new Rect(contentLeft(), sceneArea().bottom() + UiStyle.GAP, CONTENT_W, textH);
    }

    private int maxScroll() {
        return Math.max(0, lines.size() * UiStyle.LINE - textH);
    }

    @Override
    public void tick() {
        super.tick();
        scene.tick();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        GuiSurface surface = new GuiSurface(graphics, this.font);
        TitledPanel.draw(surface, leftPos, topPos, WIDTH, panelHeight, this.title.getString());
        if (chapters.isEmpty()) {
            Ui.emptyState(surface, Component.translatable("screen.chiikawa.handbook.empty").getString(),
                leftPos + WIDTH / 2, contentY + UiStyle.PAD);
            return;
        }
        surface.drawText(Component.translatable(current().title()).getString(), contentLeft(), contentY, UiTheme.TEXT);

        // The screen is handed the time since the last frame, not how far into the tick
        // this frame is; the pets move by the tick, so they ask the game's own clock.
        float sceneTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Rect picture = sceneArea();
        drawFrame(surface, picture);
        scene.draw(graphics, surface, picture, sceneTick);
        drawText(graphics, surface);
    }

    /** A manga panel: sky above a strip of grass, inside the ink line. */
    private static void drawFrame(DrawSurface surface, Rect scene) {
        Ui.sticker(surface, scene.x(), scene.y(), scene.width(), scene.height(), Ui.WELL_RADIUS, UiTheme.SKY_PALE);
        surface.fillRect(scene.x() + 2, scene.bottom() - ManualScene.GROUND, scene.width() - 4,
            ManualScene.GROUND - 2, UiTheme.LEAF_PALE);
    }

    /** The chapter's text, cut to its box and moved by the wheel, with a bar beside it when there is more. */
    private void drawText(GuiGraphics graphics, DrawSurface surface) {
        Rect area = textArea();
        graphics.enableScissor(area.x(), area.y(), area.right(), area.bottom());
        for (int i = 0; i < lines.size(); i++) {
            int y = area.y() + i * UiStyle.LINE - scroll;
            if (y + UiStyle.LINE > area.y() && y < area.bottom()) {
                surface.drawText(lines.get(i), area.x(), y, UiTheme.TEXT);
            }
        }
        graphics.disableScissor();
        int total = lines.size() * UiStyle.LINE;
        if (total > area.height()) {
            int x = area.right() - BAR_W;
            surface.fillRect(x, area.y(), BAR_W, area.height(), UiTheme.DIVIDER);
            int thumb = Math.max(UiStyle.LINE, area.height() * area.height() / total);
            int at = area.y() + (area.height() - thumb) * scroll / maxScroll();
            surface.fillRect(x, at, BAR_W, thumb, UiTheme.ACCENT);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!chapters.isEmpty() && maxScroll() > 0) {
            scroll = Mth.clamp(scroll - (int) Math.signum(scrollY) * SCROLL_LINES * UiStyle.LINE, 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!chapters.isEmpty() && (keyCode == InputConstants.KEY_UP || keyCode == InputConstants.KEY_DOWN)) {
            show(chapter + (keyCode == InputConstants.KEY_DOWN ? 1 : -1));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
