package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.PetDirective;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** What an owner asks of a pet from its screen. */
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
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "pet_directive"));
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
}
