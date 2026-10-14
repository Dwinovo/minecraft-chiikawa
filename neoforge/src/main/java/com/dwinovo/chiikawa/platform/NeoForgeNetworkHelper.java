package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.menu.PetBackpackMenu;
import com.dwinovo.chiikawa.platform.services.INetworkHelper;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.payload.AdvancedOpenScreenPayload;

public final class NeoForgeNetworkHelper implements INetworkHelper {
    @Override
    public void sendToClient(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    @Override
    public void openPetMenu(ServerPlayer player, AbstractPet pet) {
        // The stand-in player an in-game test makes has no game to open a screen on, and the loader
        // refuses to send the opening to it.
        if (!canReceive(player, AdvancedOpenScreenPayload.TYPE)) {
            return;
        }
        player.openMenu(PetBackpackMenu.providerFor(pet), buf -> PetBackpackMenu.OPENING_DATA.encode(buf, pet.getId()));
    }

    @Override
    public boolean canReceive(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return player.connection.hasChannel(type);
    }
}
