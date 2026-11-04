package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.client.ui.mc.GuiSurface;
import com.dwinovo.chiikawa.client.ui.mc.UiButton;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetRules;
import com.dwinovo.chiikawa.network.PetPayloads.PetRulesPayload;
import com.dwinovo.chiikawa.platform.Services;
import com.dwinovo.chiikawa.ui.PixelArt;
import com.dwinovo.chiikawa.ui.Rect;
import com.dwinovo.chiikawa.ui.Ui;
import com.dwinovo.chiikawa.ui.UiStyle;
import com.dwinovo.chiikawa.ui.UiTheme;
import com.dwinovo.chiikawa.ui.widget.Switch;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;

/**
 * The one thing an owner tells a pet from its screen: whether it picks up what lies on the ground, a row under the player's
 * hotbar with a picture, its name and the switch at the right, and what it does waiting under the cursor. The switch is drawn
 * here and pressed through a button laid over it, which keeps the game's own clicking, keyboard and narration.
 */
final class PickUpRow {
    private static final String NAME = "screen.chiikawa.pet.pick_up";

    private final HoverNotes notes;

    PickUpRow(HoverNotes notes) {
        this.notes = notes;
    }

    /** Where the switch is, from the panel's corner. */
    private static Rect switchArea() {
        Rect row = PetScreenLayout.PICK_UP_ROW;
        return new Rect(row.right() - Switch.WIDTH, UiStyle.centerIn(row.y(), row.height(), Switch.HEIGHT), Switch.WIDTH,
            Switch.HEIGHT);
    }

    /** The place the switch is pressed, over the one that is drawn there. */
    UiButton button(int left, int top, Supplier<AbstractPet> pet) {
        return UiButton.over(switchArea().offset(left, top), Component.translatable(NAME), () -> tell(pet.get()));
    }

    void draw(GuiSurface surface, int left, int top, PetRules rules, int mouseX, int mouseY) {
        Rect row = PetScreenLayout.PICK_UP_ROW.offset(left, top);
        PixelArt.PICK_UP.drawCentered(surface, row.x(), UiStyle.centerIn(row.y(), row.height(), UiStyle.ICON), UiStyle.ICON);
        Ui.textClipped(surface, Component.translatable(NAME).getString(), left + PetScreenLayout.PICK_UP_TEXT_X,
            UiStyle.centerIn(row.y(), row.height(), surface.lineHeight()), PetScreenLayout.PICK_UP_ROOM, UiTheme.TEXT);
        Rect toggle = switchArea().offset(left, top);
        Switch.draw(surface, toggle.x(), toggle.y(), rules.pickUp(), toggle.contains(mouseX, mouseY));
        notes.add(row, List.of(Component.translatable(NAME + ".hint").getString()));
    }

    /** Asks the server to turn picking up the other way. */
    private static void tell(AbstractPet pet) {
        if (pet != null) {
            PetRules now = pet.getRules();
            Services.NETWORK.sendToServer(new PetRulesPayload(pet.getId(), now.withPickUp(!now.pickUp())));
        }
    }
}
