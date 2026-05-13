package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.music.MusicBoxSelection;
import com.dwinovo.chiikawa.music.ServerMusicLibrary;
import com.dwinovo.chiikawa.music.ServerMusicSystem;
import com.dwinovo.chiikawa.network.MusicPayloads.MusicCatalogPayload;
import com.dwinovo.chiikawa.network.MusicPayloads.MusicCatalogRequestPayload;
import com.dwinovo.chiikawa.network.MusicPayloads.MusicBoxSelectTrackPayload;
import com.dwinovo.chiikawa.platform.Services;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class MusicServerPacketHandler {
    private MusicServerPacketHandler() {
    }

    public static void handleSelectTrack(MusicBoxSelectTrackPayload payload, ServerPlayer player) {
        ItemStack stack = musicBoxStack(player, payload.handIndex());
        if (!stack.is(InitItems.MUSIC_BOX.get())) {
            return;
        }
        ServerMusicSystem.library(player.level().getServer()).getTrack(payload.trackId()).ifPresent(track -> {
            MusicBoxSelection current = MusicBoxSelection.get(stack);
            int revision = current == null ? 1 : current.revision() + 1;
            stack.set(com.dwinovo.chiikawa.init.InitDataComponents.MUSIC_BOX_SELECTION.get(),
                new MusicBoxSelection(track.trackId(), track.title(), revision));
        });
    }

    public static void handleCatalogRequest(MusicCatalogRequestPayload payload, ServerPlayer player) {
        ItemStack stack = musicBoxStack(player, payload.handIndex());
        if (!stack.is(InitItems.MUSIC_BOX.get())) {
            return;
        }
        ServerMusicLibrary library = ServerMusicSystem.library(player.level().getServer());
        if (payload.rescan()) {
            library.rescan();
        }
        Services.NETWORK.sendToClient(player, new MusicCatalogPayload(payload.handIndex(), library.catalog(), false));
    }

    private static ItemStack musicBoxStack(ServerPlayer player, int handIndex) {
        InteractionHand hand = handIndex == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        return player.getItemInHand(hand);
    }
}
