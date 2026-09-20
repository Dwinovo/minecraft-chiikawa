package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.client.ui.PetStatusText;
import com.dwinovo.chiikawa.client.ui.mc.GuiSurface;
import com.dwinovo.chiikawa.client.ui.mc.ItemIcon;
import com.dwinovo.chiikawa.network.BoardPayloads.SlipView;
import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.TextClip;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import com.dwinovo.chiikawa.ui.widget.Badge;
import com.dwinovo.chiikawa.ui.widget.Slot;
import com.dwinovo.chiikawa.ui.widget.Tooltip;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The day's slips on a labor board. Read-only — pets take their own slips, the owner only
 * looks — so it is built to be looked at: a row is a picture, a name and a state, and
 * everything else waits under the cursor.
 */
public class LaborBoardScreen extends Screen {
    private static final int WIDTH = 236;

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
        this.panelHeight = UiStyle.TITLE_H + UiStyle.PAD
            + rows * UiStyle.ROW_H + (rows - 1) * UiStyle.GAP + UiStyle.PAD;
        this.leftPos = (this.width - WIDTH) / 2;
        this.topPos = (this.height - panelHeight) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        GuiSurface surface = new GuiSurface(graphics, this.font);
        int contentY = Ui.titledPanel(surface, leftPos, topPos, WIDTH, panelHeight, this.title.getString());
        int right = leftPos + WIDTH - UiStyle.PAD;

        if (slips.isEmpty()) {
            Ui.emptyState(surface, Component.translatable("screen.chiikawa.labor_board.empty").getString(),
                leftPos + WIDTH / 2, UiStyle.centerIn(contentY, UiStyle.ROW_H, surface.lineHeight()));
            return;
        }
        // How many are up belongs beside the title, not in a row of its own.
        Ui.textRight(surface, Component.translatable("screen.chiikawa.labor_board.count", slips.size()).getString(),
            right, UiStyle.centerIn(topPos, UiStyle.TITLE_H, surface.lineHeight()), UiTheme.TEXT_MUTED);

        for (int i = 0; i < slips.size(); i++) {
            Rect row = rowAt(contentY, i);
            drawRow(surface, slips.get(i), row, row.contains(mouseX, mouseY));
        }
        hovered(contentY, mouseX, mouseY).ifPresent(slip -> surface.onTop(() ->
            Tooltip.draw(surface, detail(slip), mouseX, mouseY, this.width, this.height)));
    }

    private Rect rowAt(int contentY, int index) {
        return new Rect(leftPos + UiStyle.PAD, contentY + index * (UiStyle.ROW_H + UiStyle.GAP),
            WIDTH - 2 * UiStyle.PAD, UiStyle.ROW_H);
    }

    /**
     * What the owner is at the board to find out: what the work is (its icon), what it is
     * called, whose job it is, how much of it, and whether anyone has it yet. Which job may
     * take a slip decides whether it is any of this owner's business at all, so it is read
     * off the row rather than hunted for — quiet, beside the name, since the name is what
     * tells two rows apart. Only the taker's name waits in {@link #detail}.
     */
    private void drawRow(DrawSurface surface, SlipView slip, Rect row, boolean hovered) {
        if (hovered) {
            Ui.rowHighlight(surface, row);
        }
        Slot.draw(surface, ItemIcon.of(slip.icon()), row.x(), UiStyle.centerIn(row.y(), row.height(), UiStyle.SLOT));

        boolean taken = !slip.taker().isEmpty();
        String state = Component.translatable(taken
            ? "screen.chiikawa.labor_board.taken"
            : "screen.chiikawa.labor_board.open").getString();
        int stateWidth = Badge.width(surface, state);
        Badge.draw(surface, state, row.right() - stateWidth,
            UiStyle.centerIn(row.y(), row.height(), Badge.height(surface)),
            taken ? UiTheme.SUCCESS : UiTheme.TEXT_MUTED);

        String amount = PetStatusText.taskAmount(slip.type(), slip.target()).getString();
        int amountRight = row.right() - stateWidth - UiStyle.GAP;
        Ui.textRight(surface, amount, amountRight,
            UiStyle.centerIn(row.y(), row.height(), surface.lineHeight()), UiTheme.TEXT_MUTED);


        String forJob = Component.translatable("screen.chiikawa.labor_board.for_job",
            PetStatusText.jobName(slip.capability())).getString();
        int nameX = row.x() + UiStyle.SLOT + UiStyle.GAP;
        int textY = UiStyle.centerIn(row.y(), row.height(), surface.lineHeight());
        int nameRoom = amountRight - surface.textWidth(amount) - UiStyle.GAP
            - surface.textWidth(forJob) - UiStyle.GAP - nameX;
        // The name gives way first: whose job it is stays whole, or the row stops answering
        // the question it is there to answer.
        String name = TextClip.clip(PetStatusText.taskName(slip.type()).getString(), nameRoom, surface::textWidth);
        surface.drawText(name, nameX, textY, UiTheme.TEXT);
        surface.drawText(forJob, nameX + surface.textWidth(name) + UiStyle.GAP, textY, UiTheme.TEXT_MUTED);
    }

    /** The slip in full, including the one thing a row has no room for: who took it. */
    private static List<String> detail(SlipView slip) {
        List<String> lines = new ArrayList<>();
        lines.add(PetStatusText.taskName(slip.type()).getString());
        lines.add(Component.translatable("screen.chiikawa.labor_board.for_job",
            PetStatusText.jobName(slip.capability())).getString());
        lines.add(PetStatusText.taskAmount(slip.type(), slip.target()).getString());
        lines.add(slip.taker().isEmpty()
            ? Component.translatable("screen.chiikawa.labor_board.open").getString()
            : Component.translatable("screen.chiikawa.labor_board.taken_by", slip.taker()).getString());
        return lines;
    }

    private Optional<SlipView> hovered(int contentY, int mouseX, int mouseY) {
        for (int i = 0; i < slips.size(); i++) {
            if (rowAt(contentY, i).contains(mouseX, mouseY)) {
                return Optional.of(slips.get(i));
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
