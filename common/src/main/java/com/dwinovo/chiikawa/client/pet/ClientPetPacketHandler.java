package com.dwinovo.chiikawa.client.pet;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.menu.PetBackpackMenu;
import com.dwinovo.chiikawa.network.PetPayloads.PetGesturePayload;
import com.dwinovo.chiikawa.network.PetPayloads.PetOutlookPayload;
import java.util.List;
import net.minecraft.client.Minecraft;

/** Has the pet the server named make its move on this player's game, and tells its screen what it is up to. */
public final class ClientPetPacketHandler {
    private ClientPetPacketHandler() {
    }

    public static void handleGesture(PetGesturePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getEntity(payload.pet()) instanceof AbstractPet pet) {
            // A pet without the move shows nothing: the data names moves some pets lack.
            pet.playAnimation(List.of(payload.animation()));
        }
    }

    /** Gives the pet screen that is open what the server said of its pet. */
    public static void handleOutlook(PetOutlookPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.containerMenu instanceof PetBackpackMenu menu) {
            menu.receive(payload);
        }
    }
}
