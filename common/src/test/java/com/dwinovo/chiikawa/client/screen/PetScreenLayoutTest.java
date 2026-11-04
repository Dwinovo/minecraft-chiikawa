package com.dwinovo.chiikawa.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.menu.PetBackpackMenu;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.widget.Hearts;
import com.dwinovo.chiikawa.ui.widget.Segments;
import org.junit.jupiter.api.Test;

/** The pet screen's layout is one drawing: the slots the menu places sit in the cards the screen draws round them. */
class PetScreenLayoutTest {
    private static Rect well(int slotX, int slotY) {
        return new Rect(slotX - 1, slotY - 1, UiStyle.SLOT, UiStyle.SLOT);
    }

    private static boolean inside(Rect outer, Rect inner) {
        return inner.x() >= outer.x() && inner.y() >= outer.y() && inner.right() <= outer.right() && inner.bottom() <= outer.bottom();
    }

    @Test
    void thePetsOwnSlotsAreInItsCardTheHandAndTenAboveTheLineAndTheBagAndItsTenUnderIt() {
        Rect card = PetScreenLayout.PET_CARD;
        int pitch = PetBackpackMenu.SLOT_PITCH;
        Rect hand = well(PetBackpackMenu.MAINHAND_X, PetBackpackMenu.MAINHAND_Y);
        Rect bag = well(PetBackpackMenu.BAG_X, PetBackpackMenu.BAG_Y);
        Rect ten = new Rect(PetBackpackMenu.PET_GRID_X - 1, PetBackpackMenu.PET_GRID_Y - 1,
            PetBackpackMenu.GRID_COLUMNS * pitch, 2 * pitch);
        Rect bagTen = new Rect(PetBackpackMenu.PET_GRID_X - 1, PetBackpackMenu.BAG_ROWS_Y - 1,
            PetBackpackMenu.GRID_COLUMNS * pitch, 2 * pitch);

        for (Rect part : new Rect[] {hand, bag, ten, bagTen}) {
            assertTrue(inside(card, part), part + " is not in " + card);
        }
        assertTrue(ten.bottom() < PetScreenLayout.CARD_DIVIDER_Y && PetScreenLayout.CARD_DIVIDER_Y < bagTen.y(),
            "the line across the card is not between the pet's own and the bag's");
        assertTrue(hand.right() <= ten.x() && bag.right() <= bagTen.x(), "the hand or the bag is not to the left of its ten");
    }

    @Test
    void theHandIsLevelWithTheMiddleOfTheTenAndTheBagWithTheMiddleOfTheBagsTen() {
        int pitch = PetBackpackMenu.SLOT_PITCH;
        int middleOfTen = PetBackpackMenu.PET_GRID_Y - 1 + pitch;
        int middleOfBagsTen = PetBackpackMenu.BAG_ROWS_Y - 1 + pitch;

        assertEquals(middleOfTen, PetBackpackMenu.MAINHAND_Y - 1 + UiStyle.SLOT / 2);
        assertEquals(middleOfBagsTen, PetBackpackMenu.BAG_Y - 1 + UiStyle.SLOT / 2);
    }

    @Test
    void thePlayersSlotsAreBesideTheCardAndInsideThePanelWithTheHotbarLevelWithTheBagsSecondRow() {
        int pitch = PetBackpackMenu.SLOT_PITCH;
        Rect panel = new Rect(0, 0, PetScreenLayout.PANEL_W, PetScreenLayout.PANEL_H);
        Rect inventory = new Rect(PetBackpackMenu.PLAYER_INV_X - 1, PetBackpackMenu.PLAYER_INV_Y - 1, 9 * pitch, 3 * pitch);
        Rect hotbar = new Rect(PetBackpackMenu.PLAYER_INV_X - 1, PetBackpackMenu.HOTBAR_Y - 1, 9 * pitch, pitch);

        assertTrue(inside(panel, inventory) && inside(panel, hotbar));
        assertTrue(inventory.x() >= PetScreenLayout.PET_CARD.right(), "the player's slots run under the pet's card");
        assertTrue(inventory.y() >= PetScreenLayout.BANNER_H, "the player's slots run under the banner");
        assertEquals(PetBackpackMenu.BAG_ROWS_Y + pitch, PetBackpackMenu.HOTBAR_Y, "the hotbar is not level with the bag's second row");
        assertTrue(inventory.bottom() < hotbar.y(), "no air between the inventory and the hotbar");
        assertTrue(hotbar.right() <= PetScreenLayout.RIGHT, "the player's slots pass the banner's right edge");
    }

    @Test
    void theCardsAreUnderTheBannerAndInsideThePanel() {
        Rect panel = new Rect(0, 0, PetScreenLayout.PANEL_W, PetScreenLayout.PANEL_H);

        for (Rect card : new Rect[] {PetScreenLayout.PET_CARD, PetScreenLayout.SETTINGS_CARD}) {
            assertTrue(inside(panel, card) && card.y() > PetScreenLayout.BANNER_H, card + " is not under the banner in the panel");
        }
        assertEquals(PetScreenLayout.PET_CARD.y(), PetScreenLayout.SETTINGS_CARD.y());
        assertEquals(PetScreenLayout.PET_CARD.bottom(), PetScreenLayout.SETTINGS_CARD.bottom());
    }

    @Test
    void theBannersPartsDoNotRunIntoEachOther() {
        Rect hearts = new Rect(PetScreenLayout.TEXT_X, PetScreenLayout.HEARTS_Y, Hearts.width(20), Hearts.SIZE);
        Rect bubble = PetScreenLayout.bubble(true);
        Rect gift = new Rect(PetScreenLayout.RIGHT - PetScreenLayout.GIFT_W, PetScreenLayout.GIFT_Y, PetScreenLayout.GIFT_W, 18);

        assertTrue(hearts.right() < PetScreenLayout.ORDERS.x(), "the hearts run into the orders");
        assertEquals(PetScreenLayout.RIGHT, PetScreenLayout.ORDERS.right());
        assertTrue(bubble.right() < gift.x(), "the bubble runs into the badge for a present");
        assertEquals(PetScreenLayout.RIGHT, PetScreenLayout.bubble(false).right());
        assertTrue(PetScreenLayout.ORDERS.bottom() < PetScreenLayout.BUBBLE_Y, "the orders run into the bubble");
        assertTrue(PetScreenLayout.BUBBLE_Y + PetScreenLayout.BUBBLE_H < PetScreenLayout.BANNER_H - 1, "the bubble is on the banner's line");
        assertTrue(PetScreenLayout.MODEL.right() < PetScreenLayout.BUBBLE_X - 3, "the pet runs into the bubble's tail");
        assertTrue(PetScreenLayout.HEARTS_Y + Hearts.SIZE <= PetScreenLayout.BUBBLE_Y);
        assertEquals(Segments.HEIGHT, PetScreenLayout.ORDERS.height());
    }

    @Test
    void thePetStandsOnTheBannersFloorAndRisesOverThePanelsEdgeByNoMoreThanTheRoomForIt() {
        Rect model = PetScreenLayout.MODEL;

        assertEquals(-PetScreenLayout.RISE, model.y());
        assertTrue(model.bottom() > PetScreenLayout.FLOOR_Y && model.bottom() < PetScreenLayout.BANNER_H);
        assertTrue(PetScreenLayout.SHADOW.bottom() <= model.bottom() + 1);
    }

    @Test
    void theSettingsRowsAndTheLineBetweenThemFillTheCard() {
        Rect card = PetScreenLayout.SETTINGS_CARD;

        assertEquals(card.y() + 1, PetScreenLayout.FIRST_ROW_Y);
        assertEquals(PetScreenLayout.FIRST_ROW_Y + PetScreenLayout.ROW_H, PetScreenLayout.SETTINGS_DIVIDER_Y);
        assertTrue(PetScreenLayout.SECOND_ROW_Y + PetScreenLayout.ROW_H <= card.bottom() - 1);
        assertTrue(PetScreenLayout.SWITCH_ROW_ROOM > 0 && PetScreenLayout.STANCE_ROW_ROOM > 0);
    }
}
