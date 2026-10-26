package com.dwinovo.chiikawa.entity;

import net.minecraft.world.entity.player.Player;

/**
 * Who may tell a pet what to do, other than its owner: its owner's friends, as the game counts them, which is the players
 * in the same team. The pet's own {@code isAlliedTo} says so, since a tamed animal takes the team of its owner, and it
 * says so only while the owner is in the pet's level, so a friend can only command a pet whose owner is around.
 *
 * <p>A friend may change what the pet is told to do in general, whistle for it, feed it, give it a dish and pay it. What is
 * the owner's alone stays so: the pet's books and bag, the rules, and the present it carries.
 */
public final class PetFriends {
    private PetFriends() {
    }

    /**
     * @param pet the pet
     * @param player whoever asks
     * @return whether {@code player} may tell {@code pet} what to do in general
     */
    public static boolean mayCommand(AbstractPet pet, Player player) {
        return pet.isOwnedBy(player) || pet.isTame() && pet.isAlliedTo(player);
    }
}
