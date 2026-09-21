package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.PetDirective;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** What an owner asks of a pet from its screen. */
public final class PetPayloads {
    public static final ResourceLocation PET_DIRECTIVE = new ResourceLocation(Constants.MOD_ID, "pet_directive");

    private PetPayloads() {
    }

    /**
     * An order picked on the pet screen: follow, sit, or get on with things.
     *
     * @param pet the pet's entity id; the server checks it is the sender's and within reach
     * @param directive the order
     */
    public record PetDirectivePayload(int pet, PetDirective directive) implements MusicPayloads.Payload {
        public static PetDirectivePayload read(FriendlyByteBuf buffer) {
            return new PetDirectivePayload(buffer.readVarInt(), PetDirective.fromId(buffer.readByte()));
        }

        @Override
        public ResourceLocation id() {
            return PET_DIRECTIVE;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(pet);
            buffer.writeByte(directive.ordinal());
        }
    }
}
