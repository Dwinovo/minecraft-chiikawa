package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.client.ui.mc.GuiSurface;
import com.dwinovo.chiikawa.client.ui.mc.UiButton;
import com.dwinovo.chiikawa.music.MusicTrackStatus;
import com.dwinovo.chiikawa.music.MusicTrackView;
import com.dwinovo.chiikawa.network.MusicPayloads.MusicBoxSelectTrackPayload;
import com.dwinovo.chiikawa.network.MusicPayloads.MusicCatalogRequestPayload;
import com.dwinovo.chiikawa.platform.Services;
import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import com.dwinovo.chiikawa.ui.widget.Arrow;
import com.dwinovo.chiikawa.ui.widget.Badge;
import com.dwinovo.chiikawa.ui.widget.TitledPanel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

/**
 * The music box: the tracks found in the player's own music folder, one to a row, picked
 * by clicking. Drawn from the {@code chiikawa-ui} library — panel, rows, badges, arrows —
 * so adding a track never needed art and now neither does anything else on it.
 *
 * <p>A track that is still being imported, or that could not be read, stays on the list
 * with a badge saying so rather than vanishing: a file the player just dropped in and
 * cannot find again is worse than one that says it went wrong.
 */
public class MusicBoxScreen extends Screen {
    private static final int PANEL_W = 236;
    private static final int ROWS = 8;
    /** Rows sit against one another; the tint under the cursor is what separates them. */
    private static final int ROW_H = UiStyle.ROW_H;

    private final int handIndex;
    private List<MusicTrackView> tracks;
    private int page;
    private int leftPos, topPos, panelHeight;
    private int listY, footerY;

    public MusicBoxScreen(int handIndex, List<MusicTrackView> tracks) {
        super(Component.translatable("screen.chiikawa.music_box.title"));
        this.handIndex = handIndex;
        this.tracks = List.copyOf(tracks);
    }

    public int handIndex() {
        return handIndex;
    }

    public void replaceTracks(List<MusicTrackView> nextTracks) {
        this.tracks = List.copyOf(nextTracks);
        clampPage();
        init(this.minecraft, this.width, this.height);
    }

    @Override
    protected void init() {
        int hint = hasFailedTracks() ? UiStyle.LINE + UiStyle.GAP : 0;
        this.panelHeight = UiStyle.TITLE_H + UiStyle.PAD + ROWS * ROW_H + UiStyle.PAD
            + hint + UiStyle.CONTROL_H + UiStyle.PAD;
        this.leftPos = (this.width - PANEL_W) / 2;
        this.topPos = (this.height - panelHeight) / 2;
        this.listY = topPos + UiStyle.TITLE_H + UiStyle.PAD;
        this.footerY = topPos + panelHeight - UiStyle.PAD - UiStyle.CONTROL_H;
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearWidgets();
        clampPage();
        int start = page * ROWS;
        int end = Math.min(tracks.size(), start + ROWS);
        for (int i = start; i < end; i++) {
            MusicTrackView track = tracks.get(i);
            RowButton row = new RowButton(leftPos + UiStyle.PAD, listY + (i - start) * ROW_H, track);
            row.active = track.status() == MusicTrackStatus.READY;
            addRenderableWidget(row);
        }

        int pages = pages();
        UiButton previous = new UiButton(leftPos + UiStyle.PAD, footerY, UiStyle.CONTROL_H, UiStyle.CONTROL_H,
            Component.translatable("screen.chiikawa.music_box.previous_page"),
            (surface, area, argb) -> Arrow.draw(surface, area, true, argb),
            () -> turnTo(page - 1));
        previous.active = page > 0;
        addRenderableWidget(previous);

        UiButton next = new UiButton(leftPos + PANEL_W - UiStyle.PAD - UiStyle.CONTROL_H, footerY,
            UiStyle.CONTROL_H, UiStyle.CONTROL_H,
            Component.translatable("screen.chiikawa.music_box.next_page"),
            (surface, area, argb) -> Arrow.draw(surface, area, false, argb),
            () -> turnTo(page + 1));
        next.active = page + 1 < pages;
        addRenderableWidget(next);

        // The two doings sit in the middle, between the two page arrows.
        Component folder = Component.translatable("screen.chiikawa.music_box.open_folder");
        Component reload = Component.translatable("screen.chiikawa.music_box.reload");
        int folderWidth = buttonWidth(folder);
        int reloadWidth = buttonWidth(reload);
        int doingsX = leftPos + (PANEL_W - folderWidth - UiStyle.GAP - reloadWidth) / 2;
        addRenderableWidget(UiButton.text(doingsX, footerY, folderWidth, UiStyle.CONTROL_H, folder,
            this::openMusicFolder));
        addRenderableWidget(UiButton.text(doingsX + folderWidth + UiStyle.GAP, footerY, reloadWidth,
            UiStyle.CONTROL_H, reload, () -> requestCatalog(true)));
    }

    private int buttonWidth(Component label) {
        return this.font.width(label) + 2 * UiStyle.PAD;
    }

    private void turnTo(int wanted) {
        page = Math.max(0, Math.min(wanted, pages() - 1));
        rebuildButtons();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Screen.render() runs renderBackground() — which blurs the whole framebuffer via
        // GameRenderer.processBlurEffect() — at its START. Draw the panel HERE, right after
        // that blur, so the panel isn't smeared. (Widgets render later in super.render() and
        // already stayed sharp, which is why only the panel looked fuzzy before.)
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        DrawSurface surface = new GuiSurface(graphics, this.font);
        int contentY = TitledPanel.draw(surface, leftPos, topPos, PANEL_W, panelHeight, this.title.getString());
        Ui.textRight(surface, (page + 1) + "/" + pages(), leftPos + PANEL_W - UiStyle.PAD,
            UiStyle.centerIn(topPos, UiStyle.TITLE_H, surface.lineHeight()), UiTheme.TEXT_MUTED);

        if (tracks.isEmpty()) {
            int middle = contentY + (ROWS * ROW_H - UiStyle.LINE) / 2;
            Ui.emptyState(surface, Component.translatable("screen.chiikawa.music_box.empty_catalog").getString(),
                leftPos + PANEL_W / 2, middle);
            Ui.emptyState(surface, Component.translatable("screen.chiikawa.music_box.empty_hint").getString(),
                leftPos + PANEL_W / 2, middle + UiStyle.LINE + UiStyle.GAP);
        } else if (hasFailedTracks()) {
            Ui.emptyState(surface, Component.translatable("screen.chiikawa.music_box.format_hint").getString(),
                leftPos + PANEL_W / 2, footerY - UiStyle.GAP - UiStyle.LINE);
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void openMusicFolder() {
        try {
            Path folder = Minecraft.getInstance().gameDirectory.toPath().resolve("config/chiikawa/music");
            Files.createDirectories(folder);
            Util.getPlatform().openPath(folder);
        } catch (Exception ex) {
            Constants.LOG.warn("[chiikawa-music] Failed to open music folder", ex);
        }
    }

    /** What a row that is not ready yet has to say for itself, or nothing when it is. */
    private static Component state(MusicTrackView track) {
        return switch (track.status()) {
            case IMPORTING -> Component.translatable("screen.chiikawa.music_box.importing");
            case FAILED -> Component.translatable("screen.chiikawa.music_box.failed");
            default -> null;
        };
    }

    private void select(MusicTrackView track) {
        if (track.status() != MusicTrackStatus.READY) {
            return;
        }
        Services.NETWORK.sendToServer(new MusicBoxSelectTrackPayload(handIndex, track.trackId()));
        onClose();
    }

    private void requestCatalog(boolean rescan) {
        Services.NETWORK.sendToServer(new MusicCatalogRequestPayload(handIndex, rescan));
    }

    private int pages() {
        return Math.max(1, (tracks.size() + ROWS - 1) / ROWS);
    }

    private void clampPage() {
        page = Math.max(0, Math.min(page, pages() - 1));
    }

    private boolean hasFailedTracks() {
        return tracks.stream().anyMatch(track -> track.status() == MusicTrackStatus.FAILED);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** One track: its name, a badge if it is not ready, and a tint under the cursor. */
    private final class RowButton extends Button {
        private final MusicTrackView track;

        private RowButton(int x, int y, MusicTrackView track) {
            super(x, y, PANEL_W - 2 * UiStyle.PAD, ROW_H, Component.literal(track.title()),
                button -> MusicBoxScreen.this.select(track), supplier -> supplier.get());
            this.track = track;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            DrawSurface surface = new GuiSurface(graphics, MusicBoxScreen.this.font);
            Rect area = new Rect(getX(), getY(), getWidth(), getHeight());
            if (active && isHoveredOrFocused()) {
                Ui.rowHighlight(surface, area);
            }
            int textY = UiStyle.centerIn(area.y(), area.height(), surface.lineHeight());
            int room = area.width() - 2 * UiStyle.GAP;

            Component state = state(track);
            if (state != null) {
                String badge = state.getString();
                int badgeWidth = Badge.width(surface, badge);
                Badge.draw(surface, badge, area.right() - UiStyle.GAP - badgeWidth,
                    UiStyle.centerIn(area.y(), area.height(), Badge.height(surface)),
                    track.status() == MusicTrackStatus.FAILED ? UiTheme.ACCENT : UiTheme.TEXT_MUTED);
                room -= badgeWidth + UiStyle.GAP;
            }
            Ui.textClipped(surface, track.title(), area.x() + UiStyle.GAP, textY, room,
                active ? UiTheme.TEXT : UiTheme.TEXT_MUTED);
        }
    }
}
