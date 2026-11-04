package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.client.ui.mc.GuiSurface;
import com.dwinovo.chiikawa.menu.PetBackpackMenu;
import com.dwinovo.chiikawa.ui.PixelArt;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import com.dwinovo.chiikawa.ui.widget.DashedWell;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

/**
 * What the pet screen draws behind its slots: the pet's own in a card, its hand and ten on top and its bag and the bag's
 * ten under a dashed line, and the player's beside it. The slots are the menu's, and so is where they are: a well is drawn
 * behind each one by asking the slot, so a slot moved is its well moved. The bag's ten, with no bag worn, are dashed wells
 * where the slots will be, and the bag's own is a shadow of one.
 */
final class BackpackPage {
    private final HoverNotes notes;

    BackpackPage(HoverNotes notes) {
        this.notes = notes;
    }

    void draw(GuiSurface surface, int left, int top, PetBackpackMenu menu) {
        Rect card = PetScreenLayout.PET_CARD.offset(left, top);
        Ui.sticker(surface, card.x(), card.y(), card.width(), card.height(), Ui.CARD_RADIUS, UiTheme.ACCENT_PALE);
        // The line across the card says the bag's pockets are another thing from the pet's own; it is kept in from the edges
        // by the card's own padding.
        Ui.divider(surface, card.x() - UiStyle.GAP, top + PetScreenLayout.CARD_DIVIDER_Y, card.width() + 2 * UiStyle.GAP);

        Rect empty = null;
        for (Slot slot : menu.slots) {
            Rect well = new Rect(left + slot.x - 1, top + slot.y - 1, UiStyle.SLOT, UiStyle.SLOT);
            if (slot.isActive()) {
                Ui.well(surface, well.x(), well.y(), well.width(), well.height());
                if (menu.isBagSlot(slot) && !slot.hasItem()) {
                    PixelArt.BAG.drawCentered(surface, well.x(), well.y(), UiStyle.SLOT);
                }
            } else if (menu.isBagContent(slot)) {
                DashedWell.draw(surface, well.x(), well.y(), well.width(), well.height());
                empty = empty == null ? well : union(empty, well);
            }
        }
        if (empty != null) {
            notes.add(empty, List.of(Component.translatable("screen.chiikawa.pet.bag_hint").getString()));
        }
    }

    private static Rect union(Rect first, Rect second) {
        int x = Math.min(first.x(), second.x());
        int y = Math.min(first.y(), second.y());
        return new Rect(x, y, Math.max(first.right(), second.right()) - x, Math.max(first.bottom(), second.bottom()) - y);
    }
}
