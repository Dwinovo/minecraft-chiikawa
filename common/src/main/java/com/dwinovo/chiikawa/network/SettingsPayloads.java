package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.settings.WorldSettings;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** What the world's settings say, and a request to change them. */
public final class SettingsPayloads {
    private SettingsPayloads() {
    }

    /**
     * The world's settings, sent to a player as they join and to everyone whenever they change.
     *
     * @param settings what they are
     * @param canChange whether the server will take a change from this player, which only the
     *                  server can say: whether somebody is an operator is not known to their game
     */
    public record WorldSettingsPayload(WorldSettings settings, boolean canChange) implements CustomPacketPayload {
        public static final Type<WorldSettingsPayload> TYPE = new Type<>(
            new ResourceLocation(Constants.MOD_ID, "world_settings"));
        public static final StreamCodec<RegistryFriendlyByteBuf, WorldSettingsPayload> STREAM_CODEC =
            StreamCodec.composite(
                WorldSettings.STREAM_CODEC, WorldSettingsPayload::settings,
                ByteBufCodecs.BOOL, WorldSettingsPayload::canChange,
                WorldSettingsPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * A player asking for the world's settings to be these. Whether they may is for the server
     * to say, and it says no to anyone but an operator.
     */
    public record ChangeWorldSettingsPayload(WorldSettings settings) implements CustomPacketPayload {
        public static final Type<ChangeWorldSettingsPayload> TYPE = new Type<>(
            new ResourceLocation(Constants.MOD_ID, "change_world_settings"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ChangeWorldSettingsPayload> STREAM_CODEC =
            StreamCodec.composite(
                WorldSettings.STREAM_CODEC, ChangeWorldSettingsPayload::settings,
                ChangeWorldSettingsPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
