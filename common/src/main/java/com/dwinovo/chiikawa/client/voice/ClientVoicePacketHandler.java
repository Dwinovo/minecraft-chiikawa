package com.dwinovo.chiikawa.client.voice;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.network.VoicePayloads.PetSpeechPayload;
import net.minecraft.client.Minecraft;

/** Has the pet the server named say its line on this player's game. */
public final class ClientVoicePacketHandler {
    private ClientVoicePacketHandler() {
    }

    public static void handleSpeech(PetSpeechPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getEntity(payload.pet()) instanceof AbstractPet pet) {
            pet.speak(payload.line());
        }
    }
}
