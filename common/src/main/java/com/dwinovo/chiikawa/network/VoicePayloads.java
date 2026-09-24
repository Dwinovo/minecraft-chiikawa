package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** What the server tells the players nearby that a pet says. */
public final class VoicePayloads {
    public static final ResourceLocation PET_SPEECH = new ResourceLocation(Constants.MOD_ID, "pet_speech");

    private VoicePayloads() {
    }

    /**
     * A pet said something. The server picked the line, so everyone who hears it hears the
     * same one; each player reads it in their own language.
     *
     * @param pet the pet's entity id
     * @param line the translation key of what it said
     */
    public record PetSpeechPayload(int pet, String line) implements MusicPayloads.Payload {
        public static PetSpeechPayload read(FriendlyByteBuf buffer) {
            return new PetSpeechPayload(buffer.readVarInt(), buffer.readUtf());
        }

        @Override
        public ResourceLocation id() {
            return PET_SPEECH;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(pet);
            buffer.writeUtf(line);
        }
    }
}
