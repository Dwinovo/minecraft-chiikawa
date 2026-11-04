package com.dwinovo.chiikawa.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.testing.ItemComponents;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Where a pet puts things: its own pockets, and the bag's only while it wears one, never the hand or the bag's slot. */
class PetPocketsTest {
    private static final int FIRST_POCKET = AbstractPet.BAG_SLOT + 1;
    /** An emerald's stack size, which the game only knows once the items have their components. */
    private static int stackSize;
    private static final int POCKETS = AbstractPet.BACKPACK_SIZE - FIRST_POCKET;

    private final SimpleContainer backpack = new SimpleContainer(AbstractPet.FULL_BACKPACK_SIZE);
    private boolean wearingBag;
    private final PetPockets pockets = new PetPockets(backpack, () -> wearingBag);

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        ItemComponents.bind();
        stackSize = Items.EMERALD.getDefaultMaxStackSize();
    }

    private void fillPockets(ItemStack stack) {
        for (int slot = FIRST_POCKET; slot < AbstractPet.BACKPACK_SIZE; slot++) {
            backpack.setItem(slot, stack.copy());
        }
    }

    private boolean bagSlotsEmpty() {
        for (int slot = AbstractPet.BACKPACK_SIZE; slot < AbstractPet.FULL_BACKPACK_SIZE; slot++) {
            if (!backpack.getItem(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Test
    void a_stack_goes_into_a_pocket_and_the_hand_stays_empty() {
        ItemStack left = pockets.stow(new ItemStack(Items.EMERALD, 24));

        assertTrue(left.isEmpty());
        assertTrue(backpack.getItem(AbstractPet.MAINHAND_SLOT).isEmpty());
        assertEquals(24, backpack.getItem(FIRST_POCKET).getCount());
    }

    @Test
    void the_slot_for_the_bag_stays_empty() {
        pockets.stow(new ItemStack(Items.EMERALD, 24));
        pockets.stow(new ItemStack(Items.WHEAT, 5));

        assertTrue(backpack.getItem(AbstractPet.BAG_SLOT).isEmpty());
    }

    @Test
    void with_no_bag_nothing_goes_into_the_bags_ten() {
        fillPockets(new ItemStack(Items.DIRT, 64));

        ItemStack left = pockets.stow(new ItemStack(Items.EMERALD, 24));

        assertEquals(24, left.getCount());
        assertTrue(bagSlotsEmpty());
        assertTrue(backpack.getItem(AbstractPet.MAINHAND_SLOT).isEmpty());
        assertTrue(backpack.getItem(AbstractPet.BAG_SLOT).isEmpty());
    }

    @Test
    void wearing_a_bag_what_overflows_goes_into_its_ten() {
        wearingBag = true;
        fillPockets(new ItemStack(Items.DIRT, 64));

        ItemStack left = pockets.stow(new ItemStack(Items.EMERALD, 24));

        assertTrue(left.isEmpty());
        assertEquals(24, backpack.getItem(AbstractPet.BACKPACK_SIZE).getCount());
        assertTrue(backpack.getItem(AbstractPet.MAINHAND_SLOT).isEmpty());
        assertTrue(backpack.getItem(AbstractPet.BAG_SLOT).isEmpty());
    }

    @Test
    void a_stack_that_matches_is_filled_before_an_empty_slot_is_used() {
        backpack.setItem(FIRST_POCKET + 3, new ItemStack(Items.EMERALD, 40));

        ItemStack left = pockets.stow(new ItemStack(Items.EMERALD, 30));

        assertTrue(left.isEmpty());
        assertEquals(stackSize, backpack.getItem(FIRST_POCKET + 3).getCount());
        assertEquals(6, backpack.getItem(FIRST_POCKET).getCount());
    }

    @Test
    void a_matching_stack_in_the_bags_ten_is_filled_before_a_pocket_is_begun() {
        wearingBag = true;
        backpack.setItem(AbstractPet.BACKPACK_SIZE + 2, new ItemStack(Items.EMERALD, 10));

        pockets.stow(new ItemStack(Items.EMERALD, 5));

        assertEquals(15, backpack.getItem(AbstractPet.BACKPACK_SIZE + 2).getCount());
        assertTrue(backpack.getItem(FIRST_POCKET).isEmpty());
    }

    @Test
    void what_does_not_fit_comes_back_and_what_was_given_is_not_touched() {
        fillPockets(new ItemStack(Items.EMERALD, stackSize - 1));
        ItemStack given = new ItemStack(Items.EMERALD, 30);

        ItemStack left = pockets.stow(given);

        assertEquals(30 - POCKETS, left.getCount());
        assertEquals(30, given.getCount());
        assertTrue(backpack.getItem(AbstractPet.MAINHAND_SLOT).isEmpty());
        assertTrue(backpack.getItem(AbstractPet.BAG_SLOT).isEmpty());
        assertTrue(bagSlotsEmpty());
    }

    @Test
    void a_stack_is_never_put_in_a_slot_bigger_than_it_stacks_to() {
        ItemStack left = pockets.stow(new ItemStack(Items.EMERALD, stackSize + 10));

        assertTrue(left.isEmpty());
        assertEquals(stackSize, backpack.getItem(FIRST_POCKET).getCount());
        assertEquals(10, backpack.getItem(FIRST_POCKET + 1).getCount());
    }

    @Test
    void room_is_what_stow_would_take() {
        fillPockets(new ItemStack(Items.DIRT, 64));
        backpack.setItem(FIRST_POCKET, new ItemStack(Items.EMERALD, stackSize - 4));
        backpack.setItem(FIRST_POCKET + 1, ItemStack.EMPTY);

        assertEquals(4 + stackSize, pockets.roomFor(new ItemStack(Items.EMERALD, 2 * stackSize)));
        assertEquals(5, pockets.roomFor(new ItemStack(Items.EMERALD, 5)));
        assertEquals(0, pockets.roomFor(new ItemStack(Items.WHEAT, 0)));

        wearingBag = true;
        assertEquals(2 * stackSize, pockets.roomFor(new ItemStack(Items.EMERALD, 2 * stackSize)));
    }

    @Test
    void asking_for_room_changes_nothing() {
        pockets.roomFor(new ItemStack(Items.EMERALD, 24));

        assertEquals(0, backpack.countItem(Items.EMERALD));
    }
}
