package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.settings.WorldSettings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** What the world's settings say, and a request to change them. */
public final class SettingsPayloads {
    public static final ResourceLocation WORLD_SETTINGS = new ResourceLocation(Constants.MOD_ID, "world_settings");
    public static final ResourceLocation CHANGE_WORLD_SETTINGS = new ResourceLocation(Constants.MOD_ID, "change_world_settings");

    private SettingsPayloads() {
    }

    /**
     * The world's settings, sent to a player as they join and to everyone whenever they change.
     *
     * @param settings what they are
     * @param canChange whether the server will take a change from this player, which only the
     *                  server can say: whether somebody is an operator is not known to their game
     */
    public record WorldSettingsPayload(WorldSettings settings, boolean canChange) implements MusicPayloads.Payload {
        public static WorldSettingsPayload read(FriendlyByteBuf buffer) {
            return new WorldSettingsPayload(WorldSettings.read(buffer), buffer.readBoolean());
        }

        @Override
        public ResourceLocation id() {
            return WORLD_SETTINGS;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            settings.write(buffer);
            buffer.writeBoolean(canChange);
        }
    }

    /**
     * A player asking for the world's settings to be these. Whether they may is for the server
     * to say, and it says no to anyone but an operator.
     */
    public record ChangeWorldSettingsPayload(WorldSettings settings) implements MusicPayloads.Payload {
        public static ChangeWorldSettingsPayload read(FriendlyByteBuf buffer) {
            return new ChangeWorldSettingsPayload(WorldSettings.read(buffer));
        }

        @Override
        public ResourceLocation id() {
            return CHANGE_WORLD_SETTINGS;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            settings.write(buffer);
        }
    }
}
