package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.client.ui.mc.GuiSurface;
import com.dwinovo.chiikawa.client.ui.mc.ItemIcon;
import com.dwinovo.chiikawa.client.ui.mc.UiButton;
import com.dwinovo.chiikawa.network.ExamDeskPayloads.BookingView;
import com.dwinovo.chiikawa.network.ExamDeskPayloads.CandidateView;
import com.dwinovo.chiikawa.network.ExamDeskPayloads.DeskViewPayload;
import com.dwinovo.chiikawa.network.ExamDeskPayloads.OfferView;
import com.dwinovo.chiikawa.network.ExamDeskPayloads.SignUpPayload;
import com.dwinovo.chiikawa.platform.Services;
import com.dwinovo.chiikawa.qualification.Ineligible;
import com.dwinovo.chiikawa.qualification.PassOdds;
import com.dwinovo.chiikawa.qualification.PetExams;
import com.dwinovo.chiikawa.shop.Payment;
import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import com.dwinovo.chiikawa.ui.widget.Price;
import com.dwinovo.chiikawa.ui.widget.TitledPanel;
import com.dwinovo.chiikawa.ui.widget.Tooltip;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Signing a pet up at an exam desk, as a parent signs a child up: the owner's pets nearby,
 * each with the grade it would sit and its odds, or why it cannot go, and a button to sign
 * it up with the fee on it. Under the cursor, what the odds come from, so the owner can
 * tell whether to send it now or let it work and read first. Above the pets, whoever the
 * desk has already, or why it takes nobody now.
 */
public class ExamDeskScreen extends Screen {
    private static final int WIDTH = 236;
    private static final int LINE_H = 11;
    /** Room for the word and the fee, whichever language it is in. */
    private static final int BUTTON_MIN_W = 48;
    /** As many pets as fit; an owner with more nearby sees the likeliest first. */
    private static final int MAX_ROWS = 8;

    private final DeskViewPayload view;
    private int licence;
    private int leftPos;
    private int topPos;
    private int panelHeight;
    private int headerY;
    private int rowsY;
    private final List<Row> rows = new ArrayList<>();

    public ExamDeskScreen(DeskViewPayload view) {
        super(Component.translatable("screen.chiikawa.exam_desk"));
        this.view = view;
    }

    @Override
    protected void init() {
        Optional<OfferView> offer = offer();
        int shown = offer.map(o -> Math.min(MAX_ROWS, o.candidates().size())).orElse(0);
        int tabs = view.offers().size() > 1 ? UiStyle.CONTROL_H + UiStyle.GAP : 0;
        int list = Math.max(1, shown) * UiStyle.ROW_PITCH;
        this.panelHeight = UiStyle.TITLE_H + UiStyle.PAD + LINE_H + UiStyle.GAP + tabs + UiStyle.ROW_H + UiStyle.GAP + list
            + UiStyle.PAD;
        this.leftPos = (this.width - WIDTH) / 2;
        this.topPos = (this.height - panelHeight) / 2;
        int contentY = TitledPanel.contentY(topPos);
        int tabsY = contentY + LINE_H + UiStyle.GAP;
        this.headerY = tabsY + tabs;
        this.rowsY = headerY + UiStyle.ROW_H + UiStyle.GAP;
        clearWidgets();
        rows.clear();
        if (tabs > 0) {
            addTabs(tabsY);
        }
        offer.ifPresent(o -> addRows(o, shown));
    }

    /** A tab for each licence the desk examines, when there is more than one. */
    private void addTabs(int y) {
        int x = leftPos + UiStyle.PAD;
        for (int i = 0; i < view.offers().size(); i++) {
            int index = i;
            Component name = PetExams.name(view.offers().get(i).qualification());
            int width = this.font.width(name) + 2 * UiStyle.PAD;
            UiButton tab = UiButton.text(x, y, width, UiStyle.CONTROL_H, name, () -> {
                licence = index;
                rebuildWidgets();
            });
            tab.active = index != licence;
            addRenderableWidget(tab);
            x += width + UiStyle.GAP;
        }
    }

    /** The pets, the ones that can go first and the likeliest of those first, each with its button. */
    private void addRows(OfferView offer, int shown) {
        Item fee = BuiltInRegistries.ITEM.get(offer.feeItem());
        ItemIcon feeIcon = new ItemIcon(new ItemStack(fee));
        int carried = carried(fee);
        List<CandidateView> pets = offer.candidates().stream()
            .sorted(Comparator.comparing((CandidateView c) -> c.whyNot().isPresent())
                .thenComparing(c -> -c.odds().chance()))
            .limit(shown)
            .toList();
        for (int i = 0; i < pets.size(); i++) {
            CandidateView pet = pets.get(i);
            Rect row = new Rect(leftPos + UiStyle.PAD, rowsY + i * UiStyle.ROW_PITCH, WIDTH - 2 * UiStyle.PAD, UiStyle.ROW_H);
            Optional<Rect> button = Optional.empty();
            if (pet.whyNot().isEmpty()) {
                Component label = Component.translatable("screen.chiikawa.exam_desk.sign_up", offer.feeCount());
                int width = Math.max(BUTTON_MIN_W, Price.width(this.font.width(label)) + 2 * UiStyle.PAD);
                Rect area = new Rect(row.right() - width, UiStyle.centerIn(row.y(), row.height(), UiStyle.CONTROL_H),
                    width, UiStyle.CONTROL_H);
                UiButton signUp = new UiButton(area.x(), area.y(), area.width(), area.height(), label,
                    (surface, face, argb) -> Price.drawCentered(surface, feeIcon, label.getString(), face, argb),
                    () -> Services.NETWORK.sendToServer(new SignUpPayload(view.desk(), offer.qualification(), pet.pet())));
                signUp.active = view.closed().isEmpty() && carried >= offer.feeCount();
                addRenderableWidget(signUp);
                button = Optional.of(area);
            }
            rows.add(new Row(pet, row, button));
        }
    }

    /**
     * The panel, the desk's news, the licence and its fee, and the pets, drawn right after the
     * game dims what is behind the screen and before the buttons, as the labor board draws its
     * own: drawn after them, the panel would cover them.
     */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        GuiSurface surface = new GuiSurface(graphics, this.font);
        TitledPanel.draw(surface, leftPos, topPos, WIDTH, panelHeight, this.title.getString());
        int x = leftPos + UiStyle.PAD;
        int right = leftPos + WIDTH - UiStyle.PAD;
        Ui.textClipped(surface, news().getString(), x, TitledPanel.contentY(topPos), WIDTH - 2 * UiStyle.PAD,
            view.booking().isPresent() ? UiTheme.ACCENT : UiTheme.TEXT_MUTED);
        Optional<OfferView> offer = offer();
        if (offer.isEmpty()) {
            return;
        }
        int nameY = UiStyle.centerIn(headerY, UiStyle.ROW_H, surface.lineHeight());
        surface.drawText(PetExams.name(offer.get().qualification()).getString(), x, nameY, UiTheme.TEXT);
        Price.drawRight(surface, new ItemIcon(new ItemStack(BuiltInRegistries.ITEM.get(offer.get().feeItem()))),
            Component.translatable("screen.chiikawa.exam_desk.fee", offer.get().feeCount()).getString(),
            right, headerY, UiStyle.ROW_H, UiTheme.TEXT_MUTED);
        Ui.divider(surface, x, rowsY - UiStyle.GAP / 2 - 1, WIDTH - 2 * UiStyle.PAD);
        if (rows.isEmpty()) {
            Ui.emptyState(surface, Component.translatable("screen.chiikawa.exam_desk.nobody").getString(),
                leftPos + WIDTH / 2, UiStyle.centerIn(rowsY, UiStyle.ROW_H, surface.lineHeight()));
        }
        for (Row row : rows) {
            drawRow(surface, row, row.area().contains(mouseX, mouseY));
        }
    }

    /** The buttons over the panel, then what the odds of the pet under the cursor come from. */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        GuiSurface surface = new GuiSurface(graphics, this.font);
        rows.stream().filter(row -> row.area().contains(mouseX, mouseY)).findFirst().ifPresent(row ->
            surface.onTop(() -> Tooltip.draw(surface, oddsLines(row.pet()), mouseX, mouseY, this.width, this.height)));
    }

    /**
     * A pet's row: its name, the grade it would sit, its odds, and its button - or, for one
     * that cannot go, why not where the button would be.
     */
    private void drawRow(DrawSurface surface, Row row, boolean hovered) {
        if (hovered) {
            Ui.rowHighlight(surface, row.area());
        }
        CandidateView pet = row.pet();
        int textY = UiStyle.centerIn(row.area().y(), row.area().height(), surface.lineHeight());
        int x = row.area().x() + UiStyle.TIGHT;
        int end = row.button().map(Rect::x).orElse(row.area().right()) - UiStyle.GAP;
        if (pet.whyNot().isPresent()) {
            String why = Component.translatable(reasonKey(pet.whyNot().get())).getString();
            Ui.textRight(surface, why, end, textY, UiTheme.TEXT_MUTED);
            end -= surface.textWidth(why) + UiStyle.GAP;
        }
        if (pet.whyNot().filter(Ineligible.TOP_GRADE::equals).isEmpty()) {
            String chance = percent(pet.odds().chance());
            Ui.textRight(surface, chance, end, textY, pet.whyNot().isEmpty() ? UiTheme.SUCCESS : UiTheme.TEXT_MUTED);
            end -= surface.textWidth(chance) + UiStyle.GAP;
        }
        String grade = Component.translatable("screen.chiikawa.exam_desk.sits", pet.nextRank()).getString();
        Ui.textRight(surface, grade, end, textY, UiTheme.TEXT_MUTED);
        end -= surface.textWidth(grade) + UiStyle.GAP;
        Ui.textClipped(surface, pet.name(), x, textY, end - x, pet.whyNot().isEmpty() ? UiTheme.TEXT : UiTheme.TEXT_MUTED);
    }

    /** Whoever the desk has, or why it takes nobody now, or what to do. */
    private Component news() {
        if (view.booking().isPresent()) {
            BookingView booking = view.booking().get();
            String key = "screen.chiikawa.exam_desk.booking." + booking.stage().name().toLowerCase(Locale.ROOT);
            return Component.translatable(key, booking.name(), PetExams.name(booking.qualification()), booking.rank());
        }
        return view.closed()
            .map(refusal -> Component.translatable("screen.chiikawa.exam_desk.closed." + refusal.name().toLowerCase(Locale.ROOT)))
            .orElseGet(() -> Component.translatable("screen.chiikawa.exam_desk.pick"));
    }

    /** What a pet's odds come from, piece by piece. */
    private static List<String> oddsLines(CandidateView pet) {
        PassOdds odds = pet.odds();
        List<String> lines = new ArrayList<>();
        lines.add(pet.name() + (pet.heldRank() == 0
            ? Component.translatable("screen.chiikawa.exam_desk.holds_none").getString()
            : Component.translatable("screen.chiikawa.exam_desk.holds", pet.heldRank()).getString()));
        lines.add(Component.translatable("screen.chiikawa.exam_desk.odds.base", percentNumber(odds.base())).getString());
        if (odds.practice() > 0.0F) {
            lines.add(Component.translatable("screen.chiikawa.exam_desk.odds.practice", percentNumber(odds.practice())).getString());
        }
        if (odds.failing() > 0.0F) {
            lines.add(Component.translatable("screen.chiikawa.exam_desk.odds.failing", percentNumber(odds.failing())).getString());
        }
        if (odds.book() > 0.0F) {
            lines.add(Component.translatable("screen.chiikawa.exam_desk.odds.book", percentNumber(odds.book())).getString());
        }
        if (odds.aptitude() != 1.0F) {
            lines.add(Component.translatable("screen.chiikawa.exam_desk.odds.aptitude",
                String.format(Locale.ROOT, "%.1f", odds.aptitude())).getString());
        }
        lines.add(Component.translatable("screen.chiikawa.exam_desk.odds.total", percent(odds.chance()),
            percentNumber(odds.max())).getString());
        return lines;
    }

    private static String reasonKey(Ineligible reason) {
        return "screen.chiikawa.exam_desk.ineligible." + reason.name().toLowerCase(Locale.ROOT);
    }

    private static String percent(float chance) {
        return percentNumber(chance) + "%";
    }

    private static String percentNumber(float chance) {
        return Integer.toString(Math.round(chance * 100.0F));
    }

    private Optional<OfferView> offer() {
        return view.offers().isEmpty() ? Optional.empty()
            : Optional.of(view.offers().get(Math.min(licence, view.offers().size() - 1)));
    }

    /** How much of the fee the owner is carrying, which is what the fee is measured against. */
    private static int carried(Item fee) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? 0 : Payment.count(minecraft.player.getInventory(), stack -> stack.is(fee));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** A pet's row: where it is, and where its button went, if it has one. */
    private record Row(CandidateView pet, Rect area, Optional<Rect> button) {
    }
}
