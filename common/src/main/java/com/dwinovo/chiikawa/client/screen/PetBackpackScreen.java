package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.client.ui.mc.GuiSurface;
import com.dwinovo.chiikawa.client.ui.mc.ItemIcon;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.menu.PetBackpackMenu;
import com.dwinovo.chiikawa.ui.PixelArt;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import com.dwinovo.chiikawa.ui.widget.Banner;
import com.dwinovo.chiikawa.ui.widget.TabStrip;
import com.dwinovo.chiikawa.ui.widget.Tooltip;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * A pet's screen, in two pages behind two tabs: what it carries, and what it may do.
 *
 * <p>Drawn after Chiikawa Pocket — paper stickers in an ink line, colour only where it means something — and nothing on it is
 * a texture. Over both pages is the same banner, with the pet in it standing on its floor and rising over the panel's top
 * edge, so the pet, its name, its health and its order are in front of the hand whichever page it is on; what differs is
 * what is under the banner. The panel is as big on both, and the tabs stand on its top edge at its right end.
 */
public class PetBackpackScreen extends AbstractContainerScreen<PetBackpackMenu> {
    private static final ItemStack BAG_ICON_STACK = new ItemStack(InitItems.BACKPACK.get());

    /** The pages, in tab order. */
    private enum Page {
        BACKPACK("screen.chiikawa.pet.tab.backpack"),
        SETTINGS("screen.chiikawa.pet.tab.settings");

        private final String key;

        Page(String key) {
            this.key = key;
        }
    }

    private final HoverNotes notes = new HoverNotes();
    private final PetHeader header = new PetHeader(notes);
    private final BackpackPage backpackPage = new BackpackPage(notes);
    private final SettingsPage settingsPage = new SettingsPage();
    private Page page = Page.BACKPACK;
    private TabStrip tabs;

    public PetBackpackScreen(PetBackpackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PetScreenLayout.PANEL_W;
        this.imageHeight = PetScreenLayout.PANEL_H;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = PetScreenLayout.left(this.width);
        this.topPos = PetScreenLayout.top(this.height);
        this.tabs = new TabStrip(new Rect(this.leftPos, this.topPos, this.imageWidth, this.imageHeight), List.of(
            new TabStrip.Tab((surface, x, y) -> surface.drawIcon(new ItemIcon(BAG_ICON_STACK), x, y),
                Component.translatable(Page.BACKPACK.key).getString()),
            new TabStrip.Tab((surface, x, y) -> PixelArt.GEAR.drawCentered(surface, x, y, UiStyle.ICON),
                Component.translatable(Page.SETTINGS.key).getString())), this.font::width);
        showPage(page);
    }

    private void showPage(Page shown) {
        this.page = shown;
        this.menu.showSlots(shown == Page.BACKPACK);
        clearWidgets();
        header.buttons(this.leftPos, this.topPos, this.font::width, this::pet).forEach(this::addRenderableWidget);
        if (shown == Page.SETTINGS) {
            settingsPage.buttons(this.leftPos, this.topPos, this.font::width, this::pet).forEach(this::addRenderableWidget);
        }
    }

    private AbstractPet pet() {
        return this.menu.getPet(Minecraft.getInstance().level);
    }

    // ---- drawing -------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
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
        tabs.drawBehind(surface, page.ordinal());
        Ui.card(surface, x, y, this.imageWidth, this.imageHeight);
        Banner.draw(surface, x + 2, y + 2, this.imageWidth - 4, PetScreenLayout.BANNER_H - 2, UiTheme.ACCENT_PALE);
        tabs.drawFront(surface, page.ordinal(), UiTheme.ACCENT_PALE);

        AbstractPet pet = pet();
        if (pet == null) {
            return;
        }
        header.draw(graphics, surface, x, y, pet, this.menu, mouseX, mouseY);
        switch (page) {
            case BACKPACK -> backpackPage.draw(surface, x, y, this.menu);
            case SETTINGS -> settingsPage.draw(surface, x, y, pet.getRules(), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // The banner and the pages say what the default title and "Inventory" labels would.
    }

    // ---- what waits under the cursor ---------------------------------------------------

    private void renderNotes(GuiGraphics graphics, int mouseX, int mouseY) {
        GuiSurface surface = new GuiSurface(graphics, this.font);
        int tab = tabs.at(mouseX, mouseY);
        List<String> lines = tab >= 0
            ? List.of(Component.translatable(Page.values()[tab].key).getString())
            : notes.at(mouseX, mouseY).orElse(List.of());
        if (!lines.isEmpty()) {
            surface.onTop(() -> Tooltip.draw(surface, lines, mouseX, mouseY, this.width, this.height));
        }
    }

    // ---- input -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int tab = tabs.at((int) mouseX, (int) mouseY);
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
        // The tabs, and the pet standing over the panel's edge, are not outside it: a click there is not a drop.
        return super.hasClickedOutside(mouseX, mouseY, left, top, button)
            && tabs.at((int) mouseX, (int) mouseY) < 0
            && !PetScreenLayout.MODEL.offset(this.leftPos, this.topPos).contains((int) mouseX, (int) mouseY);
    }
}
