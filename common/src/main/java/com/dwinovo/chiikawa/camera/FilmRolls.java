package com.dwinovo.chiikawa.camera;

import com.dwinovo.chiikawa.init.InitDataComponents;
import com.dwinovo.chiikawa.item.FilmItem;
import com.dwinovo.chiikawa.photo.FilmRoll;
import com.dwinovo.chiikawa.photo.PhotoData;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Putting shots on rolls of film, in a pet's bag or a player's hand. A roll that has a shot
 * on it is a roll of its own, one to a stack; film that has none stacks like any item, and a
 * roll is begun by taking one off the pile.
 */
public final class FilmRolls {
    private FilmRolls() {
    }

    /** Whether this is film with shots on it and room for more. */
    public static boolean isOpen(ItemStack stack, Item film) {
        FilmRoll roll = FilmItem.rollOf(stack);
        return stack.is(film) && roll.started() && !roll.full();
    }

    /** Whether this is film nothing has been shot on. */
    public static boolean isFresh(ItemStack stack, Item film) {
        return stack.is(film) && !FilmItem.rollOf(stack).started();
    }

    /** Whether there is film to shoot on: a roll with room left, or a fresh one to begin. */
    public static boolean hasRoom(Container container, Item film) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (isOpen(stack, film) || isFresh(stack, film)) {
                return true;
            }
        }
        return false;
    }

    /** A roll with its first shot on it. */
    public static ItemStack begin(Item film, int exposures, PhotoData shot) {
        ItemStack roll = new ItemStack(film);
        roll.set(InitDataComponents.FILM_ROLL.get(), FilmRoll.begun(exposures, shot));
        // A roll is one to a stack, whatever the film it was made from stacks to.
        roll.set(DataComponents.MAX_STACK_SIZE, 1);
        return roll;
    }

    /** Puts a shot on a roll that has room for it. */
    public static void add(ItemStack roll, PhotoData shot) {
        roll.set(InitDataComponents.FILM_ROLL.get(), FilmItem.rollOf(roll).with(shot));
    }

    /**
     * Records a shot on the first roll in the container that has some but not all of its shots
     * taken; failing that, begins a roll from fresh film, taking one off the pile.
     *
     * @param exposures how many shots a roll begun now holds
     * @param overflow where a roll goes that there is no room for beside the film it came from
     * @return the roll the shot is on, nothing if there was no film to record it on
     */
    public static Optional<ItemStack> record(SimpleContainer container, Item film, int exposures, PhotoData shot,
            Consumer<ItemStack> overflow) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (isOpen(stack, film)) {
                add(stack, shot);
                container.setChanged();
                return Optional.of(stack);
            }
        }
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (isFresh(stack, film)) {
                ItemStack roll = begin(film, exposures, shot);
                stack.shrink(1);
                if (stack.isEmpty()) {
                    container.setItem(slot, roll);
                } else {
                    ItemStack left = container.addItem(roll);
                    if (!left.isEmpty()) {
                        overflow.accept(left);
                    }
                }
                container.setChanged();
                return Optional.of(roll);
            }
        }
        return Optional.empty();
    }
}
