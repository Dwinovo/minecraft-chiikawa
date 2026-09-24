package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** What the server tells the players nearby that a pet says. */
public final class VoicePayloads {
    private VoicePayloads() {
    }

    /**
     * A pet said something. The server picked the line, so everyone who hears it hears the
     * same one; each player reads it in their own language.
     *
     * @param pet the pet's entity id
     * @param line the translation key of what it said
     * @param ticks how long the line stays up over the pet's head
     */
    public record PetSpeechPayload(int pet, String line, int ticks) implements CustomPacketPayload {
        public static final Type<PetSpeechPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "pet_speech"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PetSpeechPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PetSpeechPayload::pet,
            ByteBufCodecs.STRING_UTF8, PetSpeechPayload::line,
            ByteBufCodecs.VAR_INT, PetSpeechPayload::ticks,
            PetSpeechPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
