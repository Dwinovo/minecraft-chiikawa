package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.menu.PetBackpackMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;

public class PetBackpackScreen extends AbstractContainerScreen<PetBackpackMenu> {


    private static final ResourceLocation TEXTURE = new ResourceLocation(Constants.MOD_ID, "textures/gui/pet_backpack.png");

    public PetBackpackScreen(PetBackpackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Draw background.
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        // Draw pet preview.
        LivingEntity pet = this.menu.getPet(Minecraft.getInstance().level);
        if (pet != null) {
            int centerX = this.leftPos + 51;
            int centerY = this.topPos + 50;
            int size = 30;
            // 1.20.1 API: renderEntityInInventoryFollowsMouse(graphics, x, y, size, rotationX, rotationY, entity)
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics,
                    centerX,
                    centerY,
                    size,
                    (float) (centerX - mouseX),
                    (float) (centerY - mouseY - pet.getEyeHeight() * size),
                    pet);
        }
    }
    
}
