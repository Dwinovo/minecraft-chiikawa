package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.PetRules;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** What an owner asks of a pet from its screen, and what the players who can see a pet are told it does. */
public final class PetPayloads {
    private PetPayloads() {
    }

    /**
     * An order picked on the pet screen: follow, sit, or get on with things.
     *
     * @param pet the pet's entity id; the server checks it is the sender's and within reach
     * @param directive the order
     */
    public record PetDirectivePayload(int pet, PetDirective directive) implements CustomPacketPayload {
        public static final Type<PetDirectivePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "pet_directive"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PetDirectivePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeVarInt(value.pet);
                buffer.writeByte(value.directive.ordinal());
            },
            buffer -> new PetDirectivePayload(buffer.readVarInt(), PetDirective.fromId(buffer.readByte()))
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * What an owner has picked on the pet screen that the pet may and may not do.
     *
     * @param pet the pet's entity id; the server checks it is the sender's and within reach
     * @param rules all of them, as the screen now shows them
     */
    public record PetRulesPayload(int pet, PetRules rules) implements CustomPacketPayload {
        public static final Type<PetRulesPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "pet_rules"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PetRulesPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PetRulesPayload::pet,
            PetRules.STREAM_CODEC, PetRulesPayload::rules,
            PetRulesPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * A pet made a move once, one named by data rather than by code — a clap as it listens
     * to music, say. A player's game plays it if the pet has an animation by that name, and
     * otherwise shows nothing.
     *
     * @param pet the pet's entity id
     * @param animation the move's animation name
     */
    public record PetGesturePayload(int pet, String animation) implements CustomPacketPayload {
        public static final Type<PetGesturePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "pet_gesture"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PetGesturePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PetGesturePayload::pet,
            ByteBufCodecs.STRING_UTF8, PetGesturePayload::animation,
            PetGesturePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
