package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.client.ui.PetStatusText;
import com.dwinovo.chiikawa.client.ui.PetStyle;
import com.dwinovo.chiikawa.client.ui.PetTheme;
import com.dwinovo.chiikawa.client.ui.Surface;
import com.dwinovo.chiikawa.client.ui.Ui;
import com.dwinovo.chiikawa.client.ui.mc.GuiSurface;
import com.dwinovo.chiikawa.network.BoardPayloads.SlipView;
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
        this.panelHeight = PetStyle.TITLE_H + PetStyle.PAD + rows * PetStyle.ROW_PITCH + PetStyle.PAD;
        this.leftPos = (this.width - WIDTH) / 2;
        this.topPos = (this.height - panelHeight) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Surface surface = new GuiSurface(graphics, this.font);
        int y = Ui.titledPanel(surface, leftPos, topPos, WIDTH, panelHeight, this.title);
        int right = leftPos + WIDTH - PetStyle.PAD;

        if (slips.isEmpty()) {
            surface.drawText(Component.translatable("screen.chiikawa.labor_board.empty"),
                leftPos + PetStyle.PAD, y, PetTheme.TEXT_MUTED);
            return;
        }
        for (SlipView slip : slips) {
            surface.drawText(PetStatusText.taskName(slip.type()), leftPos + PetStyle.PAD, y, PetTheme.TEXT);
            Ui.textRight(surface, Component.translatable("screen.chiikawa.labor_board.detail",
                PetStatusText.taskAmount(slip.type(), slip.target()), PetStatusText.jobName(slip.capability())),
                right, y, PetTheme.TEXT_MUTED);
            surface.drawText(status(slip), leftPos + PetStyle.PAD + PetStyle.INDENT, y + PetStyle.LINE,
                slip.taker().isEmpty() ? PetTheme.TEXT_MUTED : PetTheme.SUCCESS);
            y += PetStyle.ROW_PITCH;
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
