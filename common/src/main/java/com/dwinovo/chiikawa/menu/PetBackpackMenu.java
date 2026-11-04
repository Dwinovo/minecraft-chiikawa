package com.dwinovo.chiikawa.menu;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitMenu;
import com.dwinovo.chiikawa.item.BagItem;
import com.dwinovo.chiikawa.network.PetPayloads.PetOutlookPayload;
import com.dwinovo.chiikawa.platform.Services;

import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.SimpleContainer;
import com.dwinovo.chiikawa.shop.Wallet;

public class PetBackpackMenu extends AbstractContainerMenu {
    // Where the slots are, from the panel's top-left corner. The screen draws a well behind each one by asking the slot
    // where it is, so these numbers are the layout and there is no second copy of them. An item sits a pixel inside its
    // 18px well, and wells sit side by side. The pet's own are in a card on the left and the player's to the right of it,
    // the hotbar level with the bag's first row; see PetScreenLayout for the card they are in.
    public static final int SLOT_PITCH = 18;
    /** The pet's hand, level with the middle of its own fifteen. */
    public static final int MAINHAND_X = 9;
    public static final int MAINHAND_Y = 97;
    /** The pet's own fifteen: three rows of five, to the right of its hand. */
    public static final int PET_GRID_X = 30;
    public static final int PET_GRID_Y = 79;
    /** How wide the pet's own grid is, and the bag's under it. */
    public static final int GRID_COLUMNS = 5;
    /** The bag the pet wears, level with the middle of the bag's own ten. */
    public static final int BAG_X = MAINHAND_X;
    public static final int BAG_Y = 146;
    /** Where the bag's rows start: under the pockets, across a line that says they are another thing. */
    public static final int BAG_ROWS_Y = 137;
    public static final int PLAYER_INV_X = 129;
    public static final int PLAYER_INV_Y = 79;
    public static final int HOTBAR_Y = 137;

    private final AbstractPet pet;
    private final Level level;
    private final SimpleContainer petContainer;
    private final int petSlotCount;
    /** Which entity the pet is, fixed when the menu is opened; the client finds the pet in its level by it. */
    private final int petId;
    /** How often the server tells the player who has the menu open what the pet would do next and why it is not at work. */
    private static final int OUTLOOK_INTERVAL = 20;
    /** Who has the menu open, on the server; the outlook is for them alone. */
    private final ServerPlayer viewer;
    /** Ticks of the server's sync left before the outlook is sent again; the first is sent at once. */
    private int untilOutlook;
    /** What the server last said of the pet, on the client. */
    private Optional<PetOutlookPayload> outlook = Optional.empty();

    /**
     * How the pet's id travels when the menu is opened, as the horse screen's does: a whole int in the
     * opening data. It cannot ride in a data slot, which the game sends as a short.
     */
    public static final StreamCodec<ByteBuf, Integer> OPENING_DATA = ByteBufCodecs.INT;
    /** What the opening data holds when there is no pet. */
    public static final int NO_PET = -1;

    /** What the server opens for a pet; each loader opens it with the pet's id as the opening data. */
    public static MenuProvider providerFor(AbstractPet pet) {
        return new SimpleMenuProvider((containerId, inventory, player) -> new PetBackpackMenu(containerId, inventory, pet),
            Component.translatable("menu.chiikawa.pet_backpack"));
    }

    /** The client's menu: the pet is the entity with the id the server opened it with. */
    public PetBackpackMenu(int containerId, Inventory playerInventory, int petId) {
        this(containerId, playerInventory, null, petId);
    }

    /** The server's menu, which has the pet in hand. */
    public PetBackpackMenu(int containerId, Inventory playerInventory, AbstractPet pet) {
        this(containerId, playerInventory, pet, pet != null ? pet.getId() : NO_PET);
    }

    private PetBackpackMenu(int containerId, Inventory playerInventory, AbstractPet pet, int petId) {
        super(InitMenu.PET_BACKPACK.get(), containerId);
        this.pet = pet;
        this.petId = petId;
        this.level = playerInventory.player.level();
        this.viewer = playerInventory.player instanceof ServerPlayer player ? player : null;

        SimpleContainer handler = pet != null ? pet.getBackpack() : new SimpleContainer(petSlots());
        this.petContainer = handler;
        this.petSlotCount = handler.getContainerSize();
        // Avoid SimpleContainer.startOpen to keep cross-loader bytecode free of ContainerUser.
        if (petSlotCount > 0) {
            this.addSlot(new Slot(handler, AbstractPet.MAINHAND_SLOT, MAINHAND_X, MAINHAND_Y));
            this.addSlot(new BagSlot(handler, BAG_X, BAG_Y));
            int slot = AbstractPet.BAG_SLOT + 1;
            for (int row = 0; slot < Math.min(petSlotCount, AbstractPet.BACKPACK_SIZE); row++) {
                for (int col = 0; col < GRID_COLUMNS && slot < AbstractPet.BACKPACK_SIZE; col++) {
                    this.addSlot(new Slot(handler, slot++,
                        PET_GRID_X + col * SLOT_PITCH, PET_GRID_Y + row * SLOT_PITCH));
                }
            }
            // The bag's own ten, waiting under the pet's grid for a bag to be worn.
            for (int row = 0; slot < petSlotCount; row++) {
                for (int col = 0; col < GRID_COLUMNS && slot < petSlotCount; col++) {
                    this.addSlot(new BagContentSlot(handler, slot++,
                        PET_GRID_X + col * SLOT_PITCH, BAG_ROWS_Y + row * SLOT_PITCH));
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

    /**
     * As a container menu syncs its slots, the server also tells the player who has the menu open what the pet would do next
     * and why it is not at work, every {@link #OUTLOOK_INTERVAL} ticks: not to anyone else, and not from the pet's tick,
     * which knows nothing of who is looking.
     */
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (viewer != null && pet != null && --untilOutlook <= 0) {
            untilOutlook = OUTLOOK_INTERVAL;
            if (pet.level() instanceof ServerLevel level && Services.NETWORK.canReceive(viewer, PetOutlookPayload.TYPE)) {
                Services.NETWORK.sendToClient(viewer, PetOutlookPayload.of(pet.getId(), pet.getMind().look(level),
                    pet.getMind().whyNot()));
            }
        }
    }

    /** Takes what the server said of the pet, if it is of this menu's pet. Client only. */
    public void receive(PetOutlookPayload said) {
        if (said.pet() == petId) {
            this.outlook = Optional.of(said);
        }
    }

    /** @return what the server last said of the pet, once it has said anything. Client only. */
    public Optional<PetOutlookPayload> outlook() {
        return outlook;
    }

    /** Whether a slot is the one the worn bag goes in. */
    public boolean isBagSlot(Slot slot) {
        return slot instanceof BagSlot;
    }

    /** Whether a slot is one of the bag's own ten, which are there to be used only while a bag is worn. */
    public boolean isBagContent(Slot slot) {
        return slot instanceof BagContentSlot;
    }

    /** How much money the pet has on it, as the owner sees its pockets. */
    public int petMoney() {
        return Wallet.count(petContainer);
    }

    /** Where the worn bag goes: one bag, and nothing else. */
    private final class BagSlot extends Slot {
        private BagSlot(SimpleContainer handler, int x, int y) {
            super(handler, AbstractPet.BAG_SLOT, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return BagItem.isBag(stack);
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
        Entity entity = level.getEntity(petId);
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
