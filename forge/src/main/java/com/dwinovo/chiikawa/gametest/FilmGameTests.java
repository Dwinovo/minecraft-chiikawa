package com.dwinovo.chiikawa.gametest;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.camera.FilmRolls;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.item.FilmItem;
import com.dwinovo.chiikawa.photo.PhotoData;
import com.dwinovo.chiikawa.photo.PhotoSubject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Rolls of film: where a shot goes in a bag, when a roll is full, and what stacks. These are
 * about the film as an item, which needs the game's own registries, so they are cases here
 * rather than unit tests.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FilmGameTests {
    private static final int EXPOSURES = 3;

    private static PhotoData shot() {
        return new PhotoData(UUID.randomUUID(), PhotoSubject.SCENERY, "", 1, "Hachiware");
    }

    private static Item film() {
        return InitItems.FILM.get();
    }

    /** Fresh film stacks to sixty-four; a roll with a shot on it is one to a stack. */
    @GameTest(template = "floor8", batch = "chiikawa_film", timeoutTicks = 20)
    public static void fresh_film_stacks_and_a_roll_does_not(GameTestHelper helper) {
        ItemStack fresh = new ItemStack(film(), 5);
        ItemStack roll = FilmRolls.begin(film(), EXPOSURES, shot());

        helper.assertTrue(fresh.getMaxStackSize() == 64, "fresh film stacks to " + fresh.getMaxStackSize());
        helper.assertTrue(roll.getMaxStackSize() == 1, "a roll stacks to " + roll.getMaxStackSize());
        helper.assertFalse(ItemStack.isSameItemSameTags(fresh, roll), "a roll is the same as fresh film");
        helper.succeed();
    }

    /** A shot goes on the roll that has room before a new roll is begun. */
    @GameTest(template = "floor8", batch = "chiikawa_film", timeoutTicks = 20)
    public static void a_shot_goes_on_the_roll_that_is_open_before_a_new_one_is_begun(GameTestHelper helper) {
        SimpleContainer bag = new SimpleContainer(8);
        bag.setItem(2, new ItemStack(film(), 3));
        ItemStack open = FilmRolls.begin(film(), EXPOSURES, shot());
        bag.setItem(5, open);

        boolean recorded = FilmRolls.record(bag, film(), EXPOSURES, shot(), dropped -> { });

        helper.assertTrue(recorded, "the shot was not recorded");
        helper.assertTrue(FilmItem.rollOf(open).shots().size() == 2, "the open roll has " + FilmItem.rollOf(open).shots().size());
        helper.assertTrue(bag.getItem(2).getCount() == 3, "a fresh roll was begun while one had room");
        helper.succeed();
    }

    /** With no roll open, one is split off the fresh pile and begun. */
    @GameTest(template = "floor8", batch = "chiikawa_film", timeoutTicks = 20)
    public static void a_new_roll_is_split_off_fresh_film(GameTestHelper helper) {
        SimpleContainer bag = new SimpleContainer(8);
        bag.setItem(4, new ItemStack(film(), 3));

        boolean recorded = FilmRolls.record(bag, film(), EXPOSURES, shot(), dropped -> { });

        helper.assertTrue(recorded, "the shot was not recorded");
        helper.assertTrue(bag.getItem(4).getCount() == 2 && !FilmItem.rollOf(bag.getItem(4)).started(),
            "the pile was not one fewer: " + bag.getItem(4));
        List<ItemStack> rolls = rollsIn(bag);
        helper.assertTrue(rolls.size() == 1 && FilmItem.rollOf(rolls.get(0)).shots().size() == 1
            && FilmItem.rollOf(rolls.get(0)).exposures() == EXPOSURES, "there should be one roll with one shot: " + rolls);
        helper.succeed();
    }

    /** The last of the fresh film is itself the roll, not a roll beside nothing. */
    @GameTest(template = "floor8", batch = "chiikawa_film", timeoutTicks = 20)
    public static void the_last_fresh_roll_is_begun_where_it_lies(GameTestHelper helper) {
        SimpleContainer bag = new SimpleContainer(4);
        bag.setItem(1, new ItemStack(film()));

        FilmRolls.record(bag, film(), EXPOSURES, shot(), dropped -> { });

        helper.assertTrue(FilmItem.rollOf(bag.getItem(1)).started() && rollsIn(bag).size() == 1, "the roll is not where the film was");
        helper.succeed();
    }

    /** A full roll is no room: the next shot begins another, and a bag with nothing fresh has none to begin. */
    @GameTest(template = "floor8", batch = "chiikawa_film", timeoutTicks = 20)
    public static void a_full_roll_has_no_room_for_a_shot(GameTestHelper helper) {
        SimpleContainer bag = new SimpleContainer(8);
        ItemStack full = FilmRolls.begin(film(), 1, shot());
        bag.setItem(0, full);

        helper.assertTrue(FilmItem.rollOf(full).full(), "a roll of one is not full with one shot");
        helper.assertFalse(FilmRolls.hasRoom(bag, film()), "a full roll has room");
        helper.assertFalse(FilmRolls.record(bag, film(), EXPOSURES, shot(), dropped -> { }), "a shot went on a full roll");

        bag.setItem(3, new ItemStack(film()));
        helper.assertTrue(FilmRolls.hasRoom(bag, film()), "fresh film has no room");
        helper.assertTrue(FilmRolls.record(bag, film(), EXPOSURES, shot(), dropped -> { }), "no new roll was begun");
        helper.assertTrue(FilmItem.rollOf(full).shots().size() == 1, "the full roll took another shot");
        helper.succeed();
    }

    /** A roll with nowhere to go beside the pile it came from falls at the pet's feet rather than being lost. */
    @GameTest(template = "floor8", batch = "chiikawa_film", timeoutTicks = 20)
    public static void a_roll_with_no_room_in_the_bag_is_dropped(GameTestHelper helper) {
        SimpleContainer bag = new SimpleContainer(1);
        bag.setItem(0, new ItemStack(film(), 2));
        List<ItemStack> dropped = new ArrayList<>();

        boolean recorded = FilmRolls.record(bag, film(), EXPOSURES, shot(), dropped::add);

        helper.assertTrue(recorded && dropped.size() == 1 && FilmItem.rollOf(dropped.get(0)).started(),
            "the roll was lost: " + dropped);
        helper.succeed();
    }

    /** The roll is ready to give once full and not before; fresh film is never ready. */
    @GameTest(template = "floor8", batch = "chiikawa_film", timeoutTicks = 20)
    public static void a_roll_is_ready_to_give_only_when_it_is_full(GameTestHelper helper) {
        FilmItem item = (FilmItem) film();
        ItemStack roll = FilmRolls.begin(film(), 2, shot());

        helper.assertFalse(item.readyToGive(new ItemStack(film())), "fresh film is ready to give");
        helper.assertFalse(item.readyToGive(roll), "an unfinished roll is ready to give");
        FilmRolls.add(roll, shot());
        helper.assertTrue(item.readyToGive(roll), "a full roll is not ready to give");
        helper.succeed();
    }

    private static List<ItemStack> rollsIn(SimpleContainer bag) {
        List<ItemStack> rolls = new ArrayList<>();
        for (int slot = 0; slot < bag.getContainerSize(); slot++) {
            if (bag.getItem(slot).is(film()) && FilmItem.rollOf(bag.getItem(slot)).started()) {
                rolls.add(bag.getItem(slot));
            }
        }
        return rolls;
    }
}
