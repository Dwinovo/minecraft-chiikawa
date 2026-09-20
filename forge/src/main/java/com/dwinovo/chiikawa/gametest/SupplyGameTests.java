package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;
import static com.dwinovo.chiikawa.gametest.GameTestKit.wildPet;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * The things a pet can be given to carry, and what happens when they are taken away
 * again.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SupplyGameTests {
    private static final String BATCH = "chiikawa_supply";
    private static final int STAND = 2;
    /** Where the bag's own slots start; the pockets before it are the pet's own. */
    private static final int FIRST_BAG_SLOT = AbstractPet.BACKPACK_SIZE;

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /** A bag on its back is ten more slots, and the pet knows it is wearing one. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void a_bag_adds_room(GameTestHelper helper) {
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));

        helper.assertFalse(pet.isWearingBag(), "a pet turned up already wearing a bag");
        pet.setItemSlot(EquipmentSlot.CHEST, new ItemStack(InitItems.BEAR_BACKPACK.get()));

        helper.assertTrue(pet.isWearingBag(), "the bag went on and the pet did not notice");
        helper.assertTrue(pet.getBackpack().getContainerSize() == AbstractPet.FULL_BACKPACK_SIZE,
            "the bag's slots are not there to be used");
        helper.succeed();
    }

    /**
     * Take the bag off and what was in it lands on the floor. Anything else — vanishing,
     * or being stuffed into pockets that may already be full — loses a player's things.
     */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void taking_the_bag_off_empties_it_onto_the_floor(GameTestHelper helper) {
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        pet.setItemSlot(EquipmentSlot.CHEST, new ItemStack(InitItems.BEAR_BACKPACK.get()));
        pet.getBackpack().setItem(FIRST_BAG_SLOT, new ItemStack(Items.CAKE));

        pet.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        pet.dropBagContents();

        helper.assertTrue(pet.getBackpack().getItem(FIRST_BAG_SLOT).isEmpty(),
            "the cake stayed in a bag nobody is wearing");
        helper.assertItemEntityPresent(Items.CAKE, new BlockPos(3, STAND, 3), 4.0);
        helper.succeed();
    }

    /** The worn bag rides in the pet's own pockets, so it is saved and revived with it. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void the_worn_bag_is_part_of_what_the_pet_carries(GameTestHelper helper) {
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        pet.getBackpack().setItem(AbstractPet.BAG_SLOT, new ItemStack(InitItems.BEAR_BACKPACK.get()));

        helper.assertTrue(pet.getItemBySlot(EquipmentSlot.CHEST).is(InitItems.BEAR_BACKPACK.get()),
            "a bag put in the bag slot is not what the pet is wearing");
        helper.succeed();
    }
}
