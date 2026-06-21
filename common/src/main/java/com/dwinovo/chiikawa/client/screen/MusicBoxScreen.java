package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.music.MusicTrackStatus;
import com.dwinovo.chiikawa.music.MusicTrackView;
import com.dwinovo.chiikawa.network.MusicPayloads.MusicCatalogRequestPayload;
import com.dwinovo.chiikawa.network.MusicPayloads.MusicBoxSelectTrackPayload;
import com.dwinovo.chiikawa.platform.Services;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Chiikawa music box screen — a cozy cream panel listing the local music
 * catalog. The visual chrome (panel frame, control-button frames, folder /
 * reload icons, page arrows) is baked into {@code music_box.png}; the list
 * rows and their hover state are blitted at runtime from the {@code row} /
 * {@code row_selected} sprites in that same atlas, so adding tracks never
 * needs new art. Slice coordinates mirror {@code art/ui-kit/music_box.aseprite}.
 */
public class MusicBoxScreen extends Screen {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/music_box.png");
    private static final int ATLAS_W = 256, ATLAS_H = 320;

    private static final int PANEL_W = 240, PANEL_H = 248;
    private static final int PANEL_U = 8, PANEL_V = 4;        // panel sprite in the atlas

    private static final int ROWS = 8;
    private static final int ROW_W = 212, ROW_H = 20, ROW_PITCH = 22;
    private static final int ROW_X = 14, ROW_Y = 30;         // first row, relative to the panel
    private static final int ROW_U = 8, ROW_V = 256, ROWSEL_V = 280;
    private static final int ROW_TEXT_X = 18, ROW_TEXT_Y = 6;

    private static final int CTRL_Y = 222;                    // control row, relative to the panel
    private static final int PREV_X = 14, NEXT_X = 204, ARROW_W = 22, ARROW_H = 18;
    private static final int FOLDER_X = 98, RELOAD_X = 124, ICON_BTN = 18;

    private static final int TITLE_Y = 9;
    private static final int TEXT = 0xFF5A3A2B;       // theme dark brown
    private static final int TEXT_DIM = 0xFFB39474;   // muted (disabled / page)
    private static final int TEXT_HINT = 0xFF8A6A52;
    private static final int TEXT_FAIL = 0xFFCF7E5A;
    private static final int HOVER_TINT = 0x33FFFFFF;
    private static final int DISABLED_TINT = 0x66FCEFD3;

    private final int handIndex;
    private List<MusicTrackView> tracks;
    private int page;
    private int leftPos, topPos;

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
        rebuildButtons();
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - PANEL_W) / 2;
        this.topPos = (this.height - PANEL_H) / 2;
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearWidgets();
        clampPage();
        int start = page * ROWS;
        int end = Math.min(tracks.size(), start + ROWS);
        for (int i = start; i < end; i++) {
            MusicTrackView track = tracks.get(i);
            int row = i - start;
            RowButton button = new RowButton(leftPos + ROW_X, topPos + ROW_Y + row * ROW_PITCH, track, ignored -> select(track));
            button.active = track.status() == MusicTrackStatus.READY;
            addRenderableWidget(button);
        }

        int pages = pages();
        IconButton prev = new IconButton(leftPos + PREV_X, topPos + CTRL_Y, ARROW_W, ARROW_H, ignored -> {
            if (page > 0) { page--; rebuildButtons(); }
        }, null);
        prev.active = page > 0;
        addRenderableWidget(prev);

        IconButton next = new IconButton(leftPos + NEXT_X, topPos + CTRL_Y, ARROW_W, ARROW_H, ignored -> {
            if (page + 1 < pages) { page++; rebuildButtons(); }
        }, null);
        next.active = page + 1 < pages;
        addRenderableWidget(next);

        addRenderableWidget(new IconButton(leftPos + FOLDER_X, topPos + CTRL_Y, ICON_BTN, ICON_BTN,
                ignored -> openMusicFolder(), Component.translatable("screen.chiikawa.music_box.open_folder")));
        addRenderableWidget(new IconButton(leftPos + RELOAD_X, topPos + CTRL_Y, ICON_BTN, ICON_BTN,
                ignored -> requestCatalog(true), Component.translatable("screen.chiikawa.music_box.reload")));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x55000000);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos,
                (float) PANEL_U, (float) PANEL_V, PANEL_W, PANEL_H, ATLAS_W, ATLAS_H);

        Component decoratedTitle = Component.literal("♪ ").append(title).append(Component.literal(" ♪"));
        centered(graphics, decoratedTitle, leftPos + PANEL_W / 2, topPos + TITLE_Y, TEXT);

        int pages = pages();
        centered(graphics, Component.literal((page + 1) + "/" + pages), leftPos + 222, topPos + TITLE_Y, TEXT_DIM);

        if (tracks.isEmpty()) {
            centered(graphics, Component.translatable("screen.chiikawa.music_box.empty_catalog"),
                    leftPos + PANEL_W / 2, topPos + 104, TEXT_HINT);
            centered(graphics, Component.translatable("screen.chiikawa.music_box.empty_hint"),
                    leftPos + PANEL_W / 2, topPos + 118, TEXT_HINT);
        } else if (hasFailedTracks()) {
            centered(graphics, Component.translatable("screen.chiikawa.music_box.format_hint"),
                    leftPos + PANEL_W / 2, topPos + 208, TEXT_FAIL);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** Centered text without the default drop shadow — shadows look muddy on the cream panel. */
    private void centered(GuiGraphics graphics, Component text, int centerX, int y, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
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

    private Component rowTitle(MusicTrackView track) {
        if (track.status() == MusicTrackStatus.IMPORTING) {
            return Component.literal(track.title()).append(" - ")
                    .append(Component.translatable("screen.chiikawa.music_box.importing"));
        }
        if (track.status() == MusicTrackStatus.FAILED) {
            return Component.literal(track.title()).append(" - ")
                    .append(Component.translatable("screen.chiikawa.music_box.failed"));
        }
        return Component.literal(track.title());
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

    /** Full-width list row: blits the cream / pink row sprite plus the track title. */
    private final class RowButton extends Button {
        private final MusicTrackView track;

        private RowButton(int x, int y, MusicTrackView track, Button.OnPress onPress) {
            super(x, y, ROW_W, ROW_H, Component.empty(), onPress, supplier -> supplier.get());
            this.track = track;
        }

        @Override
        protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int v = (active && isHoveredOrFocused()) ? ROWSEL_V : ROW_V;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, getX(), getY(),
                    (float) ROW_U, (float) v, ROW_W, ROW_H, ATLAS_W, ATLAS_H);
            graphics.drawString(font, rowTitle(track), getX() + ROW_TEXT_X, getY() + ROW_TEXT_Y,
                    active ? TEXT : TEXT_DIM, false);
        }
    }

    /** Click + tooltip hit area over a control button whose frame/icon is baked into the panel. */
    private final class IconButton extends Button {
        private IconButton(int x, int y, int w, int h, Button.OnPress onPress, Component tooltip) {
            super(x, y, w, h, Component.empty(), onPress, supplier -> supplier.get());
            if (tooltip != null) {
                setTooltip(Tooltip.create(tooltip));
            }
        }

        @Override
        protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (!active) {
                graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), DISABLED_TINT);
            } else if (isHoveredOrFocused()) {
                graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), HOVER_TINT);
            }
        }
    }
}
