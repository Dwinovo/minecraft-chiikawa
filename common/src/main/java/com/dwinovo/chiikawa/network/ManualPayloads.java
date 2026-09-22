package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** What the server tells a player about the handbook. */
public final class ManualPayloads {
    public static final ResourceLocation OPEN_HANDBOOK = new ResourceLocation(Constants.MOD_ID, "open_handbook");

    private ManualPayloads() {
    }

    /** Opens the handbook. The pages are the player's own resources, so nothing goes with it. */
    public record OpenHandbookPayload() implements MusicPayloads.Payload {
        public static final OpenHandbookPayload INSTANCE = new OpenHandbookPayload();

        public static OpenHandbookPayload read(FriendlyByteBuf buffer) {
            return INSTANCE;
        }

        @Override
        public ResourceLocation id() {
            return OPEN_HANDBOOK;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
        }
    }
}
