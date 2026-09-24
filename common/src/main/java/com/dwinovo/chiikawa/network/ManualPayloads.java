package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** What the server tells a player about the handbook. */
public final class ManualPayloads {
    private ManualPayloads() {
    }

    /** Opens the handbook. The pages are the player's own resources, so nothing goes with it. */
    public record OpenHandbookPayload() implements CustomPacketPayload {
        public static final OpenHandbookPayload INSTANCE = new OpenHandbookPayload();
        public static final Type<OpenHandbookPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "open_handbook"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenHandbookPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
