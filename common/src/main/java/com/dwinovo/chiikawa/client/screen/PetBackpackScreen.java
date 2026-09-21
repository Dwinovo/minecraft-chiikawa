package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.client.ui.PetStatusText;
import com.dwinovo.chiikawa.client.ui.mc.GuiSurface;
import com.dwinovo.chiikawa.client.ui.mc.ItemIcon;
import com.dwinovo.chiikawa.client.ui.mc.UiButton;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.menu.PetBackpackMenu;
import com.dwinovo.chiikawa.network.PetPayloads.PetDirectivePayload;
import com.dwinovo.chiikawa.platform.Services;
import com.dwinovo.chiikawa.task.PetTask;
import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.PixelArt;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import com.dwinovo.chiikawa.ui.widget.Bar;
import com.dwinovo.chiikawa.ui.widget.Hearts;
import com.dwinovo.chiikawa.ui.widget.Sign;
import com.dwinovo.chiikawa.ui.widget.Slot;
import com.dwinovo.chiikawa.ui.widget.Tabs;
import com.dwinovo.chiikawa.ui.widget.Tooltip;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * A pet's screen, in three pages behind three tabs: what it carries, how it is keeping,
 * and what it has been told to do.
 *
 * <p>Drawn after Chiikawa Pocket — paper stickers in an ink line, the pet's name on a
 * wooden board hung across the top edge, colour only where it means something — and
 * nothing on it is a texture. The panel is as tall as the page on it, and its top edge
 * never moves between pages, so the tabs and the name stay where the hand left them.
 */
public class PetBackpackScreen extends AbstractContainerScreen<PetBackpackMenu> {
    private static final int PANEL_WIDTH = 192;
    /** The backpack page, the tallest: the pet's things and the player's under them. */
    private static final int BACKPACK_HEIGHT = 214;
    private static final int ORDERS_HEIGHT = 136;

    /** Where the hearts sit: under the name board, above everything on the page. */
    private static final int HEARTS_Y = 12;
    /** Where a page's contents start. */
    private static final int CONTENT_Y = 24;
    private static final Rect PORTRAIT = new Rect(UiStyle.PAD, CONTENT_Y, 56, 64);
    private static final int PORTRAIT_SCALE = 34;
    /** Nudges the model down inside its window (entity-space units; +down). */
    private static final float PORTRAIT_Y_OFFSET = 0.18F;

    /** Backpack page: what the pet is at, in a strip under its picture. */
    private static final Rect WORK_STRIP = new Rect(UiStyle.PAD, 94, 56, 26);
    /** Backpack page: where the bag's ten go when there is a bag. */
    private static final Rect BAG_ROWS = new Rect(94, 82, 90, 36);
    private static final int DIVIDER_Y = 125;

    /** Status page: the slip card beside the picture. */
    private static final Rect WORK_CARD = new Rect(70, CONTENT_Y, 114, 40);
    private static final int CHIP_H = 20;

    private static final Rect ORDER_FIRST = new Rect(UiStyle.PAD, CONTENT_Y, PANEL_WIDTH - 2 * UiStyle.PAD, 31);
    private static final int ORDER_PITCH = 36;

    private static final int[] TAB_TINTS = {UiTheme.ACCENT_PALE, UiTheme.SKY_PALE, UiTheme.LEAF_PALE};
    private static final ItemStack BAG_ICON_STACK = new ItemStack(InitItems.BACKPACK.get());

    /** The pages, in tab order. */
    private enum Page {
        BACKPACK("screen.chiikawa.pet.tab.backpack"),
        STATUS("screen.chiikawa.pet.tab.status"),
        ORDERS("screen.chiikawa.pet.tab.orders");

        private final String key;

        Page(String key) {
            this.key = key;
        }
    }

    private final List<Tabs.Face> tabFaces = List.of(
        (surface, x, y) -> surface.drawIcon(new ItemIcon(BAG_ICON_STACK), x, y),
        (surface, x, y) -> PixelArt.SLIP.drawCentered(surface, x, y, UiStyle.ICON),
        (surface, x, y) -> PixelArt.SPEECH.drawCentered(surface, x, y, UiStyle.ICON));

    private Page page = Page.BACKPACK;
    /** Where the chips on the status page ended up, for their tooltips. */
    private final List<HoverNote> notes = new ArrayList<>();

    /** A patch of the screen with something to say under the cursor. */
    private record HoverNote(Rect area, List<String> lines) {
    }

    public PetBackpackScreen(PetBackpackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PANEL_WIDTH;
        this.imageHeight = BACKPACK_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        // The top edge is placed for the tallest page and stays put for the others.
        this.leftPos = (this.width - PANEL_WIDTH) / 2;
        this.topPos = (this.height - BACKPACK_HEIGHT + Tabs.HEIGHT) / 2;
        showPage(page);
    }

    private void showPage(Page shown) {
        this.page = shown;
        this.menu.showSlots(shown == Page.BACKPACK);
        this.imageHeight = shown == Page.BACKPACK ? BACKPACK_HEIGHT : shown == Page.ORDERS ? ORDERS_HEIGHT : statusHeight();
        clearWidgets();
        if (shown == Page.ORDERS) {
            PetDirective[] orders = PetDirective.values();
            for (int i = 0; i < orders.length; i++) {
                addRenderableWidget(orderButton(orders[i], i));
            }
        }
    }

    // ---- drawing -------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (page == Page.STATUS) {
            this.imageHeight = statusHeight();
        }
        notes.clear();
        super.render(graphics, mouseX, mouseY, partialTick);
        renderNotes(graphics, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        GuiSurface surface = new GuiSurface(graphics, this.font);
        int x = this.leftPos;
        int y = this.topPos;
        Tabs.drawBehind(surface, x, y, tabFaces, TAB_TINTS, page.ordinal());
        Ui.card(surface, x, y, this.imageWidth, this.imageHeight);
        Tabs.drawFront(surface, x, y, tabFaces.get(page.ordinal()), page.ordinal());

        AbstractPet pet = this.menu.getPet(Minecraft.getInstance().level);
        if (pet == null) {
            return;
        }
        String name = pet.getDisplayName().getString();
        Sign.draw(surface, x + this.imageWidth - 10 - Sign.width(surface, name), y - 9, name);
        int maxHealth = Math.max(1, Mth.ceil(pet.getMaxHealth()));
        Hearts.draw(surface, x + this.imageWidth - 10 - Hearts.width(maxHealth), y + HEARTS_Y,
            Math.max(0, Mth.ceil(pet.getHealth())), maxHealth);

        switch (page) {
            case BACKPACK -> drawBackpack(graphics, surface, pet, mouseX, mouseY);
            case STATUS -> drawStatus(graphics, surface, pet, mouseX, mouseY);
            case ORDERS -> {
                // The three orders are buttons, and draw themselves.
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // The name board and the pages stand in for the default title and "Inventory" labels.
    }

    private void drawPortrait(GuiGraphics graphics, DrawSurface surface, AbstractPet pet, int mouseX, int mouseY) {
        Rect at = PORTRAIT.offset(this.leftPos, this.topPos);
        Ui.sticker(surface, at.x(), at.y(), at.width(), at.height(), Ui.CARD_RADIUS, UiTheme.SKY_PALE);
        // 1.20.1 only offers the point-based renderEntityInInventoryFollowsMouse, so clip
        // to the window and stand the pet in it the way the rectangle variant does later:
        // its middle at the window's centre, the head turned from there towards the mouse.
        int x1 = at.x() + 2;
        int y1 = at.y() + 2;
        int x2 = at.right() - 2;
        int y2 = at.bottom() - 2;
        float centerX = (x1 + x2) / 2.0F;
        float centerY = (y1 + y2) / 2.0F;
        graphics.enableScissor(x1, y1, x2, y2);
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, (int) centerX,
            (int) (centerY + PORTRAIT_SCALE * (pet.getBbHeight() / 2.0F + PORTRAIT_Y_OFFSET)),
            PORTRAIT_SCALE, centerX - mouseX, centerY - mouseY, pet);
        graphics.disableScissor();
    }

    /**
     * The pet's things: its hand and bag beside its picture, its pockets, and the bag's
     * rows — or, with no bag worn, a dashed space that says what goes there.
     */
    private void drawBackpack(GuiGraphics graphics, GuiSurface surface, AbstractPet pet, int mouseX, int mouseY) {
        drawPortrait(graphics, surface, pet, mouseX, mouseY);
        // A well behind every slot, each one asked where it is — the menu owns the layout.
        for (net.minecraft.world.inventory.Slot slot : this.menu.slots) {
            if (slot.isActive()) {
                Ui.well(surface, this.leftPos + slot.x - 1, this.topPos + slot.y - 1, UiStyle.SLOT, UiStyle.SLOT);
            }
        }
        if (!pet.isWearingBag()) {
            Rect bag = BAG_ROWS.offset(this.leftPos, this.topPos);
            dashed(surface, bag);
            surface.drawIcon(new ItemIcon(BAG_ICON_STACK), bag.x() + 26, UiStyle.centerIn(bag.y(), bag.height(), UiStyle.ICON));
            surface.drawText("+10", bag.x() + 46, UiStyle.centerIn(bag.y(), bag.height(), surface.lineHeight()), UiTheme.TEXT_MUTED);
            notes.add(new HoverNote(bag, List.of(Component.translatable("screen.chiikawa.pet.bag_hint").getString())));
        }
        drawWorkStrip(surface, pet);
        Ui.divider(surface, this.leftPos, this.topPos + DIVIDER_Y, this.imageWidth);
    }

    /** What the pet is at, small: the slip's picture, the count and the bar, or a word. */
    private void drawWorkStrip(GuiSurface surface, AbstractPet pet) {
        Rect strip = WORK_STRIP.offset(this.leftPos, this.topPos);
        Ui.sticker(surface, strip.x(), strip.y(), strip.width(), strip.height(), Ui.CARD_RADIUS, UiTheme.LEAF_PALE);
        Optional<PetTask> slip = pet.getTask();
        if (slip.isPresent()) {
            PetTask task = slip.get();
            surface.drawIcon(ItemIcon.of(task.icon()), strip.x() + 3, UiStyle.centerIn(strip.y(), strip.height(), UiStyle.ICON));
            surface.drawText(PetStatusText.slipCount(task).getString(), strip.x() + 21, strip.y() + 4, UiTheme.TEXT);
            Bar.draw(surface, strip.x() + 21, strip.y() + 15, strip.width() - 25, UiStyle.BAR_H,
                task.progress(), task.target());
            notes.add(new HoverNote(strip, slipDetail(pet, task)));
        } else {
            Ui.textClipped(surface, PetStatusText.activity(pet).getString(), strip.x() + UiStyle.GAP,
                UiStyle.centerIn(strip.y(), strip.height(), surface.lineHeight()),
                strip.width() - 2 * UiStyle.GAP, UiTheme.TEXT_MUTED);
        }
    }

    /**
     * How the pet is keeping: the slip in full, its mood and its money as two chips, the
     * present it is carrying for its owner, and its job.
     */
    private void drawStatus(GuiGraphics graphics, GuiSurface surface, AbstractPet pet, int mouseX, int mouseY) {
        drawPortrait(graphics, surface, pet, mouseX, mouseY);
        Rect card = WORK_CARD.offset(this.leftPos, this.topPos);
        Ui.sticker(surface, card.x(), card.y(), card.width(), card.height(), Ui.CARD_RADIUS, UiTheme.LEAF_PALE);
        Optional<PetTask> slip = pet.getTask();
        if (slip.isPresent()) {
            PetTask task = slip.get();
            Slot.draw(surface, ItemIcon.of(task.icon()), card.x() + 5, card.y() + 5);
            Ui.textClipped(surface, PetStatusText.taskName(task.type()).getString(), card.x() + 28, card.y() + 6,
                card.width() - 32, UiTheme.TEXT);
            Ui.textClipped(surface, PetStatusText.activity(pet).getString(), card.x() + 28, card.y() + 16,
                card.width() - 32, UiTheme.TEXT_MUTED);
            String count = PetStatusText.slipCount(task).getString();
            Ui.textRight(surface, count, card.right() - 6, card.y() + 28, UiTheme.TEXT);
            Bar.draw(surface, card.x() + 5, card.y() + 28, card.width() - 16 - surface.textWidth(count),
                UiStyle.BAR_H, task.progress(), task.target());
            notes.add(new HoverNote(card, slipDetail(pet, task)));
        } else {
            surface.drawText(PetStatusText.activity(pet).getString(), card.x() + UiStyle.PAD, card.y() + 9, UiTheme.TEXT);
            surface.drawText(Component.translatable("screen.chiikawa.pet.no_slip").getString(),
                card.x() + UiStyle.PAD, card.y() + 21, UiTheme.TEXT_MUTED);
        }

        // Mood and money, a chip each; what they mean waits under the cursor.
        int chipX = card.x();
        int chipY = card.bottom() + UiStyle.GAP;
        if (pet.isEager()) {
            String left = clock(pet.eagerTicksLeft());
            chipX += chip(surface, chipX, chipY, new ItemIcon(new ItemStack(InitItems.SIMPLE_DISH.get())), left,
                UiTheme.ACCENT_PALE, List.of(Component.translatable("screen.chiikawa.pet.eager", left).getString()))
                + UiStyle.GAP;
        }
        int emeralds = this.menu.petEmeralds();
        chip(surface, chipX, chipY, new ItemIcon(new ItemStack(net.minecraft.world.item.Items.EMERALD)),
            String.valueOf(emeralds), UiTheme.LEAF_PALE,
            List.of(Component.translatable("screen.chiikawa.pet.money", emeralds).getString()));

        int y = this.topPos + PORTRAIT.bottom() + 6;
        ItemStack gift = pet.getPendingGift();
        if (!gift.isEmpty()) {
            int giftX = this.leftPos + UiStyle.PAD;
            int giftW = this.imageWidth - 2 * UiStyle.PAD;
            Ui.sticker(surface, giftX, y, giftW, 30, Ui.CARD_RADIUS, UiTheme.ACCENT_PALE);
            Slot.draw(surface, new ItemIcon(gift), giftX + 6, y + 6);
            surface.drawText(Component.translatable("screen.chiikawa.pet.gift").getString(), giftX + 30, y + 7, UiTheme.TEXT);
            surface.drawText(Component.translatable("screen.chiikawa.pet.gift_hint").getString(), giftX + 30, y + 17,
                UiTheme.TEXT_MUTED);
            y += 30 + 6;
        }
        chip(surface, this.leftPos + UiStyle.PAD, y, new ItemIcon(pet.getMainHandItem()),
            PetStatusText.jobName(pet.getCapabilityId()).getString(), UiTheme.SURFACE, List.of());
    }

    /**
     * How tall the status page is: the picture and what is beside it, the present when
     * there is one, and the job chip under that.
     */
    private int statusHeight() {
        AbstractPet pet = this.menu.getPet(Minecraft.getInstance().level);
        boolean gift = pet != null && !pet.getPendingGift().isEmpty();
        return PORTRAIT.bottom() + 6 + (gift ? 36 : 0) + CHIP_H + UiStyle.PAD;
    }

    /** A small sticker: a picture and a few words, with more waiting under the cursor. */
    private int chip(GuiSurface surface, int x, int y, ItemIcon icon, String label, int fill, List<String> note) {
        int width = 3 + UiStyle.ICON + 3 + surface.textWidth(label) + 6;
        Ui.sticker(surface, x, y, width, CHIP_H, Ui.CARD_RADIUS, fill);
        surface.drawIcon(icon, x + 3, y + 2);
        surface.drawText(label, x + 3 + UiStyle.ICON + 3, UiStyle.centerIn(y, CHIP_H, surface.lineHeight()), UiTheme.TEXT);
        if (!note.isEmpty()) {
            notes.add(new HoverNote(new Rect(x, y, width, CHIP_H), note));
        }
        return width;
    }

    /** One of the three orders, as a card to pick; the one in force is pink and ticked. */
    private UiButton orderButton(PetDirective directive, int index) {
        Rect at = ORDER_FIRST.offset(this.leftPos, this.topPos + index * ORDER_PITCH);
        Component name = Component.translatable("screen.chiikawa.pet.order." + key(directive));
        return UiButton.painted(at.x(), at.y(), at.width(), at.height(), name,
            (surface, area, hovered, active) -> {
                AbstractPet pet = this.menu.getPet(Minecraft.getInstance().level);
                boolean current = pet != null && pet.getPetDirective() == directive;
                int fill = current ? UiTheme.ACCENT_SOFT : hovered ? UiTheme.ACCENT_PALE : UiTheme.PANEL;
                Ui.sticker(surface, area.x(), area.y(), area.width(), area.height(), Ui.PANEL_RADIUS, fill);
                Ui.roundRect(surface, area.x() + 5, area.y() + 5, 21, 21, Ui.CARD_RADIUS, UiTheme.INK);
                Ui.roundRect(surface, area.x() + 6, area.y() + 6, 19, 19, Ui.CARD_RADIUS - 1,
                    current ? UiTheme.ACCENT_PALE : UiTheme.SURFACE);
                icon(directive).drawCentered(surface, area.x() + 7, area.y() + 7, 17);
                surface.drawText(name.getString(), area.x() + 32, area.y() + 6, UiTheme.TEXT);
                Ui.textClipped(surface, Component.translatable("screen.chiikawa.pet.order." + key(directive) + ".hint").getString(),
                    area.x() + 32, area.y() + 17, area.width() - 32 - 22, UiTheme.TEXT_MUTED);
                if (current) {
                    PixelArt.CHECK.draw(surface, area.right() - 18, UiStyle.centerIn(area.y(), area.height(), 7));
                }
            },
            () -> {
                AbstractPet pet = this.menu.getPet(Minecraft.getInstance().level);
                if (pet != null && pet.getPetDirective() != directive) {
                    Services.NETWORK.sendToServer(new PetDirectivePayload(pet.getId(), directive));
                }
            });
    }

    private static String key(PetDirective directive) {
        return switch (directive) {
            case FOLLOW -> "follow";
            case STAY -> "stay";
            case FREE -> "free";
        };
    }

    private static PixelArt icon(PetDirective directive) {
        return switch (directive) {
            case FOLLOW -> PixelArt.PAWS;
            case STAY -> PixelArt.CUSHION;
            case FREE -> PixelArt.TUFT;
        };
    }

    /** A dashed outline: a space something could go in, not a thing that is there. */
    private static void dashed(DrawSurface surface, Rect area) {
        for (int at = 0; at < area.width(); at += 4) {
            surface.fillRect(area.x() + at, area.y(), Math.min(2, area.width() - at), 1, UiTheme.SHADE);
            surface.fillRect(area.x() + at, area.bottom() - 1, Math.min(2, area.width() - at), 1, UiTheme.SHADE);
        }
        for (int at = 0; at < area.height(); at += 4) {
            surface.fillRect(area.x(), area.y() + at, 1, Math.min(2, area.height() - at), UiTheme.SHADE);
            surface.fillRect(area.right() - 1, area.y() + at, 1, Math.min(2, area.height() - at), UiTheme.SHADE);
        }
    }

    /** Ticks as minutes and seconds, the way a timer on a label reads. */
    private static String clock(long ticks) {
        long seconds = ticks / 20;
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }

    // ---- what waits under the cursor ---------------------------------------------------

    private void renderNotes(GuiGraphics graphics, int mouseX, int mouseY) {
        GuiSurface surface = new GuiSurface(graphics, this.font);
        int tab = Tabs.at(this.leftPos, this.topPos, Page.values().length, mouseX, mouseY);
        List<String> lines = null;
        if (tab >= 0) {
            lines = List.of(Component.translatable(Page.values()[tab].key).getString());
        } else {
            for (HoverNote note : notes) {
                if (note.area().contains(mouseX, mouseY)) {
                    lines = note.lines();
                }
            }
        }
        if (lines != null && !lines.isEmpty()) {
            List<String> shown = lines;
            surface.onTop(() -> Tooltip.draw(surface, shown, mouseX, mouseY, this.width, this.height));
        }
    }

    /** The slip, spelled out: its name, what the pet is doing about it, how much, and for whom. */
    private static List<String> slipDetail(AbstractPet pet, PetTask task) {
        return List.of(
            PetStatusText.taskName(task.type()).getString(),
            PetStatusText.activity(pet).getString(),
            PetStatusText.taskAmount(task.type(), task.target()).getString(),
            PetStatusText.jobName(task.capability()).getString());
    }

    // ---- input -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int tab = Tabs.at(this.leftPos, this.topPos, Page.values().length, (int) mouseX, (int) mouseY);
        if (tab >= 0 && button == 0) {
            if (tab != page.ordinal()) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                showPage(Page.values()[tab]);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        // The tabs and the name board stand above the panel; a click there is not a drop.
        return super.hasClickedOutside(mouseX, mouseY, left, top, button)
            && Tabs.at(this.leftPos, this.topPos, Page.values().length, (int) mouseX, (int) mouseY) < 0;
    }
}
