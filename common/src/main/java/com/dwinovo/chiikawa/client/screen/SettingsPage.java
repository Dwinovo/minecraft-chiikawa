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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;
import net.minecraft.network.chat.Component;

/**
 * The settings page: one card of one row, what the owner has told the pet it may do. Each row is a picture, what it is in a
 * line and what it does in a quieter one, and the control that sets it at the right. The controls are drawn here and pressed
 * through buttons laid over them, which keep the game's own clicking, keyboard and narration.
 */
final class SettingsPage {
    /** The air between a row's title and the line under it. */
    private static final int LINE_GAP = UiStyle.TIGHT;

    /** Where the switch is, from the panel's corner. */
    private static Rect switchArea() {
        return new Rect(PetScreenLayout.ROW_CONTROL_RIGHT - Switch.WIDTH,
            UiStyle.centerIn(PetScreenLayout.FIRST_ROW_Y, PetScreenLayout.ROW_H, Switch.HEIGHT), Switch.WIDTH, Switch.HEIGHT);
    }

    /** The places the controls are pressed. */
    List<UiButton> buttons(int left, int top, ToIntFunction<String> textWidth, Supplier<AbstractPet> pet) {
        List<UiButton> buttons = new ArrayList<>();
        buttons.add(UiButton.over(switchArea().offset(left, top), Component.translatable("screen.chiikawa.pet.pick_up"),
            () -> tell(pet.get(), rules -> rules.withPickUp(!rules.pickUp()))));
        return buttons;
    }

    void draw(GuiSurface surface, int left, int top, PetRules rules, int mouseX, int mouseY) {
        Rect card = PetScreenLayout.SETTINGS_CARD.offset(left, top);
        Ui.sticker(surface, card.x(), card.y(), card.width(), card.height(), Ui.CARD_RADIUS, UiTheme.PANEL);

        int firstY = top + PetScreenLayout.FIRST_ROW_Y;
        PixelArt.PICK_UP.drawCentered(surface, left + PetScreenLayout.ROW_ICON_X,
            UiStyle.centerIn(firstY, PetScreenLayout.ROW_H, UiStyle.ICON), UiStyle.ICON);
        words(surface, left, firstY, PetScreenLayout.SWITCH_ROW_ROOM, Component.translatable("screen.chiikawa.pet.pick_up").getString(),
            Component.translatable("screen.chiikawa.pet.pick_up.hint").getString());
        Rect toggle = switchArea().offset(left, top);
        Switch.draw(surface, toggle.x(), toggle.y(), rules.pickUp(), toggle.contains(mouseX, mouseY));
    }

    /** A row's title and under it the line that says what it does, the pair in the middle of the row's height. */
    private static void words(GuiSurface surface, int left, int rowY, int room, String title, String summary) {
        int height = 2 * surface.lineHeight() + LINE_GAP;
        int y = UiStyle.centerIn(rowY, PetScreenLayout.ROW_H, height);
        Ui.textClipped(surface, title, left + PetScreenLayout.ROW_TEXT_X, y, room, UiTheme.TEXT);
        Ui.textClipped(surface, summary, left + PetScreenLayout.ROW_TEXT_X, y + surface.lineHeight() + LINE_GAP, room,
            UiTheme.TEXT_MUTED);
    }

    /** Asks the server to change the rules, if the change is one. */
    private static void tell(AbstractPet pet, UnaryOperator<PetRules> change) {
        if (pet != null) {
            PetRules now = pet.getRules();
            PetRules next = change.apply(now);
            if (!next.equals(now)) {
                Services.NETWORK.sendToServer(new PetRulesPayload(pet.getId(), next));
            }
        }
    }
}
