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
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import com.dwinovo.chiikawa.shop.Wallet;

public class PetBackpackMenu extends AbstractContainerMenu {
    // Where the slots are. The screen draws a well behind each one by asking the slot
    // where it is, so these numbers are the layout and there is no second copy of them.
    // An item sits a pixel inside its 18px well, and wells sit side by side.
    private static final int SLOT_PITCH = 18;
    /** The hand, beside the pet's picture. */
    private static final int MAINHAND_X = 71;
    private static final int MAINHAND_Y = 25;
    /** The bag the pet wears, under the hand that holds its tool. */
    private static final int BAG_X = MAINHAND_X;
    private static final int BAG_Y = MAINHAND_Y + 21;
    private static final int PET_GRID_X = 95;
    private static final int PET_GRID_Y = MAINHAND_Y;
    /** How wide the pet's own grid is; the bag's ten hang under it in two rows. */
    private static final int PET_GRID_COLUMNS = 5;
    /** Where the bag's rows start: under the pockets, with a gap to say they are another thing. */
    private static final int BAG_ROWS_Y = 83;
    private static final int PLAYER_INV_X = 16;
    private static final int PLAYER_INV_Y = 132;
    private static final int HOTBAR_Y = 190;

    private final AbstractPet pet;
    private final Level level;
    private final SimpleContainer petContainer;
    private final int petSlotCount;
    /**
     * Whether the slots are showing: the pet screen has pages, and only one of them is its
     * backpack. Only ever turned off on the client — the server keeps every slot live, so
     * nothing it does with them depends on which page the owner happens to be looking at.
     */
    private boolean slotsShown = true;
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
            this.addSlot(new PageSlot(handler, AbstractPet.MAINHAND_SLOT, MAINHAND_X, MAINHAND_Y));
            this.addSlot(new BagSlot(handler, BAG_X, BAG_Y));
            int slot = AbstractPet.BAG_SLOT + 1;
            for (int row = 0; slot < Math.min(petSlotCount, AbstractPet.BACKPACK_SIZE); row++) {
                for (int col = 0; col < PET_GRID_COLUMNS && slot < AbstractPet.BACKPACK_SIZE; col++) {
                    this.addSlot(new PageSlot(handler, slot++,
                        PET_GRID_X + col * SLOT_PITCH, PET_GRID_Y + row * SLOT_PITCH));
                }
            }
            // The bag's own ten, waiting under the pet's grid for a bag to be worn.
            for (int row = 0; slot < petSlotCount; row++) {
                for (int col = 0; col < PET_GRID_COLUMNS && slot < petSlotCount; col++) {
                    this.addSlot(new BagContentSlot(handler, slot++,
                        PET_GRID_X + col * SLOT_PITCH, BAG_ROWS_Y + row * SLOT_PITCH));
                }
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new PageSlot(playerInventory, col + row * 9 + 9, PLAYER_INV_X + col * SLOT_PITCH, PLAYER_INV_Y + row * SLOT_PITCH));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new PageSlot(playerInventory, col, PLAYER_INV_X + col * SLOT_PITCH, HOTBAR_Y));
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

    /** Shows or hides every slot, for the page of the pet screen that is open. Client only. */
    public void showSlots(boolean shown) {
        this.slotsShown = shown;
    }

    /** How much money the pet has on it, as the owner sees its pockets. */
    public int petMoney() {
        return Wallet.count(petContainer);
    }

    /** A slot that is only there on the page that shows slots. */
    private class PageSlot extends Slot {
        private PageSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean isActive() {
            return slotsShown;
        }
    }

    /** Where the worn bag goes: one bag, and nothing else. */
    private final class BagSlot extends PageSlot {
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
    private final class BagContentSlot extends PageSlot {
        private BagContentSlot(SimpleContainer handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean isActive() {
            AbstractPet wearer = pet();
            return super.isActive() && wearer != null && wearer.isWearingBag();
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
