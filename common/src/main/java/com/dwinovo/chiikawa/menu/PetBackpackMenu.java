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
import net.minecraft.world.SimpleContainer;

public class PetBackpackMenu extends AbstractContainerMenu {
    // Slot positions match the pet_gui texture (item top-left = visual cell + 1px inset).
    private static final int SLOT_PITCH = 17;          // 18px cell, overlapping 1px
    private static final int MAINHAND_X = 77;
    private static final int MAINHAND_Y = 38;
    private static final int PET_GRID_X = 101;
    private static final int PET_GRID_Y = 20;
    private static final int PLAYER_INV_X = 24;
    private static final int PLAYER_INV_Y = 97;
    private static final int HOTBAR_Y = 151;

    private final AbstractPet pet;
    private final int petSlotCount;
    private final DataSlot petId = DataSlot.standalone();

    public PetBackpackMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public PetBackpackMenu(int containerId, Inventory playerInventory, AbstractPet pet) {
        super(InitMenu.PET_BACKPACK.get(), containerId);
        this.pet = pet;
        this.petId.set(pet != null ? pet.getId() : -1);
        this.addDataSlot(this.petId);

        SimpleContainer handler = pet != null ? pet.getBackpack() : new SimpleContainer(AbstractPet.BACKPACK_SIZE);
        this.petSlotCount = handler.getContainerSize();
        // Avoid SimpleContainer.startOpen to keep cross-loader bytecode free of ContainerUser.
        if (petSlotCount > 0) {
            this.addSlot(new Slot(handler, 0, MAINHAND_X, MAINHAND_Y));
            int slot = 1;
            for (int row = 0; row < 3 && slot < petSlotCount; row++) {
                for (int col = 0; col < 5 && slot < petSlotCount; col++) {
                    this.addSlot(new Slot(handler, slot++, PET_GRID_X + col * SLOT_PITCH, PET_GRID_Y + row * SLOT_PITCH));
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
        // Avoid SimpleContainer.stopOpen to keep cross-loader bytecode free of ContainerUser.
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
