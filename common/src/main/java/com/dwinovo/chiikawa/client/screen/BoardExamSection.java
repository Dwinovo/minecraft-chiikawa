package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.client.ui.mc.ItemIcon;
import com.dwinovo.chiikawa.client.ui.mc.UiButton;
import com.dwinovo.chiikawa.network.BoardPayloads.BoardExamPayload;
import com.dwinovo.chiikawa.network.BoardPayloads.ExamView;
import com.dwinovo.chiikawa.platform.Services;
import com.dwinovo.chiikawa.qualification.PetExams;
import com.dwinovo.chiikawa.shop.Payment;
import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.TextClip;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import com.dwinovo.chiikawa.ui.widget.Price;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The exams part of a labor board's screen, under the slips: for each licence, a button
 * that opens its exam here with the fee on it, and a line saying who would go, who is
 * sitting it, or why it cannot be opened now; then the results the board has posted. Laid
 * out by the screen, which gives it a place and asks how tall it is.
 */
final class BoardExamSection {
    /** One line of words. */
    private static final int LINE_H = 11;
    /** Room for the word and its fee, whichever language it is in. */
    private static final int BUTTON_MIN_W = 64;

    private final BlockPos board;
    private final ExamView view;
    private final List<Placed> placed = new ArrayList<>();
    private int x;
    private int y;
    private int width;

    BoardExamSection(BlockPos board, ExamView view) {
        this.board = board;
        this.view = view;
    }

    /** @return how tall it is; nothing at all when there is nothing to say */
    int height() {
        return view.offers().size() * (UiStyle.CONTROL_H + LINE_H) + view.posted().size() * LINE_H;
    }

    /**
     * Puts it at {@code x, y}, {@code width} wide.
     *
     * @return the buttons, for the screen to add
     */
    List<UiButton> layout(int x, int y, int width, Font font) {
        this.x = x;
        this.y = y;
        this.width = width;
        placed.clear();
        List<UiButton> buttons = new ArrayList<>();
        int rowY = y;
        for (ExamView.Offer offer : view.offers()) {
            Component label = Component.translatable("screen.chiikawa.labor_board.exam_open", offer.feeCount());
            int buttonWidth = Math.max(BUTTON_MIN_W, Price.width(font.width(label)) + 2 * UiStyle.PAD);
            Rect area = new Rect(x + width - buttonWidth, rowY, buttonWidth, UiStyle.CONTROL_H);
            ItemIcon fee = new ItemIcon(new ItemStack(feeItem(offer)));
            UiButton button = new UiButton(area.x(), area.y(), area.width(), area.height(), label,
                (surface, face, argb) -> Price.drawCentered(surface, fee, label.getString(), face, argb),
                () -> Services.NETWORK.sendToServer(new BoardExamPayload(board, offer.qualification())));
            button.active = offer.open() && !offer.going().isEmpty() && carried(offer) >= offer.feeCount();
            buttons.add(button);
            placed.add(new Placed(offer, area));
            rowY += UiStyle.CONTROL_H + LINE_H;
        }
        return buttons;
    }

    void draw(DrawSurface surface) {
        if (height() == 0) {
            return;
        }
        Ui.divider(surface, x, y - UiStyle.GAP_SECTION / 2, width);
        for (Placed row : placed) {
            ExamView.Offer offer = row.offer();
            int nameY = UiStyle.centerIn(row.button().y(), UiStyle.CONTROL_H, surface.lineHeight());
            Ui.textClipped(surface, PetExams.name(offer.qualification()).getString(), x, nameY,
                row.button().x() - UiStyle.GAP - x, UiTheme.TEXT);
            String state = TextClip.clip(state(offer).getString(), width, surface::textWidth);
            surface.drawText(state, x, row.button().bottom(), offer.called().isEmpty() ? UiTheme.TEXT_MUTED : UiTheme.ACCENT);
        }
        int lineY = y + view.offers().size() * (UiStyle.CONTROL_H + LINE_H);
        for (ExamView.Posted sitting : view.posted()) {
            surface.drawText(Component.translatable("screen.chiikawa.labor_board.sat", sitting.name(),
                PetExams.name(sitting.qualification()), sitting.rank()).getString(), x, lineY, UiTheme.TEXT);
            Ui.textRight(surface, Component.translatable(sitting.passed()
                    ? "screen.chiikawa.labor_board.passed" : "screen.chiikawa.labor_board.failed").getString(),
                x + width, lineY, sitting.passed() ? UiTheme.LEAF : UiTheme.TEXT_MUTED);
            lineY += LINE_H;
        }
    }

    /** What the cursor over an exam's button is told: the fee, what the owner has of it, and who goes. */
    Optional<List<String>> tooltip(int mouseX, int mouseY) {
        return placed.stream()
            .filter(row -> row.button().contains(mouseX, mouseY))
            .findFirst()
            .map(row -> List.of(
                Component.translatable("screen.chiikawa.labor_board.exam_fee", new ItemStack(feeItem(row.offer())).getHoverName(),
                    row.offer().feeCount(), carried(row.offer())).getString(),
                Component.translatable("screen.chiikawa.labor_board.exam_who").getString()));
    }

    /** Who is sitting it, who would, or why nobody can now. */
    private static Component state(ExamView.Offer offer) {
        if (!offer.called().isEmpty()) {
            return Component.translatable("screen.chiikawa.labor_board.exam_called", names(offer.called()));
        }
        if (!offer.open()) {
            return Component.translatable("screen.chiikawa.labor_board.exam_closed");
        }
        if (offer.going().isEmpty()) {
            return Component.translatable("screen.chiikawa.labor_board.exam_nobody");
        }
        return Component.translatable("screen.chiikawa.labor_board.exam_going", names(offer.going()));
    }

    private static String names(List<String> pets) {
        return String.join(Component.translatable("message.chiikawa.list_separator").getString(), pets);
    }

    private static Item feeItem(ExamView.Offer offer) {
        return BuiltInRegistries.ITEM.get(offer.feeItem());
    }

    /** How much of the fee the owner is carrying, which is what the fee is measured against. */
    private static int carried(ExamView.Offer offer) {
        Minecraft minecraft = Minecraft.getInstance();
        Item item = feeItem(offer);
        return minecraft.player == null ? 0 : Payment.count(minecraft.player.getInventory(), stack -> stack.is(item));
    }

    /** An exam's row: where its button went. */
    private record Placed(ExamView.Offer offer, Rect button) {
    }
}
