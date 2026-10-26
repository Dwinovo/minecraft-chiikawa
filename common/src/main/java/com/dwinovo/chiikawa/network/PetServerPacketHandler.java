package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.PetRules;
import net.minecraft.server.level.ServerPlayer;

/**
 * Orders and rules given from the pet screen. The screen is a picture of the pet a moment ago, and
 * anybody's client can send anything, so who is asking and from how far is checked here
 * before the pet hears it.
 */
public final class PetServerPacketHandler {
    /** How far from its pet an owner may stand and still be heard — the menu's own reach. */
    private static final double REACH_SQR = 64.0;

    private PetServerPacketHandler() {
    }

    public static void handleDirective(PetPayloads.PetDirectivePayload payload, ServerPlayer player) {
        if (player.level().getEntity(payload.pet()) instanceof AbstractPet pet) {
            order(player, pet, payload.directive());
        }
    }

    /**
     * Gives a pet an order, if it is the player's and the player is near enough to be heard.
     *
     * @return whether the pet took it
     */
    public static boolean order(ServerPlayer player, AbstractPet pet, PetDirective directive) {
        if (!mayAsk(player, pet)) {
            return false;
        }
        pet.setPetDirective(directive);
        return true;
    }

    public static void handleRules(PetPayloads.PetRulesPayload payload, ServerPlayer player) {
        if (player.level().getEntity(payload.pet()) instanceof AbstractPet pet) {
            rule(player, pet, payload.rules());
        }
    }

    /**
     * Tells a pet what it may and may not do, if it is the player's and the player is near enough to be heard. The
     * owner's alone, not their friends'.
     *
     * @return whether the pet took it
     */
    public static boolean rule(ServerPlayer player, AbstractPet pet, PetRules rules) {
        if (!mayAsk(player, pet)) {
            return false;
        }
        pet.setRules(rules);
        return true;
    }

    private static boolean mayAsk(ServerPlayer player, AbstractPet pet) {
        return pet.isAlive() && player.getUUID().equals(pet.getOwnerUUID()) && player.distanceToSqr(pet) <= REACH_SQR;
    }
}
