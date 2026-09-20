package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.client.ui.PetStatusText;
import com.dwinovo.chiikawa.client.ui.mc.GuiSurface;
import com.dwinovo.chiikawa.client.ui.mc.ItemIcon;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.menu.PetBackpackMenu;
import com.dwinovo.chiikawa.task.PetTask;
import com.dwinovo.chiikawa.ui.DrawSurface;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import com.dwinovo.chiikawa.ui.widget.Bar;
import com.dwinovo.chiikawa.ui.widget.Hearts;
import com.dwinovo.chiikawa.ui.widget.Slot;
import com.dwinovo.chiikawa.ui.widget.Tooltip;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;

/**
 * A pet's backpack, drawn rather than blitted: the panel, the wells under every slot, the
 * hearts and the bar all come out of the {@code chiikawa-ui} library, so the screen keeps
 * step with the palette and there is no image to redraw when the layout moves.
 *
 * <p>Two things the owner came for, one above the line and one below. Above is the pet —
 * its picture, what it holds, what it carries, how it is doing; below is the player's own
 * inventory. What the pet is at sits in a strip among its things rather than on a card
 * floating over the panel, where it read as a notice pinned to someone else's board.
 */
public class PetBackpackScreen extends AbstractContainerScreen<PetBackpackMenu> {
    private static final int PANEL_WIDTH = 196;
    private static final int PANEL_HEIGHT = 224;

    /** The window the live pet is shown in, inside a well of its own. */
    private static final int PORTRAIT_X = UiStyle.PAD, PORTRAIT_Y = UiStyle.PAD;
    private static final int PORTRAIT_W = 60, PORTRAIT_H = 64;
    private static final int PORTRAIT_SCALE = 40;
    /** Nudges the model down inside the window (entity-space units; +down). */
    private static final float PORTRAIT_Y_OFFSET = 0.18F;

    /** Where the bag's own rows sit, empty until a bag is worn. */
    private static final int BAG_ROWS_Y = 59;
    private static final int BAG_ROWS_H = 2 * 17;
    /** The pet's name, and how much of it is left, on one line under everything it carries. */
    private static final int NAME_Y = BAG_ROWS_Y + BAG_ROWS_H + UiStyle.GAP;
    /**
     * What the pet is at, on the line under its name. Written straight onto the panel and
     * not in a well: a well is where a thing is put, so an idle pet's two words sat in a
     * long empty box, and an empty box is emptiness drawn large. Bare text with room after
     * it is just room.
     */
    private static final int STATUS_Y = NAME_Y + UiStyle.LINE + UiStyle.GAP;
    /** Tall enough for the work's icon on the days there is one. */
    private static final int STATUS_H = UiStyle.SLOT;
    /** Long enough that a slip barely started and one nearly done look nothing alike. */
    private static final int STATUS_BAR_W = 56;
    /** The line between the pet's things and the player's own. */
    private static final int DIVIDER_Y = STATUS_Y + STATUS_H + UiStyle.PAD;

    public PetBackpackScreen(PetBackpackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PANEL_WIDTH;
        this.imageHeight = PANEL_HEIGHT;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // In 1.21.11 AbstractContainerScreen.render() no longer draws the item tooltip itself;
        // every vanilla container screen overrides render() and calls renderTooltip() explicitly.
        super.render(graphics, mouseX, mouseY, partialTick);
        renderSlipTooltip(graphics, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        DrawSurface surface = new GuiSurface(graphics, this.font);
        Ui.card(surface, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
        Ui.well(surface, this.leftPos + PORTRAIT_X, this.topPos + PORTRAIT_Y, PORTRAIT_W, PORTRAIT_H);

        // A well behind every slot, each one asked where it is — the menu owns the layout,
        // and the screen never keeps a second copy of it to fall out of step. A slot that is
        // not in use yet gets no well: an empty well says "put something here", which the
        // bag's rows cannot honour until there is a bag.
        for (net.minecraft.world.inventory.Slot slot : this.menu.slots) {
            if (slot.isActive()) {
                Ui.well(surface, this.leftPos + slot.x - UiStyle.BORDER, this.topPos + slot.y - UiStyle.BORDER,
                    UiStyle.SLOT, UiStyle.SLOT);
            }
        }
        Ui.divider(surface, this.leftPos, this.topPos + DIVIDER_Y, this.imageWidth);

        LivingEntity pet = this.menu.getPet(Minecraft.getInstance().level);
        if (pet != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                this.leftPos + PORTRAIT_X + UiStyle.BORDER, this.topPos + PORTRAIT_Y + UiStyle.BORDER,
                this.leftPos + PORTRAIT_X + PORTRAIT_W - UiStyle.BORDER,
                this.topPos + PORTRAIT_Y + PORTRAIT_H - UiStyle.BORDER,
                PORTRAIT_SCALE, PORTRAIT_Y_OFFSET, mouseX, mouseY, pet);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // The pet's name and state stand in for the default title and "Inventory" labels.
        // Coordinates here are panel-local: the label pass is translated to leftPos/topPos.
        AbstractPet pet = this.menu.getPet(Minecraft.getInstance().level);
        if (pet == null) {
            return;
        }
        DrawSurface surface = new GuiSurface(graphics, this.font);
        int maxHealth = Math.max(1, Mth.ceil(pet.getMaxHealth()));
        int heartsWidth = Hearts.width(maxHealth);
        int heartsX = this.imageWidth - UiStyle.PAD - heartsWidth;

        Hearts.draw(surface, heartsX, NAME_Y, Math.max(0, Mth.ceil(pet.getHealth())), maxHealth);
        Ui.textClipped(surface, pet.getDisplayName().getString(), UiStyle.PAD, NAME_Y,
            heartsX - UiStyle.GAP_SECTION - UiStyle.PAD, UiTheme.TEXT);

        if (!pet.isWearingBag()) {
            // Says what the empty band is for, rather than leaving a hole in the panel.
            Ui.emptyState(surface, Component.translatable("screen.chiikawa.pet.bag_hint").getString(),
                this.imageWidth / 2, UiStyle.centerIn(BAG_ROWS_Y, BAG_ROWS_H, surface.lineHeight()));
        }
        drawStatus(surface, pet, UiStyle.PAD, STATUS_Y, this.imageWidth - 2 * UiStyle.PAD);
    }

    /** What the pet is at: the work's picture, what it is doing, the count and the bar. */
    private void drawStatus(DrawSurface surface, AbstractPet pet, int x, int y, int width) {
        Optional<PetTask> slip = pet.getTask();
        int textX = x;
        if (slip.isPresent()) {
            Slot.draw(surface, ItemIcon.of(slip.get().icon()), x, UiStyle.centerIn(y, STATUS_H, UiStyle.SLOT));
            textX = x + UiStyle.SLOT + UiStyle.GAP;
        }
        int textY = UiStyle.centerIn(y, STATUS_H, surface.lineHeight());
        int textRoom = x + width - textX;

        if (slip.isPresent()) {
            PetTask task = slip.get();
            int barX = x + width - STATUS_BAR_W;
            Bar.draw(surface, barX, UiStyle.centerIn(y, STATUS_H, UiStyle.BAR_H),
                STATUS_BAR_W, UiStyle.BAR_H, task.progress(), task.target());
            String count = PetStatusText.slipCount(task).getString();
            Ui.textRight(surface, count, barX - UiStyle.GAP, textY, UiTheme.TEXT_MUTED);
            textRoom = barX - UiStyle.GAP - surface.textWidth(count) - UiStyle.GAP - textX;
            // The slip's own name, which is what the picture and the bar are about.
            Ui.textClipped(surface, PetStatusText.taskName(task.type()).getString(), textX, textY,
                textRoom, UiTheme.TEXT);
            return;
        }
        Ui.textClipped(surface, PetStatusText.activity(pet).getString(), textX, textY, textRoom, UiTheme.TEXT);
    }

    /**
     * The slip, spelled out for whoever points at the strip. It repeats nothing the strip
     * already shows but its name, which is what makes the rest of it mean anything.
     */
    private void renderSlipTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        AbstractPet pet = this.menu.getPet(Minecraft.getInstance().level);
        if (pet == null) {
            return;
        }
        Rect strip = new Rect(this.leftPos + UiStyle.PAD, this.topPos + STATUS_Y,
            this.imageWidth - 2 * UiStyle.PAD, STATUS_H);
        if (!strip.contains(mouseX, mouseY)) {
            return;
        }
        GuiSurface surface = new GuiSurface(graphics, this.font);
        pet.getTask().ifPresent(task -> surface.onTop(() ->
            Tooltip.draw(surface, detail(task), mouseX, mouseY, this.width, this.height)));
    }

    private List<String> detail(PetTask task) {
        AbstractPet pet = this.menu.getPet(Minecraft.getInstance().level);
        return List.of(
            PetStatusText.taskName(task.type()).getString(),
            // What the pet is at this second: the line itself now names the slip.
            PetStatusText.activity(pet).getString(),
            PetStatusText.taskAmount(task.type(), task.target()).getString(),
            PetStatusText.jobName(task.capability()).getString());
    }
}
