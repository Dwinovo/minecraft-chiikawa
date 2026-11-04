package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.client.anim.render.ChiikawaEntityRenderer;
import com.dwinovo.chiikawa.client.ui.PetStatusText;
import com.dwinovo.chiikawa.client.ui.mc.GuiSurface;
import com.dwinovo.chiikawa.client.ui.mc.ItemIcon;
import com.dwinovo.chiikawa.client.ui.mc.UiButton;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.menu.PetBackpackMenu;
import com.dwinovo.chiikawa.network.PetPayloads.PetDirectivePayload;
import com.dwinovo.chiikawa.network.PetPayloads.PetOutlookPayload;
import com.dwinovo.chiikawa.platform.Services;
import com.dwinovo.chiikawa.qualification.LicenceView;
import com.dwinovo.chiikawa.shop.Wallet;
import com.dwinovo.chiikawa.task.PetTask;
import com.dwinovo.chiikawa.ui.Icon;
import com.dwinovo.chiikawa.ui.PixelArt;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.TextClip;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import com.dwinovo.chiikawa.ui.widget.Bar;
import com.dwinovo.chiikawa.ui.widget.Bubble;
import com.dwinovo.chiikawa.ui.widget.Chip;
import com.dwinovo.chiikawa.ui.widget.Hearts;
import com.dwinovo.chiikawa.ui.widget.Segments;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * The banner across the top of the pet screen: the pet itself, standing on it and rising over its edge, its name and what it does for a living, how much it has in its pockets and how keen it is, its health, the order
 * it has been given, and one thing it says. The words and the layout they are in are {@link HeaderRow} and
 * {@link SpeechRow}; this draws them, notes what waits under the cursor for each part, and makes the places the orders are
 * pressed.
 */
final class PetHeader {
    /** Banner pictures are smaller than whole items: see {@link Chip#FACE}. */
    private static final int FACE = Chip.FACE;

    private final HoverNotes notes;

    PetHeader(HoverNotes notes) {
        this.notes = notes;
    }

    /** The places the three orders are pressed, over the control that is drawn there. */
    List<UiButton> buttons(int left, int top, ToIntFunction<String> textWidth, Supplier<AbstractPet> pet) {
        List<UiButton> buttons = new ArrayList<>();
        PetDirective[] orders = PetDirective.values();
        List<Rect> cells = Segments.cells(textWidth, orderNames(), PetScreenLayout.ORDERS.offset(left, top));
        for (int index = 0; index < orders.length; index++) {
            PetDirective directive = orders[index];
            buttons.add(UiButton.over(cells.get(index), Component.translatable("screen.chiikawa.pet.order." + key(directive)), () -> {
                AbstractPet told = pet.get();
                if (told != null && told.getPetDirective() != directive) {
                    Services.NETWORK.sendToServer(new PetDirectivePayload(told.getId(), directive));
                }
            }));
        }
        return buttons;
    }

    /** Draws the banner's contents over the banner, for a panel whose top-left corner is at {@code left, top}. */
    void draw(GuiGraphicsExtractor graphics, GuiSurface surface, int left, int top, AbstractPet pet, PetBackpackMenu menu, int mouseX,
            int mouseY) {
        drawPet(graphics, surface, left, top, pet, mouseX, mouseY);
        drawRow(surface, left, top, pet, menu);
        int maxHealth = Math.max(1, Mth.ceil(pet.getMaxHealth()));
        Hearts.draw(surface, left + PetScreenLayout.TEXT_X, top + PetScreenLayout.HEARTS_Y,
            Math.max(0, Mth.ceil(pet.getHealth())), maxHealth);
        drawOrders(surface, left, top, pet, mouseX, mouseY);
        drawSpeech(surface, left, top, pet, menu.outlook());
    }

    // ---- the pet -----------------------------------------------------------------

    private static void drawPet(GuiGraphicsExtractor graphics, GuiSurface surface, int left, int top, AbstractPet pet, int mouseX,
            int mouseY) {
        Rect shadow = PetScreenLayout.SHADOW.offset(left, top);
        Ui.roundRect(surface, shadow.x(), shadow.y(), shadow.width(), shadow.height(), shadow.height() / 2, UiTheme.SHADOW);
        Rect box = PetScreenLayout.MODEL.offset(left, top);
        // The model stands on the floor wherever its box is: its feet are half the pet's height under its middle, and the
        // rest of the offset is what the box's middle is above the floor.
        float yOffset = (PetScreenLayout.FLOOR_Y - (PetScreenLayout.MODEL.y() + PetScreenLayout.MODEL.height() / 2.0F))
            / PetScreenLayout.MODEL_SCALE - pet.getBbHeight() / 2.0F;
        ChiikawaEntityRenderer.drawPortrait(() -> InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, box.x(), box.y(),
            box.right(), box.bottom(), PetScreenLayout.MODEL_SCALE, yOffset, mouseX, mouseY, pet));
    }

    // ---- the first row -----------------------------------------------------------

    private void drawRow(GuiSurface surface, int left, int top, AbstractPet pet, PetBackpackMenu menu) {
        String name = pet.getDisplayName().getString();
        String job = PetStatusText.jobName(pet.getCapabilityId()).getString();
        Optional<LicenceView> licence = PetStatusText.licenceShown(pet.licenceViews(), pet.getCapabilityId());
        Optional<String> rank = licence.map(view -> PetStatusText.licenceRank(view).getString());
        int money = menu.petMoney();
        Optional<String> eager = pet.isEager() ? Optional.of(PetStatusText.clock(pet.eagerTicksLeft())) : Optional.empty();
        ItemStack hand = pet.getMainHandItem();
        HeaderRow row = HeaderRow.place(surface::textWidth, name, job, !hand.isEmpty(), rank, String.valueOf(money), eager);
        int y = top + PetScreenLayout.ROW_Y;

        String shown = TextClip.clip(name, row.nameWidth() / row.nameScale(), surface::textWidth);
        if (row.nameScale() > 1) {
            surface.drawTextScaled(shown, left + PetScreenLayout.TEXT_X, y, row.nameScale(), UiTheme.TEXT);
        } else {
            surface.drawText(shown, left + PetScreenLayout.TEXT_X, UiStyle.centerIn(y, Chip.HEIGHT, surface.lineHeight()),
                UiTheme.TEXT);
        }
        // With nothing in its hand there is nothing to picture the job by: a chip of its words, not a gap before them.
        Chip.draw(surface, hand.isEmpty() ? null : item(hand), TextClip.clip(job, row.jobWords(), surface::textWidth), left + row.jobX(), y,
            UiTheme.SURFACE, false);
        licence.ifPresent(view -> chip(surface, item(new ItemStack(BuiltInRegistries.ITEM.get(view.book()))), rank.orElse(""),
            left + row.rankX(), y, row.rankWidth(), UiTheme.LEAF_PALE, view.ready(),
            PetStatusText.licenceNotes(view).stream().map(Component::getString).toList()));

        ItemStack coin = Wallet.coins(1);
        chip(surface, item(coin), String.valueOf(money), left + row.moneyX(), y, row.moneyWidth(), UiTheme.LEAF_PALE, false,
            List.of(Component.translatable("screen.chiikawa.pet.money", money, coin.getHoverName()).getString()));
        eager.ifPresent(clock -> chip(surface, item(new ItemStack(InitItems.SIMPLE_DISH.get())), clock, left + row.eagerX(), y,
            row.eagerWidth(), UiTheme.ACCENT_SOFT, false, List.of(Component.translatable("screen.chiikawa.pet.eager", clock).getString())));
    }

    /** A chip as wide as the row has given it, with its words cut to fit, and what waits under the cursor over it. */
    private void chip(GuiSurface surface, Chip.Face face, String words, int x, int y, int width, int fill, boolean marked,
            List<String> note) {
        Chip.draw(surface, face, TextClip.clip(words, HeaderRow.wordsIn(width), surface::textWidth), x, y, fill, marked);
        notes.add(new Rect(x, y, width, Chip.HEIGHT), note);
    }

    private static Chip.Face item(ItemStack stack) {
        ItemIcon icon = new ItemIcon(stack);
        return (surface, x, y) -> surface.drawIcon(icon, x, y, FACE);
    }

    // ---- the orders --------------------------------------------------------------

    private static void drawOrders(GuiSurface surface, int left, int top, AbstractPet pet, int mouseX, int mouseY) {
        Rect area = PetScreenLayout.ORDERS.offset(left, top);
        List<String> names = orderNames();
        List<Rect> cells = Segments.cells(surface::textWidth, names, area);
        int hovered = -1;
        for (int index = 0; index < cells.size(); index++) {
            if (cells.get(index).contains(mouseX, mouseY)) {
                hovered = index;
            }
        }
        Segments.draw(surface, area, names, pet.getPetDirective().ordinal(), hovered);
    }

    private static List<String> orderNames() {
        List<String> names = new ArrayList<>();
        for (PetDirective directive : PetDirective.values()) {
            names.add(Component.translatable("screen.chiikawa.pet.order." + key(directive)).getString());
        }
        return names;
    }

    private static String key(PetDirective directive) {
        return switch (directive) {
            case FOLLOW -> "follow";
            case STAY -> "stay";
            case FREE -> "free";
        };
    }

    // ---- what it says ------------------------------------------------------------

    /**
     * One thing: what the pet is short of, if that is why it is not at work; else what it is at with its slip's progress; else
     * what it is at. A present it carries has a badge of its own beside the bubble.
     */
    private void drawSpeech(GuiSurface surface, int left, int top, AbstractPet pet, Optional<PetOutlookPayload> outlook) {
        ItemStack gift = pet.getPendingGift();
        Rect bubble = PetScreenLayout.bubble(!gift.isEmpty()).offset(left, top);
        Bubble.frame(surface, bubble, bubble.y() + bubble.height() / 2);
        Optional<PetTask> slip = pet.getTask();
        int textY = UiStyle.centerIn(bubble.y(), bubble.height(), surface.lineHeight());
        int iconY = UiStyle.centerIn(bubble.y(), bubble.height(), FACE);
        List<String> note = new ArrayList<>();

        if (outlook.flatMap(PetOutlookPayload::lackKey).isPresent()) {
            String reason = PetStatusText.notWorking(outlook.get()).map(Component::getString).orElse("");
            Optional<ItemStack> lacked = outlook.get().lackIcon().map(id -> new ItemStack(BuiltInRegistries.ITEM.get(id)))
                .filter(stack -> !stack.isEmpty());
            SpeechRow row = SpeechRow.lacking(surface::textWidth, bubble.width(), reason,
                lacked.map(stack -> stack.getHoverName().getString()).orElse(""));
            words(surface, reason, bubble.x() + SpeechRow.AIR, textY, row.textWidth(), UiTheme.TEXT);
            lacked.ifPresent(stack -> {
                surface.drawIcon(new ItemIcon(stack), bubble.x() + row.iconX(), iconY, FACE);
                if (row.nameX() >= 0) {
                    words(surface, stack.getHoverName().getString(), bubble.x() + row.nameX(), textY, row.nameWidth(),
                        UiTheme.TEXT_MUTED);
                }
            });
        } else if (slip.isPresent()) {
            PetTask task = slip.get();
            String count = PetStatusText.slipCount(task).getString();
            String doing = PetStatusText.activity(pet).getString();
            SpeechRow row = SpeechRow.working(surface::textWidth, bubble.width(), doing, count);
            words(surface, doing, bubble.x() + SpeechRow.AIR, textY, row.textWidth(), UiTheme.TEXT);
            Icon icon = ItemIcon.of(task.icon());
            surface.drawIcon(icon, bubble.x() + row.iconX(), iconY, FACE);
            if (row.hasBar()) {
                Bar.draw(surface, bubble.x() + row.barX(), UiStyle.centerIn(bubble.y(), bubble.height(), UiStyle.BAR_H),
                    row.barWidth(), UiStyle.BAR_H, task.progress(), task.target());
                surface.drawText(count, bubble.x() + row.countX(), textY, UiTheme.TEXT);
            }
            note.addAll(slipDetail(pet, task));
        } else {
            String doing = PetStatusText.activity(pet).getString();
            words(surface, doing, bubble.x() + SpeechRow.AIR, textY, SpeechRow.saying(surface::textWidth, bubble.width(), doing).textWidth(),
                UiTheme.TEXT);
        }
        // What it would do next, whichever of the three it says.
        outlook.flatMap(PetOutlookPayload::next).ifPresent(next -> {
            note.add(Component.translatable("screen.chiikawa.pet.outlook.next").getString());
            note.add(PetStatusText.next(next).getString());
        });
        notes.add(bubble, note);

        if (!gift.isEmpty()) {
            Chip.Face face = (target, x, y) -> PixelArt.GIFT.drawCentered(target, x, y, FACE);
            String label = TextClip.clip(Component.translatable("screen.chiikawa.pet.gift_badge").getString(),
                PetScreenLayout.GIFT_ROOM, surface::textWidth);
            int width = Chip.width(surface, label);
            Rect badge = new Rect(left + PetScreenLayout.RIGHT - width, top + PetScreenLayout.GIFT_Y, width, Chip.HEIGHT);
            Chip.draw(surface, face, label, badge.x(), badge.y(), UiTheme.ACCENT_SOFT, false);
            notes.add(badge, List.of(Component.translatable("screen.chiikawa.pet.gift_hint").getString()));
        }
    }

    private static void words(GuiSurface surface, String text, int x, int y, int width, int argb) {
        surface.drawText(TextClip.clip(text, width, surface::textWidth), x, y, argb);
    }

    /** The slip, spelled out: its name, what the pet is doing about it, how much, and for whom. */
    private static List<String> slipDetail(AbstractPet pet, PetTask task) {
        return List.of(
            PetStatusText.taskName(task.type()).getString(),
            PetStatusText.activity(pet).getString(),
            PetStatusText.taskAmount(task.type(), task.target()).getString(),
            PetStatusText.jobName(task.capability()).getString());
    }
}
