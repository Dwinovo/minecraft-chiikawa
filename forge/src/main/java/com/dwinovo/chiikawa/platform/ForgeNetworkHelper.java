package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.menu.PetBackpackMenu;
import com.dwinovo.chiikawa.network.MusicPayloads;
import com.dwinovo.chiikawa.platform.services.INetworkHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkHooks;

public final class ForgeNetworkHelper implements INetworkHelper {
    @Override
    public void sendToClient(ServerPlayer player, MusicPayloads.Payload payload) {
        ForgeModNetworking.sendToClient(player, payload);
    }

    @Override
    public void sendToServer(MusicPayloads.Payload payload) {
        ForgeModNetworking.sendToServer(payload);
    }

    @Override
    public void openPetMenu(ServerPlayer player, AbstractPet pet) {
        NetworkHooks.openScreen(player, PetBackpackMenu.providerFor(pet), buf -> PetBackpackMenu.writeOpeningData(buf, pet.getId()));
    }

    @Override
    public boolean canReceive(ServerPlayer player, ResourceLocation id) {
        // Every payload goes on the one channel, so a game that takes the channel takes them all.
        return ForgeModNetworking.canReceive(player);
    }
}
