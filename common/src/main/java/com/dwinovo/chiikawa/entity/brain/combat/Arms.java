package com.dwinovo.chiikawa.entity.brain.combat;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitTag;
import com.dwinovo.chiikawa.utils.Utils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

/**
 * What a pet has to fight with: whether the weapon in its hand is one it can use, and the one in its bag that it
 * could take up in its place, from any pocket but the one for the bag it wears: what the owner puts in the bag is the owner's
 * word on it. A weapon is a
 * fencer's tool, or an archer's with arrows to loose; a bow without an arrow is no weapon. The rule is pure, over what
 * each pocket holds, and {@link #of} reads a pet.
 *
 * @param wielding whether the pet holds a weapon it can use
 * @param swap the weapon in the bag it may take up, none if there is none
 */
public record Arms(boolean wielding, Optional<Weapon> swap) {
    /** A pet with nothing to fight with. */
    public static final Arms NONE = new Arms(false, Optional.empty());

    /** What a thing a pet carries is as a weapon. */
    public enum Kind {
        SWORD,
        BOW,
        OTHER
    }

    /**
     * @param slot the pocket of the pet's backpack that holds it
     * @param bow whether it is fought with at a distance
     */
    public record Weapon(int slot, boolean bow) {
    }

    /**
     * @param hand what is in the pet's hand
     * @param backpack what is in each pocket of its backpack, the hand's included, by pocket
     * @param arrows whether it has arrows to loose
     * @return what it has to fight with
     */
    public static Arms choose(Kind hand, List<Kind> backpack, boolean arrows) {
        Optional<Weapon> swap = Optional.empty();
        for (int slot = 0; slot < backpack.size() && swap.isEmpty(); slot++) {
            if (slot != AbstractPet.MAINHAND_SLOT && slot != AbstractPet.BAG_SLOT && usable(backpack.get(slot), arrows)) {
                swap = Optional.of(new Weapon(slot, backpack.get(slot) == Kind.BOW));
            }
        }
        return new Arms(usable(hand, arrows), swap);
    }

    /** @return what this pet has to fight with, by the tools its job takes and what is in its bag */
    public static Arms of(AbstractPet pet) {
        List<Kind> backpack = new ArrayList<>(pet.getBackpack().getContainerSize());
        for (int slot = 0; slot < pet.getBackpack().getContainerSize(); slot++) {
            backpack.add(kindOf(pet.getBackpack().getItem(slot)));
        }
        return choose(backpack.get(AbstractPet.MAINHAND_SLOT), backpack, !Utils.getArrow(pet).isEmpty());
    }

    /** @return whether a pet can fight with a thing of this kind: a sword always, a bow if it has an arrow */
    private static boolean usable(Kind kind, boolean arrows) {
        return kind == Kind.SWORD || (kind == Kind.BOW && arrows);
    }

    private static Kind kindOf(ItemStack stack) {
        if (stack.is(InitTag.ENTITY_FENCER_TOOLS)) {
            return Kind.SWORD;
        }
        return stack.is(InitTag.ENTITY_ARCHER_TOOLS) ? Kind.BOW : Kind.OTHER;
    }
}
