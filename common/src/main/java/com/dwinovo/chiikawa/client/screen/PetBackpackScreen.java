package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.client.ui.PetStatusText;
import com.dwinovo.chiikawa.client.ui.mc.GuiSurface;
import com.dwinovo.chiikawa.client.ui.mc.ItemIcon;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.menu.PetBackpackMenu;
import com.dwinovo.chiikawa.task.PetTask;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import com.dwinovo.chiikawa.ui.widget.Bar;
import com.dwinovo.chiikawa.ui.widget.Slot;
import com.dwinovo.chiikawa.ui.widget.Tooltip;
import java.util.List;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;

public class PetBackpackScreen extends AbstractContainerScreen<PetBackpackMenu> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/pet_backpack.png");

    /** Panel size — matches the pet_gui texture (drawn from the 256x256 atlas top-left). */
    private static final int PANEL_WIDTH = 200;
    private static final int PANEL_HEIGHT = 178;

    /** Pet display window (GUI-local) where the live entity is rendered. */
    private static final int DISPLAY_X1 = 14, DISPLAY_Y1 = 15, DISPLAY_X2 = 69, DISPLAY_Y2 = 75;
    private static final int DISPLAY_SCALE = 40;
    /** Nudges the model down inside the window (entity-space units; +down). Pet models sit taller than their hitbox. */
    private static final float DISPLAY_Y_OFFSET = 0.18F;

    /** The card above the panel: what the pet is at, and how far along its slip is. */
    private static final int CARD_H = UiStyle.SLOT + 2 * UiStyle.GAP;
    /** Long enough that a slip barely started and one nearly done look nothing alike. */
    private static final int CARD_BAR_W = 56;

    /** Info strip below the display: pet name + HP hearts on one line, vertically centered in y76..91. */
    private static final int NAME_Y = 80;
    private static final int HEARTS_Y = 79;

    /** HP heart sprites packed in the atlas (9x9 each), addressed by V offset. */
    private static final int HEART_U = 0;
    private static final int HEART_FULL_V = 178, HEART_HALF_V = 187, HEART_EMPTY_V = 196;
    private static final int HEART_SIZE = 9, HEART_STEP = 8; // 1px overlap like vanilla

    public PetBackpackScreen(PetBackpackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, PANEL_WIDTH, PANEL_HEIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // In 1.21.11 AbstractContainerScreen.render() no longer draws the item tooltip itself;
        // every vanilla container screen overrides render() and calls renderTooltip() explicitly.
        super.render(graphics, mouseX, mouseY, partialTick);
        renderStatusCard(graphics, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        LivingEntity pet = this.menu.getPet(Minecraft.getInstance().level);
        if (pet != null) {
            InventoryScreen.extractEntityInInventoryFollowsMouse(
                    graphics,
                    this.leftPos + DISPLAY_X1, this.topPos + DISPLAY_Y1,
                    this.leftPos + DISPLAY_X2, this.topPos + DISPLAY_Y2,
                    DISPLAY_SCALE, DISPLAY_Y_OFFSET, mouseX, mouseY, pet);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Replace the default title / "Inventory" labels with the pet's name + HP hearts.
        // Coordinates here are GUI-local (the foreground layer is translated to leftPos/topPos).
        LivingEntity pet = this.menu.getPet(Minecraft.getInstance().level);
        if (pet == null) {
            return;
        }

        // Name centered just below the display window; HP hearts flow to its right on the same line.
        Component name = pet.getDisplayName();
        int nameWidth = this.font.width(name);
        int displayCenterX = (DISPLAY_X1 + DISPLAY_X2) / 2;
        int nameX = displayCenterX - nameWidth / 2;
        graphics.drawString(this.font, name, nameX, NAME_Y, UiTheme.TEXT, false);

        int maxHp = Math.max(1, Mth.ceil(pet.getMaxHealth()));
        int hp = Math.max(0, Mth.ceil(pet.getHealth()));
        int hearts = (maxHp + 1) / 2;
        int heartsX = nameX + nameWidth + 6;
        for (int i = 0; i < hearts; i++) {
            int remain = hp - i * 2;
            int v = remain >= 2 ? HEART_FULL_V : (remain == 1 ? HEART_HALF_V : HEART_EMPTY_V);
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                    heartsX + i * HEART_STEP, HEARTS_Y, (float) HEART_U, (float) v,
                    HEART_SIZE, HEART_SIZE, 256, 256);
        }
    }

    /**
     * What the owner opened the screen to find out, above the panel and on its own card: a
     * picture of the work, what the pet is at, and a bar. The slip's own particulars — what
     * it is called, how much it asks for, which job it is for — wait under the cursor,
     * where they cost nothing to anyone not asking.
     */
    private void renderStatusCard(GuiGraphics graphics, int mouseX, int mouseY) {
        AbstractPet pet = this.menu.getPet(Minecraft.getInstance().level);
        if (pet == null) {
            return;
        }
        GuiSurface surface = new GuiSurface(graphics, this.font);
        Rect card = new Rect(this.leftPos, this.topPos - CARD_H - UiStyle.GAP, this.imageWidth, CARD_H);
        Ui.card(surface, card.x(), card.y(), card.width(), card.height());

        Optional<PetTask> slip = pet.getTask();
        int textX = card.x() + UiStyle.PAD;
        if (slip.isPresent()) {
            Slot.draw(surface, ItemIcon.of(slip.get().icon()), card.x() + UiStyle.GAP,
                UiStyle.centerIn(card.y(), card.height(), UiStyle.SLOT));
            textX = card.x() + UiStyle.GAP + UiStyle.SLOT + UiStyle.GAP;
        }
        int textY = UiStyle.centerIn(card.y(), card.height(), surface.lineHeight());
        int textRoom = card.right() - UiStyle.PAD - textX;

        if (slip.isPresent()) {
            PetTask task = slip.get();
            int barX = card.right() - UiStyle.PAD - CARD_BAR_W;
            Bar.draw(surface, barX, UiStyle.centerIn(card.y(), card.height(), UiStyle.BAR_H),
                CARD_BAR_W, UiStyle.BAR_H, task.progress(), task.target());
            String count = PetStatusText.slipCount(task).getString();
            Ui.textRight(surface, count, barX - UiStyle.GAP, textY, UiTheme.TEXT_MUTED);
            textRoom = barX - UiStyle.GAP - surface.textWidth(count) - UiStyle.GAP - textX;
        }
        Ui.textClipped(surface, PetStatusText.activity(pet).getString(), textX, textY, textRoom, UiTheme.TEXT);

        if (card.contains(mouseX, mouseY)) {
            slip.ifPresent(task -> surface.onTop(() ->
                Tooltip.draw(surface, detail(task), mouseX, mouseY, this.width, this.height)));
        }
    }

    /** The slip, spelled out: what it is, how much it asks for, and whose work it is. */
    private static List<String> detail(PetTask task) {
        return List.of(
            PetStatusText.taskName(task.type()).getString(),
            PetStatusText.taskAmount(task.type(), task.target()).getString(),
            PetStatusText.jobName(task.capability()).getString());
    }
}
