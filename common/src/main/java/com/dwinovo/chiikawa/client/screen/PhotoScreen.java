package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.client.photo.ClientPhotoCache;
import com.dwinovo.chiikawa.client.ui.mc.GuiSurface;
import com.dwinovo.chiikawa.photo.PhotoData;
import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.TextWrap;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import com.dwinovo.chiikawa.ui.widget.TitledPanel;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * A photo, looked at: the picture in the middle of a paper frame like the handbook's, and
 * what the photo item says about it written underneath. The picture is fetched from the
 * server the first time it is wanted, and until it is here the frame says it is being
 * developed.
 */
public class PhotoScreen extends Screen {
    /** What size the picture is taken to be until it is here to say: the camera's own. */
    private static final int DEFAULT_WIDTH = 320;
    private static final int DEFAULT_HEIGHT = 240;
    /** The most a picture is blown up by; past that it is only a blur. */
    private static final double MAX_SCALE = 2.0;
    /** The lines the caption is given room for; a longer one makes the frame taller. */
    private static final int CAPTION_LINES = 2;
    private static final int MIN_PANEL_WIDTH = 2 * UiStyle.PAD + 96;
    /** Kept clear between the frame and the window's edges. */
    private static final int MARGIN = 4;

    private final PhotoData photo;
    private int leftPos, topPos, panelWidth, panelHeight;
    private Rect picture = new Rect(0, 0, 0, 0);
    private List<String> caption = List.of();
    /** Whether the frame was laid out for the picture itself rather than for the one it waits for. */
    private boolean fitted;

    public PhotoScreen(PhotoData photo) {
        super(Component.translatable("item.chiikawa.photo"));
        this.photo = photo;
    }

    @Override
    protected void init() {
        ClientPhotoCache.state(photo.photo());
        layOut();
    }

    /** Fits the frame to the window: the picture as big as the room allows, the caption under it. */
    private void layOut() {
        var here = ClientPhotoCache.picture(photo.photo());
        this.fitted = here.isPresent();
        int sourceWidth = here.map(ClientPhotoCache.Picture::width).orElse(DEFAULT_WIDTH);
        int sourceHeight = here.map(ClientPhotoCache.Picture::height).orElse(DEFAULT_HEIGHT);
        int roomWidth = this.width - 2 * MARGIN - 2 * UiStyle.PAD;
        // What is left of the height once the title, the caption and the paper round them are paid for.
        int roomHeight = this.height - 2 * MARGIN - TitledPanel.SIGN_RISE - UiStyle.TITLE_H - 2 * UiStyle.PAD
            - UiStyle.GAP - CAPTION_LINES * UiStyle.LINE;
        double scale = Math.min(MAX_SCALE, Math.min((double) roomWidth / sourceWidth, (double) roomHeight / sourceHeight));
        int pictureWidth = Math.max(1, (int) (sourceWidth * scale));
        int pictureHeight = Math.max(1, (int) (sourceHeight * scale));

        this.panelWidth = Math.max(pictureWidth + 2 * UiStyle.PAD, MIN_PANEL_WIDTH);
        this.caption = TextWrap.wrap(photo.caption().getString(), panelWidth - 2 * UiStyle.PAD, this.font::width);
        this.panelHeight = UiStyle.TITLE_H + UiStyle.PAD + pictureHeight + UiStyle.GAP
            + Math.max(1, caption.size()) * UiStyle.LINE + UiStyle.PAD;
        this.leftPos = (this.width - panelWidth) / 2;
        this.topPos = Math.max(TitledPanel.SIGN_RISE + MARGIN, (this.height - panelHeight) / 2);
        this.picture = new Rect(leftPos + (panelWidth - pictureWidth) / 2, TitledPanel.contentY(topPos),
            pictureWidth, pictureHeight);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        // The picture may have arrived since the frame was laid out, and be another size.
        if (!fitted && ClientPhotoCache.picture(photo.photo()).isPresent()) {
            layOut();
        }
        DrawSurface surface = new GuiSurface(graphics, this.font);
        TitledPanel.draw(surface, leftPos, topPos, panelWidth, panelHeight, this.title.getString());
        Ui.well(surface, picture.x(), picture.y(), picture.width(), picture.height());

        ClientPhotoCache.State state = ClientPhotoCache.state(photo.photo());
        var here = ClientPhotoCache.picture(photo.photo());
        if (state == ClientPhotoCache.State.READY && here.isPresent()) {
            graphics.blit(here.get().texture(), picture.x(), picture.y(), picture.width(), picture.height(),
                0.0F, 0.0F, here.get().width(), here.get().height(), here.get().width(), here.get().height());
        } else {
            String key = state == ClientPhotoCache.State.MISSING
                ? "screen.chiikawa.photo.missing" : "screen.chiikawa.photo.loading";
            Ui.emptyState(surface, Component.translatable(key).getString(), picture.x() + picture.width() / 2,
                UiStyle.centerIn(picture.y(), picture.height(), surface.lineHeight()));
        }

        int y = picture.bottom() + UiStyle.GAP;
        for (String line : caption) {
            surface.drawText(line, leftPos + (panelWidth - surface.textWidth(line)) / 2, y, UiTheme.TEXT);
            y += UiStyle.LINE;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
