package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;
import static com.dwinovo.chiikawa.gametest.GameTestKit.wildPet;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.menu.PetBackpackMenu;
import com.dwinovo.chiikawa.network.PetServerPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What a right click does. Every instruction an owner can give a pet goes through one
 * hand and one button, so which of them happens depends on what is held and whether the
 * owner is crouching — a tangle worth pinning down.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InteractGameTests {
    private static final String BATCH = "chiikawa_interact";
    private static final int STAND = 2;
    /** Hurt this far below full, so a feed's healing is plain to see and cannot overshoot. */
    private static final float WOUND = 6.0F;

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /** Crouch and click, and the pet takes its next instruction: heel, sit, off you go. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void crouching_and_clicking_walks_through_the_instructions(GameTestHelper helper) {
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        pet.tame(owner);
        pet.setPetDirective(PetDirective.FOLLOW);
        owner.setShiftKeyDown(true);
        owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        PetDirective first = pet.getPetDirective();
        pet.mobInteract(owner, InteractionHand.MAIN_HAND);
        PetDirective second = pet.getPetDirective();
        helper.assertFalse(second == first, "crouching and clicking left the pet on the same instruction");

        pet.mobInteract(owner, InteractionHand.MAIN_HAND);
        pet.mobInteract(owner, InteractionHand.MAIN_HAND);
        helper.assertTrue(pet.getPetDirective() == first,
            "the instructions do not come back round to where they started");
        helper.succeed();
    }

    /**
     * The pet screen's orders page: picking an order is the same as crouching and clicking
     * through to it, and it goes straight to the one picked.
     */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void an_owner_can_order_a_pet_from_its_screen(GameTestHelper helper) {
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        pet.tame(owner);
        owner.setPos(helper.absoluteVec(new BlockPos(3, STAND, 5).getCenter()));

        helper.assertTrue(PetServerPacketHandler.order(owner, pet, PetDirective.STAY), "the owner's order was refused");
        helper.assertTrue(pet.getPetDirective() == PetDirective.STAY, "the pet did not take its owner's order");
        helper.succeed();
    }

    /** Anybody's client can send anything; only the owner's orders are heard. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void a_stranger_cannot_order_somebody_elses_pet(GameTestHelper helper) {
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        ServerPlayer stranger = helper.makeMockServerPlayerInLevel();
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        pet.tame(owner);
        pet.setPetDirective(PetDirective.FOLLOW);
        stranger.setPos(helper.absoluteVec(new BlockPos(3, STAND, 5).getCenter()));

        helper.assertFalse(PetServerPacketHandler.order(stranger, pet, PetDirective.FREE),
            "a stranger's order was heard");
        helper.assertTrue(pet.getPetDirective() == PetDirective.FOLLOW, "the pet did what a stranger told it");
        helper.succeed();
    }

    /** And an owner has to be near enough to be heard, as with any other screen. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void an_order_from_across_the_field_is_not_heard(GameTestHelper helper) {
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        pet.tame(owner);
        pet.setPetDirective(PetDirective.FOLLOW);
        owner.setPos(helper.absoluteVec(new BlockPos(3, STAND, 3).getCenter()).add(40.0, 0.0, 0.0));

        helper.assertFalse(PetServerPacketHandler.order(owner, pet, PetDirective.STAY), "an order carried forty blocks");
        helper.assertTrue(pet.getPetDirective() == PetDirective.FOLLOW, "the pet heard its owner from forty blocks off");
        helper.succeed();
    }

    /** Food in hand for a pet that is already yours feeds it rather than taming it again. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void feeding_your_own_pet_heals_it(GameTestHelper helper) {
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        pet.tame(owner);
        pet.setHealth(pet.getMaxHealth() - WOUND);
        float hurt = pet.getHealth();

        owner.setShiftKeyDown(false);
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COOKIE));
        pet.mobInteract(owner, InteractionHand.MAIN_HAND);

        helper.assertTrue(pet.getHealth() > hurt, "a fed pet was none the better for it");
        helper.succeed();
    }

    /** Empty hand, standing up: the pet's backpack opens. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void clicking_a_pet_opens_its_backpack(GameTestHelper helper) {
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        pet.tame(owner);
        owner.setShiftKeyDown(false);
        owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        pet.mobInteract(owner, InteractionHand.MAIN_HAND);

        helper.assertTrue(owner.containerMenu instanceof PetBackpackMenu,
            "clicking the pet did not open its backpack");
        helper.succeed();
    }

    /** Somebody else's pet keeps its business to itself. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void a_stranger_cannot_open_a_pet_s_backpack(GameTestHelper helper) {
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        ServerPlayer stranger = helper.makeMockServerPlayerInLevel();
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        pet.tame(owner);
        stranger.setShiftKeyDown(false);
        stranger.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        pet.mobInteract(stranger, InteractionHand.MAIN_HAND);

        helper.assertFalse(stranger.containerMenu instanceof PetBackpackMenu,
            "a stranger got into somebody else's pet's backpack");
        helper.succeed();
    }
}
