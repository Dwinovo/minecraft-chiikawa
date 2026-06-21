package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.menu.PetBackpackMenu;

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

    /** Info strip below the display: pet name + HP hearts on one line, vertically centered in y76..91. */
    private static final int NAME_Y = 80;
    private static final int HEARTS_Y = 79; // 9px heart aligned against the 8px name
    private static final int TEXT_COLOR = 0xFF5A3A2B; // theme dark brown, reads on cream

    /** HP heart sprites packed in the atlas (9x9 each), addressed by V offset. */
    private static final int HEART_U = 0;
    private static final int HEART_FULL_V = 178, HEART_HALF_V = 187, HEART_EMPTY_V = 196;
    private static final int HEART_SIZE = 9, HEART_STEP = 8; // 1px overlap like vanilla

    public PetBackpackScreen(PetBackpackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, PANEL_WIDTH, PANEL_HEIGHT);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

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
        graphics.text(this.font, name, nameX, NAME_Y, TEXT_COLOR, false);

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
}
