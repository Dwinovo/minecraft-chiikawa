package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.client.ui.mc.GuiSurface;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.menu.PetBackpackMenu;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiTheme;
import com.dwinovo.chiikawa.ui.widget.Banner;
import com.dwinovo.chiikawa.ui.widget.Tooltip;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * A pet's screen: what it carries and who it is, on one page.
 *
 * <p>Drawn after Chiikawa Pocket — paper stickers in an ink line, colour only where it means something — and nothing on it is
 * a texture. Over the page is a banner, with the pet in it standing on its floor and rising over the panel's top edge, so the
 * pet, its name, its health and its order are in front of the hand; under it are the pet's slots and the player's, and under
 * the player's hotbar the one switch an owner has for a pet, whether it picks up what lies about.
 */
public class PetBackpackScreen extends AbstractContainerScreen<PetBackpackMenu> {
    private final HoverNotes notes = new HoverNotes();
    private final PetHeader header = new PetHeader(notes);
    private final BackpackPage backpackPage = new BackpackPage(notes);
    private final PickUpRow pickUpRow = new PickUpRow(notes);

    public PetBackpackScreen(PetBackpackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, PetScreenLayout.PANEL_W, PetScreenLayout.PANEL_H);
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = PetScreenLayout.left(this.width);
        this.topPos = PetScreenLayout.top(this.height);
        header.buttons(this.leftPos, this.topPos, this.font::width, this::pet).forEach(this::addRenderableWidget);
        addRenderableWidget(pickUpRow.button(this.leftPos, this.topPos, this::pet));
    }

    private AbstractPet pet() {
        return this.menu.getPet(Minecraft.getInstance().level);
    }

    // ---- drawing -------------------------------------------------------------------

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        renderNotes(graphics, mouseX, mouseY);
    }

    /** The first of a frame's drawing, so the page's notes start over here. */
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        notes.clear();
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        GuiSurface surface = new GuiSurface(graphics, this.font);
        int x = this.leftPos;
        int y = this.topPos;
        Ui.card(surface, x, y, this.imageWidth, this.imageHeight);
        Banner.draw(surface, x + 2, y + 2, this.imageWidth - 4, PetScreenLayout.BANNER_H - 2, UiTheme.ACCENT_PALE);

        AbstractPet pet = pet();
        if (pet == null) {
            return;
        }
        header.draw(graphics, surface, x, y, pet, this.menu, mouseX, mouseY);
        backpackPage.draw(surface, x, y, this.menu);
        pickUpRow.draw(surface, x, y, pet.getRules(), mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // The banner and the row say what the default title and "Inventory" labels would.
    }

    // ---- what waits under the cursor ---------------------------------------------------

    private void renderNotes(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiSurface surface = new GuiSurface(graphics, this.font);
        List<String> lines = notes.at(mouseX, mouseY).orElse(List.of());
        if (!lines.isEmpty()) {
            surface.onTop(() -> Tooltip.draw(surface, lines, mouseX, mouseY, this.width, this.height));
        }
    }

    // ---- input -------------------------------------------------------------------------

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        // The pet standing over the panel's edge is not outside it: a click there is not a drop.
        return super.hasClickedOutside(mouseX, mouseY, left, top)
            && !PetScreenLayout.MODEL.offset(this.leftPos, this.topPos).contains((int) mouseX, (int) mouseY);
    }
}
