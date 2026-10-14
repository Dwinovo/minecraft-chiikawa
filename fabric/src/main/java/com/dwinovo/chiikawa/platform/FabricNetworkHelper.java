package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.menu.PetBackpackMenu;
import com.dwinovo.chiikawa.network.MusicPayloads;
import com.dwinovo.chiikawa.platform.services.INetworkHelper;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class FabricNetworkHelper implements INetworkHelper {
    @Override
    public void sendToClient(ServerPlayer player, MusicPayloads.Payload payload) {
        FabricModNetworking.sendToClient(player, payload);
    }

    @Override
    public void sendToServer(MusicPayloads.Payload payload) {
        FabricModNetworking.sendToServer(payload);
    }

    @Override
    public void openPetMenu(ServerPlayer player, AbstractPet pet) {
        MenuProvider menu = PetBackpackMenu.providerFor(pet);
        player.openMenu(new ExtendedScreenHandlerFactory() {
            @Override
            public void writeScreenOpeningData(ServerPlayer opening, FriendlyByteBuf buf) {
                PetBackpackMenu.writeOpeningData(buf, pet.getId());
            }

            @Override
            public Component getDisplayName() {
                return menu.getDisplayName();
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player opening) {
                return menu.createMenu(containerId, inventory, opening);
            }
        });
    }

    @Override
    public boolean canReceive(ServerPlayer player, ResourceLocation id) {
        return ServerPlayNetworking.canSend(player, id);
    }
}
