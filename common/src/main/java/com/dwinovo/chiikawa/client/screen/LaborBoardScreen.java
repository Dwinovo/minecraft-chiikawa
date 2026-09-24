package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.client.ui.PetStatusText;
import com.dwinovo.chiikawa.client.ui.mc.GuiSurface;
import com.dwinovo.chiikawa.client.ui.mc.ItemIcon;
import com.dwinovo.chiikawa.client.ui.mc.UiButton;
import com.dwinovo.chiikawa.network.BoardPayloads;
import com.dwinovo.chiikawa.network.BoardPayloads.BoardSlipsPayload;
import com.dwinovo.chiikawa.network.BoardPayloads.BoardUpgradePayload;
import com.dwinovo.chiikawa.network.BoardPayloads.SlipView;
import com.dwinovo.chiikawa.platform.Services;
import com.dwinovo.chiikawa.shop.Wallet;
import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.TextClip;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import com.dwinovo.chiikawa.ui.widget.Badge;
import com.dwinovo.chiikawa.ui.widget.Price;
import com.dwinovo.chiikawa.ui.widget.Slot;
import com.dwinovo.chiikawa.ui.widget.TitledPanel;
import com.dwinovo.chiikawa.ui.widget.Tooltip;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * The day's slips on a labor board. The slips themselves are read-only — pets take their
 * own, the owner only looks — so the list is built to be looked at: a row is a picture, a
 * name and a state, and everything else waits under the cursor. The one thing the owner
 * can do here, buy the board a level, sits under a rule at the bottom, away from the
 * slips, with its price on the button.
 */
public class LaborBoardScreen extends Screen {
    private static final int WIDTH = 236;
    /** Room for the word and its price, whichever language it is in. */
    private static final int UPGRADE_MIN_W = 64;

    private final BlockPos board;
    private final int level;
    private final int daily;
    private final BoardPayloads.NextLevel next;
    private final int price;
    private final List<SlipView> slips;
    /** What the upgrade is paid in: pictured on the button, named in the tooltip. */
    private final ItemStack coin = Wallet.coins(1);
    private int leftPos;
    private int topPos;
    private int panelHeight;
    private int contentY;
    private int footerY;

    public LaborBoardScreen(BoardSlipsPayload payload) {
        super(Component.translatable("screen.chiikawa.labor_board"));
        this.board = payload.board();
        this.level = payload.level();
        this.daily = payload.daily();
        this.next = payload.next();
        this.price = next.price();
        this.slips = payload.slips();
    }

    @Override
    protected void init() {
        int rows = Math.max(1, slips.size());
        this.panelHeight = UiStyle.TITLE_H + UiStyle.PAD
            + rows * UiStyle.ROW_H + (rows - 1) * UiStyle.GAP
            + UiStyle.GAP_SECTION + UiStyle.CONTROL_H + UiStyle.PAD;
        this.leftPos = (this.width - WIDTH) / 2;
        this.topPos = (this.height - panelHeight) / 2;
        this.contentY = TitledPanel.contentY(topPos);
        this.footerY = topPos + panelHeight - UiStyle.PAD - UiStyle.CONTROL_H;
        clearWidgets();
        if (price > 0) {
            addRenderableWidget(upgradeButton());
        }
    }

    /** What the next level costs, on the button that buys it. */
    private UiButton upgradeButton() {
        Component label = Component.translatable("screen.chiikawa.labor_board.upgrade", price);
        int width = Math.max(UPGRADE_MIN_W, Price.width(this.font.width(label)) + 2 * UiStyle.PAD);
        UiButton button = new UiButton(leftPos + WIDTH - UiStyle.PAD - width, footerY, width, UiStyle.CONTROL_H,
            label, (surface, area, argb) -> Price.drawCentered(surface, new ItemIcon(coin), label.getString(), area, argb),
            () -> Services.NETWORK.sendToServer(new BoardUpgradePayload(board)));
        button.active = purse() >= price;
        return button;
    }

    /**
     * The panel, its slips and its footer, drawn right after the game dims what is behind
     * the screen and before the upgrade button, as the music box draws its own: drawn after
     * it, the panel would cover it.
     */
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        GuiSurface surface = new GuiSurface(graphics, this.font);
        TitledPanel.draw(surface, leftPos, topPos, WIDTH, panelHeight, this.title.getString());
        int right = leftPos + WIDTH - UiStyle.PAD;

        if (slips.isEmpty()) {
            Ui.emptyState(surface, Component.translatable("screen.chiikawa.labor_board.empty").getString(),
                leftPos + WIDTH / 2, UiStyle.centerIn(contentY, UiStyle.ROW_H, surface.lineHeight()));
        } else {
            // How many are up belongs beside the title, not in a row of its own.
            Ui.textRight(surface, Component.translatable("screen.chiikawa.labor_board.count", slips.size()).getString(),
                right, UiStyle.centerIn(topPos, UiStyle.TITLE_H, surface.lineHeight()), UiTheme.TEXT_MUTED);
            for (int i = 0; i < slips.size(); i++) {
                Rect row = rowAt(i);
                drawRow(surface, slips.get(i), row, row.contains(mouseX, mouseY));
            }
        }
        // A board with nothing on it today can still be paid up.
        drawFooter(surface);
    }

    /** The buttons over the panel, then whatever the cursor is asking about over them. */
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        GuiSurface surface = new GuiSurface(graphics, this.font);
        hovered(mouseX, mouseY).ifPresent(slip -> surface.onTop(() ->
            Tooltip.draw(surface, detail(slip), mouseX, mouseY, this.width, this.height)));
        if (footer().contains(mouseX, mouseY)) {
            surface.onTop(() -> Tooltip.draw(surface, upgradeDetail(), mouseX, mouseY, this.width, this.height));
        }
    }

    /**
     * How far the board has been paid up, and what that buys — a level is a number until
     * it is said in slips a day, so it is said in slips a day.
     */
    private void drawFooter(DrawSurface surface) {
        Ui.divider(surface, leftPos + UiStyle.PAD, footerY - UiStyle.GAP_SECTION / 2, WIDTH - 2 * UiStyle.PAD);
        String badge = Component.translatable("screen.chiikawa.labor_board.level", level).getString();
        Badge.draw(surface, badge, leftPos + UiStyle.PAD,
            UiStyle.centerIn(footerY, UiStyle.CONTROL_H, Badge.height(surface)), UiTheme.ACCENT);
        String perDay = Component.translatable("screen.chiikawa.labor_board.daily", daily).getString();
        surface.drawText(perDay, leftPos + UiStyle.PAD + Badge.width(surface, badge) + UiStyle.GAP,
            UiStyle.centerIn(footerY, UiStyle.CONTROL_H, surface.lineHeight()), UiTheme.TEXT_MUTED);
        if (price <= 0) {
            Ui.textRight(surface, Component.translatable("screen.chiikawa.labor_board.max_level").getString(),
                leftPos + WIDTH - UiStyle.PAD,
                UiStyle.centerIn(footerY, UiStyle.CONTROL_H, surface.lineHeight()), UiTheme.TEXT_MUTED);
        }
    }

    /** What a level is worth, and whether the owner can afford the next one. */
    private List<String> upgradeDetail() {
        List<String> lines = new ArrayList<>();
        lines.add(Component.translatable("screen.chiikawa.labor_board.level", level).getString());
        lines.add(Component.translatable("screen.chiikawa.labor_board.daily", daily).getString());
        if (price > 0) {
            lines.add(Component.translatable("screen.chiikawa.labor_board.upgrade_hint", level + 1, next.daily())
                .getString());
            for (Identifier type : next.unlocks()) {
                lines.add(Component.translatable("screen.chiikawa.labor_board.unlocks", PetStatusText.taskName(type))
                    .getString());
            }
            lines.add(Component.translatable("screen.chiikawa.labor_board.purse", purse(), coin.getHoverName()).getString());
        } else {
            lines.add(Component.translatable("screen.chiikawa.labor_board.max_level").getString());
        }
        return lines;
    }

    private Rect footer() {
        return new Rect(leftPos + UiStyle.PAD, footerY, WIDTH - 2 * UiStyle.PAD, UiStyle.CONTROL_H);
    }

    /** How much money the owner is carrying, which is what a price is measured against. */
    private static int purse() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? 0 : Wallet.count(minecraft.player.getInventory());
    }

    private Rect rowAt(int index) {
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

    private Optional<SlipView> hovered(int mouseX, int mouseY) {
        for (int i = 0; i < slips.size(); i++) {
            if (rowAt(i).contains(mouseX, mouseY)) {
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
