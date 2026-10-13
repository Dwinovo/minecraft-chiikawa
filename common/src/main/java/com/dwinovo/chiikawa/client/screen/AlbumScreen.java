package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.client.photo.ClientPhotoCache;
import com.dwinovo.chiikawa.client.ui.mc.GuiSurface;
import com.dwinovo.chiikawa.client.ui.mc.UiButton;
import com.dwinovo.chiikawa.photo.PhotoData;
import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.TextWrap;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import com.dwinovo.chiikawa.ui.widget.Arrow;
import com.dwinovo.chiikawa.ui.widget.TitledPanel;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * A roll of film, looked through: the shots on it one to a page, in a paper frame like the
 * handbook's, with what was taken written underneath and arrows to turn the page. Each
 * picture is fetched from the server the first time its page is turned to, and until it is
 * here the frame says it is being developed.
 */
public class AlbumScreen extends Screen {
    /** What size the picture is taken to be: the camera's own, 4 by 3. */
    private static final int PICTURE_WIDTH = 320;
    private static final int PICTURE_HEIGHT = 240;
    /** The most a picture is blown up by; past that it is only a blur. */
    private static final double MAX_SCALE = 2.0;
    /** The lines the caption is given room for. */
    private static final int CAPTION_LINES = 2;
    private static final int MIN_PICTURE_WIDTH = 96;
    /** Kept clear between the frame and the window's edges. */
    private static final int MARGIN = 4;

    private final List<PhotoData> shots;
    private int page;
    private int leftPos, topPos, panelWidth, panelHeight, footerY;
    /** Where on the page the picture goes: the frame it is fitted into, whatever its own shape. */
    private Rect frame = new Rect(0, 0, 0, 0);

    public AlbumScreen(List<PhotoData> shots) {
        super(Component.translatable("item.chiikawa.film"));
        this.shots = List.copyOf(shots);
    }

    @Override
    protected void init() {
        layOut();
        UiButton previous = new UiButton(leftPos + UiStyle.PAD, footerY, UiStyle.CONTROL_H, UiStyle.CONTROL_H,
            Component.translatable("screen.chiikawa.album.previous_page"),
            (surface, area, argb) -> Arrow.draw(surface, area, true, argb), () -> turnTo(page - 1));
        previous.active = page > 0;
        addRenderableWidget(previous);
        UiButton next = new UiButton(leftPos + panelWidth - UiStyle.PAD - UiStyle.CONTROL_H, footerY,
            UiStyle.CONTROL_H, UiStyle.CONTROL_H, Component.translatable("screen.chiikawa.album.next_page"),
            (surface, area, argb) -> Arrow.draw(surface, area, false, argb), () -> turnTo(page + 1));
        next.active = page + 1 < shots.size();
        addRenderableWidget(next);
        if (!shots.isEmpty()) {
            ClientPhotoCache.state(current().photo());
        }
    }

    private PhotoData current() {
        return shots.get(page);
    }

    private void turnTo(int wanted) {
        page = Math.max(0, Math.min(wanted, shots.size() - 1));
        // The buttons are built for the page, which of them may be pressed being its to say.
        rebuildWidgets();
    }

    /** Fits the frame to the window: the picture as big as the room allows, the caption under it. */
    private void layOut() {
        int roomWidth = this.width - 2 * MARGIN - 2 * UiStyle.PAD;
        // What is left of the height once the title, the caption, the page buttons and the paper round them are paid for.
        int roomHeight = this.height - 2 * MARGIN - TitledPanel.SIGN_RISE - UiStyle.TITLE_H - 3 * UiStyle.PAD
            - 2 * UiStyle.GAP - CAPTION_LINES * UiStyle.LINE - UiStyle.CONTROL_H;
        double scale = Math.min(MAX_SCALE, Math.min((double) roomWidth / PICTURE_WIDTH, (double) roomHeight / PICTURE_HEIGHT));
        int pictureWidth = Math.max(MIN_PICTURE_WIDTH, (int) (PICTURE_WIDTH * scale));
        int pictureHeight = pictureWidth * PICTURE_HEIGHT / PICTURE_WIDTH;

        this.panelWidth = pictureWidth + 2 * UiStyle.PAD;
        this.panelHeight = UiStyle.TITLE_H + UiStyle.PAD + pictureHeight + UiStyle.GAP + CAPTION_LINES * UiStyle.LINE
            + UiStyle.GAP + UiStyle.CONTROL_H + UiStyle.PAD;
        this.leftPos = (this.width - panelWidth) / 2;
        this.topPos = Math.max(TitledPanel.SIGN_RISE + MARGIN, (this.height - panelHeight) / 2);
        this.footerY = topPos + panelHeight - UiStyle.PAD - UiStyle.CONTROL_H;
        this.frame = new Rect(leftPos + UiStyle.PAD, TitledPanel.contentY(topPos), pictureWidth, pictureHeight);
    }

    /**
     * 1.20.1's {@code Screen.render} draws only the widgets, so the background, with the
     * panel on it, goes first here, as vanilla's own screens draw theirs.
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics);
        DrawSurface surface = new GuiSurface(graphics, this.font);
        TitledPanel.draw(surface, leftPos, topPos, panelWidth, panelHeight, this.title.getString());
        if (shots.isEmpty()) {
            return;
        }
        Ui.textRight(surface, (page + 1) + "/" + shots.size(), leftPos + panelWidth - UiStyle.PAD,
            UiStyle.centerIn(topPos, UiStyle.TITLE_H, surface.lineHeight()), UiTheme.TEXT_MUTED);
        Ui.well(surface, frame.x(), frame.y(), frame.width(), frame.height());

        PhotoData shot = current();
        ClientPhotoCache.State state = ClientPhotoCache.state(shot.photo());
        var here = ClientPhotoCache.picture(shot.photo());
        if (state == ClientPhotoCache.State.READY && here.isPresent()) {
            drawPicture(graphics, here.get());
        } else {
            String key = state == ClientPhotoCache.State.MISSING
                ? "screen.chiikawa.album.missing" : "screen.chiikawa.album.loading";
            Ui.emptyState(surface, Component.translatable(key).getString(), frame.x() + frame.width() / 2,
                UiStyle.centerIn(frame.y(), frame.height(), surface.lineHeight()));
        }

        int y = frame.bottom() + UiStyle.GAP;
        List<String> caption = TextWrap.wrap(shot.caption().getString(), panelWidth - 2 * UiStyle.PAD, this.font::width);
        for (String line : caption.subList(0, Math.min(caption.size(), CAPTION_LINES))) {
            surface.drawText(line, leftPos + (panelWidth - surface.textWidth(line)) / 2, y, UiTheme.TEXT);
            y += UiStyle.LINE;
        }
    }

    /** The picture, as big as it can be inside the frame without being squeezed out of shape. */
    private void drawPicture(GuiGraphics graphics, ClientPhotoCache.Picture picture) {
        double scale = Math.min((double) frame.width() / picture.width(), (double) frame.height() / picture.height());
        int width = Math.max(1, (int) (picture.width() * scale));
        int height = Math.max(1, (int) (picture.height() * scale));
        graphics.blit(picture.texture(), frame.x() + (frame.width() - width) / 2, frame.y() + (frame.height() - height) / 2,
            width, height, 0.0F, 0.0F, picture.width(), picture.height(), picture.width(), picture.height());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
