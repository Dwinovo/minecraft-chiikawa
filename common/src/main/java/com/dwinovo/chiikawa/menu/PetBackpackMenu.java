package com.dwinovo.chiikawa.menu;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitMenu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.dwinovo.chiikawa.init.InitItems;
import net.minecraft.world.SimpleContainer;

public class PetBackpackMenu extends AbstractContainerMenu {
    // Where the slots are. The screen draws a well behind each one by asking the slot
    // where it is, so these numbers are the layout and there is no second copy of them.
    // An item sits a pixel inside its 18px cell, and cells overlap by a pixel.
    private static final int SLOT_PITCH = 17;
    private static final int MAINHAND_X = 77;
    private static final int MAINHAND_Y = 32;
    /** The bag the pet wears, under the hand that holds its tool. */
    private static final int BAG_X = MAINHAND_X;
    private static final int BAG_Y = MAINHAND_Y + SLOT_PITCH + 4;
    private static final int PET_GRID_X = 103;
    private static final int PET_GRID_Y = 9;
    /** How wide the pet's own grid is; the bag's ten hang under it in two rows. */
    private static final int PET_GRID_COLUMNS = 5;
    private static final int PLAYER_INV_X = 22;
    private static final int PLAYER_INV_Y = 144;
    private static final int HOTBAR_Y = 199;

    private final AbstractPet pet;
    private final Level level;
    private final int petSlotCount;
    private final DataSlot petId = DataSlot.standalone();

    public PetBackpackMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public PetBackpackMenu(int containerId, Inventory playerInventory, AbstractPet pet) {
        super(InitMenu.PET_BACKPACK.get(), containerId);
        this.pet = pet;
        this.level = playerInventory.player.level();
        this.petId.set(pet != null ? pet.getId() : -1);
        this.addDataSlot(this.petId);

        SimpleContainer handler = pet != null ? pet.getBackpack() : new SimpleContainer(petSlots());
        this.petSlotCount = handler.getContainerSize();
        if (pet != null) {
            handler.startOpen(playerInventory.player);
        }
        if (petSlotCount > 0) {
            this.addSlot(new Slot(handler, AbstractPet.MAINHAND_SLOT, MAINHAND_X, MAINHAND_Y));
            this.addSlot(new BagSlot(handler, BAG_X, BAG_Y));
            int slot = AbstractPet.BAG_SLOT + 1;
            for (int row = 0; slot < Math.min(petSlotCount, AbstractPet.BACKPACK_SIZE); row++) {
                for (int col = 0; col < PET_GRID_COLUMNS && slot < AbstractPet.BACKPACK_SIZE; col++) {
                    this.addSlot(new Slot(handler, slot++,
                        PET_GRID_X + col * SLOT_PITCH, PET_GRID_Y + row * SLOT_PITCH));
                }
            }
            // The bag's own ten, waiting under the pet's grid for a bag to be worn.
            int bagRowsStart = PET_GRID_Y + 3 * SLOT_PITCH;
            for (int row = 0; slot < petSlotCount; row++) {
                for (int col = 0; col < PET_GRID_COLUMNS && slot < petSlotCount; col++) {
                    this.addSlot(new BagContentSlot(handler, slot++,
                        PET_GRID_X + col * SLOT_PITCH, bagRowsStart + row * SLOT_PITCH));
                }
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, PLAYER_INV_X + col * SLOT_PITCH, PLAYER_INV_Y + row * SLOT_PITCH));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, PLAYER_INV_X + col * SLOT_PITCH, HOTBAR_Y));
        }
    }

    /**
     * How many pockets a pet has, and so how many slots a menu builds for one. The client
     * is handed a menu without the pet in it and has to lay out the same slots all the
     * same: one fewer and the first packet of contents falls off the end of the list, which
     * is a disconnect rather than a missing slot.
     */
    public static int petSlots() {
        return AbstractPet.FULL_BACKPACK_SIZE;
    }

    /**
     * The pet this menu is about, on either side. The server has it in hand; the client
     * looks it up by the id the menu carries, which is what lets a slot know whether the
     * pet is wearing its bag.
     */
    private AbstractPet pet() {
        return pet != null ? pet : getPet(level);
    }

    /** Where the worn bag goes: one bag, and nothing else. */
    private final class BagSlot extends Slot {
        private BagSlot(SimpleContainer handler, int x, int y) {
            super(handler, AbstractPet.BAG_SLOT, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(InitItems.BEAR_BACKPACK.get());
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            super.onTake(player, stack);
            // Taking the bag off empties it where the owner can see, rather than leaving
            // ten slots of things in a bag nobody is wearing.
            AbstractPet wearer = pet();
            if (wearer != null && !wearer.isWearingBag()) {
                wearer.dropBagContents();
            }
        }
    }

    /** One of the bag's own slots: there to be used only while a bag is worn. */
    private final class BagContentSlot extends Slot {
        private BagContentSlot(SimpleContainer handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean isActive() {
            AbstractPet wearer = pet();
            return wearer != null && wearer.isWearingBag();
        }
    }

    public AbstractPet getPet() {
        return pet;
    }

    public AbstractPet getPet(Level level) {
        if (pet != null) {
            return pet;
        }
        if (level == null) {
            return null;
        }
        Entity entity = level.getEntity(petId.get());
        return entity instanceof AbstractPet ? (AbstractPet) entity : null;
    }

    @Override
    public boolean stillValid(Player player) {
        return pet == null || (pet.isAlive() && player.distanceToSqr(pet) <= 64.0D);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (pet != null) {
            pet.getBackpack().stopOpen(player);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (player.level().isClientSide()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = ItemStack.EMPTY;
        if (slotIndex < 0 || slotIndex >= this.slots.size()) {
            return result;
        }
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            int playerInvStart = petSlotCount;
            int playerInvEnd = playerInvStart + 27;
            int hotbarEnd = playerInvEnd + 9;

            if (slotIndex < petSlotCount) {
                if (!this.moveItemStackTo(stack, playerInvStart, hotbarEnd, true)) {
                    return ItemStack.EMPTY;
                }
            }
            else {
                if (!this.moveItemStackTo(stack, 0, petSlotCount, false)) {
                    if (slotIndex < playerInvEnd) {
                        if (!this.moveItemStackTo(stack, playerInvEnd, hotbarEnd, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                    else if (!this.moveItemStackTo(stack, playerInvStart, playerInvEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            }
            else {
                slot.setChanged();
            }
            if (stack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
            this.broadcastChanges();
        }
        return result;
    }
}

