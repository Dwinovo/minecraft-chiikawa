package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.music.MusicTrackStatus;
import com.dwinovo.chiikawa.music.MusicTrackView;
import com.dwinovo.chiikawa.network.MusicPayloads.MusicCatalogRequestPayload;
import com.dwinovo.chiikawa.network.MusicPayloads.MusicBoxSelectTrackPayload;
import com.dwinovo.chiikawa.platform.Services;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MusicBoxScreen extends Screen {
    private static final int ROWS = 8;
    private static final int PANEL_WIDTH = 260;
    private static final int ROW_HEIGHT = 22;
    private static final int PANEL_BACKGROUND = 0xB8000000;
    private static final int PANEL_INNER_BACKGROUND = 0x66000000;
    private static final int PANEL_BORDER = 0x66FFFFFF;
    private static final int PANEL_SEPARATOR = 0x33FFFFFF;

    private final int handIndex;
    private List<MusicTrackView> tracks;
    private int page;

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
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearWidgets();
        clampPage();
        int left = (width - PANEL_WIDTH) / 2;
        int top = Math.max(28, (height - 220) / 2);
        int start = page * ROWS;
        int end = Math.min(tracks.size(), start + ROWS);
        for (int i = start; i < end; i++) {
            MusicTrackView track = tracks.get(i);
            Button button = musicButton(rowTitle(track), ignored -> select(track),
                    left, top + 26 + (i - start) * ROW_HEIGHT, PANEL_WIDTH, 20);
            button.active = track.status() == MusicTrackStatus.READY;
            addRenderableWidget(button);
        }

        int pages = Math.max(1, (tracks.size() + ROWS - 1) / ROWS);
        Button previous = musicButton(Component.literal("<"), ignored -> {
            if (page > 0) {
                page--;
                rebuildButtons();
            }
        }, left, top + 210, 32, 20);
        previous.active = page > 0;
        addRenderableWidget(previous);

        Button next = musicButton(Component.literal(">"), ignored -> {
            if (page + 1 < pages) {
                page++;
                rebuildButtons();
            }
        }, left + PANEL_WIDTH - 32, top + 210, 32, 20);
        next.active = page + 1 < pages;
        addRenderableWidget(next);

        addRenderableWidget(musicButton(Component.translatable("screen.chiikawa.music_box.reload"), ignored -> {
            requestCatalog(true);
        }, left + (PANEL_WIDTH - 72) / 2, top + 210, 72, 20));
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

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = (width - PANEL_WIDTH) / 2;
        int top = Math.max(28, (height - 220) / 2);
        graphics.fill(left - 12, top - 12, left + PANEL_WIDTH + 12, top + 238, PANEL_BACKGROUND);
        drawOutline(graphics, left - 12, top - 12, PANEL_WIDTH + 24, 250, PANEL_BORDER);
        graphics.fill(left - 6, top + 20, left + PANEL_WIDTH + 6, top + 203, PANEL_INNER_BACKGROUND);
        graphics.fill(left - 12, top + 18, left + PANEL_WIDTH + 12, top + 19, PANEL_SEPARATOR);
        graphics.fill(left - 12, top + 205, left + PANEL_WIDTH + 12, top + 206, PANEL_SEPARATOR);
        graphics.drawCenteredString(font, title, width / 2, top, 0xFFE8E1D2);
        if (tracks.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.chiikawa.music_box.empty_catalog"),
                    width / 2, top + 70, 0xFFA8A8A8);
        }
        int pages = Math.max(1, (tracks.size() + ROWS - 1) / ROWS);
        if (hasFfmpegFailure()) {
            graphics.drawCenteredString(font, Component.translatable("screen.chiikawa.music_box.ffmpeg_missing"),
                    width / 2, top + 188, 0xFFFFB36B);
            graphics.drawCenteredString(font, Component.translatable("screen.chiikawa.music_box.ffmpeg_hint"),
                    width / 2, top + 198, 0xFFA8A8A8);
        }
        graphics.drawCenteredString(font, Component.literal((page + 1) + " / " + pages),
                width / 2, top + 234, 0xFFA8A8A8);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static Button musicButton(Component message, Button.OnPress onPress, int x, int y, int width, int height) {
        return new MusicBoxButton(x, y, width, height, message, onPress);
    }

    private static void drawOutline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private void requestCatalog(boolean rescan) {
        Services.NETWORK.sendToServer(new MusicCatalogRequestPayload(handIndex, rescan));
    }

    private void clampPage() {
        int pages = Math.max(1, (tracks.size() + ROWS - 1) / ROWS);
        page = Math.max(0, Math.min(page, pages - 1));
    }

    private boolean hasFfmpegFailure() {
        return tracks.stream()
            .anyMatch(track -> track.status() == MusicTrackStatus.FAILED
                && track.error().toLowerCase(Locale.ROOT).contains("ffmpeg"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class MusicBoxButton extends Button {
        private static final int BACKGROUND = 0x8A000000;
        private static final int BACKGROUND_HOVERED = 0xB0202020;
        private static final int BACKGROUND_DISABLED = 0x52000000;
        private static final int BORDER = 0x77FFFFFF;
        private static final int BORDER_HOVERED = 0xFFE8E1D2;
        private static final int BORDER_DISABLED = 0x33FFFFFF;
        private static final int HIGHLIGHT = 0x33FFFFFF;

        private MusicBoxButton(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
            super(x, y, width, height, message, onPress, defaultNarrationSupplier -> defaultNarrationSupplier.get());
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            int width = getWidth();
            int height = getHeight();
            boolean hovered = active && isHoveredOrFocused();
            int background = active ? hovered ? BACKGROUND_HOVERED : BACKGROUND : BACKGROUND_DISABLED;
            int border = active ? hovered ? BORDER_HOVERED : BORDER : BORDER_DISABLED;
            graphics.fill(x, y, x + width, y + height, background);
            drawOutline(graphics, x, y, width, height, border);
            if (hovered) {
                graphics.fill(x + 1, y + 1, x + width - 1, y + 2, HIGHLIGHT);
            }
            renderString(graphics, Minecraft.getInstance().font, active ? 0xFFE8E1D2 : 0xFF777777);
        }
    }
}
