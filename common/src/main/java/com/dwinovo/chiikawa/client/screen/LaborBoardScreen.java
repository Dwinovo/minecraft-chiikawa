package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.client.ui.PetStatusText;
import com.dwinovo.chiikawa.client.ui.mc.GuiSurface;
import com.dwinovo.chiikawa.network.BoardPayloads.SlipView;
import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The day's slips on a labor board: what work is up, how much of it, and whose pet took
 * it. Read-only — pets take their own slips, the owner only looks.
 */
public class LaborBoardScreen extends Screen {
    private static final int WIDTH = 220;

    private final List<SlipView> slips;
    private int leftPos;
    private int topPos;
    private int panelHeight;

    public LaborBoardScreen(List<SlipView> slips) {
        super(Component.translatable("screen.chiikawa.labor_board"));
        this.slips = slips;
    }

    @Override
    protected void init() {
        int rows = Math.max(1, slips.size());
        this.panelHeight = UiStyle.TITLE_H + UiStyle.PAD + rows * UiStyle.ROW_PITCH + UiStyle.PAD;
        this.leftPos = (this.width - WIDTH) / 2;
        this.topPos = (this.height - panelHeight) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        DrawSurface surface = new GuiSurface(graphics, this.font);
        int y = Ui.titledPanel(surface, leftPos, topPos, WIDTH, panelHeight, this.title.getString());
        int left = leftPos + UiStyle.PAD;
        int right = leftPos + WIDTH - UiStyle.PAD;

        if (slips.isEmpty()) {
            surface.drawText(Component.translatable("screen.chiikawa.labor_board.empty").getString(),
                left, y, UiTheme.TEXT_MUTED);
            return;
        }
        for (SlipView slip : slips) {
            String detail = Component.translatable("screen.chiikawa.labor_board.detail",
                PetStatusText.taskAmount(slip.type(), slip.target()), PetStatusText.jobName(slip.capability())).getString();
            Ui.textRight(surface, detail, right, y, UiTheme.TEXT_MUTED);
            // The name gives way to the detail beside it rather than running into it.
            Ui.textClipped(surface, PetStatusText.taskName(slip.type()).getString(), left, y,
                right - left - surface.textWidth(detail) - UiStyle.PAD, UiTheme.TEXT);
            Ui.textClipped(surface, status(slip).getString(), left + UiStyle.INDENT, y + UiStyle.LINE,
                right - left - UiStyle.INDENT, slip.taker().isEmpty() ? UiTheme.TEXT_MUTED : UiTheme.SUCCESS);
            y += UiStyle.ROW_PITCH;
        }
    }

    private static Component status(SlipView slip) {
        return slip.taker().isEmpty()
            ? Component.translatable("screen.chiikawa.labor_board.open")
            : Component.translatable("screen.chiikawa.labor_board.taken", slip.taker());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
