package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.player;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;
import static com.dwinovo.chiikawa.gametest.GameTestKit.wildPet;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.init.InitDataComponents;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.item.WhistleCandyItem;
import com.dwinovo.chiikawa.whistle.PetWhistle;
import com.dwinovo.chiikawa.whistle.WhistleSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/**
 * The whistle candy: an owner blows it and the pets within earshot take the order, each a
 * moment later than the next; wild ones come over to look and stay wild; a pet at its exam
 * desk keeps writing. The blow itself is called the way the item calls it, so what is
 * checked is the service and the candy, not the right-click that holds one.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WhistleGameTests {
    private static final String BATCH = "chiikawa_whistle";
    /**
     * The candy's own range carries across several of these yards, so the cases that use it
     * run apart from the rest: a batch's cases run side by side, and another yard's wild pet
     * would hear the blow and be sent off to a different spot.
     */
    private static final String CANDY_BATCH = "chiikawa_whistle_candy";
    private static final int STAND = 2;
    /** Longer than the longest wait a pet has before it answers, and then some. */
    private static final int ANSWER_TICKS = 40;
    /** Long enough to walk over to see who whistled, which a pet in no hurry does at a few blocks a second. */
    private static final int COME_OVER_TICKS = 400;
    private static final Identifier FLUTE = Identifier.withDefaultNamespace("block.note_block.flute");
    /** Short enough that the far side of a sixteen block yard is out of earshot. */
    private static final WhistleSettings YARD = new WhistleSettings(14.0, 10, 40, 10, 200, FLUTE, 2.0F);
    private static final WhistleSettings SHORT = new WhistleSettings(6.0, 10, 40, 10, 200, FLUTE, 2.0F);

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.PEACEFUL, NOON);
    }

    @BeforeBatch(batch = CANDY_BATCH)
    public static void settleCandy(ServerLevel level) {
        settleWorld(level, Difficulty.PEACEFUL, NOON);
    }

    /** Told to follow, a pet within earshot comes to heel after its own wait; one out of earshot sits on. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = ANSWER_TICKS + 20)
    public static void blowing_follow_calls_the_pets_in_earshot_and_not_the_ones_out_of_it(GameTestHelper helper) {
        ServerPlayer owner = ownerAt(helper, new BlockPos(2, STAND, 2));
        AbstractPet near = owned(helper, owner, new BlockPos(4, STAND, 2), PetDirective.STAY);
        AbstractPet far = owned(helper, owner, new BlockPos(14, STAND, 14), PetDirective.STAY);

        PetWhistle.Heard heard = PetWhistle.blow(owner, PetDirective.FOLLOW, SHORT);

        helper.assertTrue(heard.owned() == 1 && heard.wild() == 0, "heard by " + heard);
        helper.succeedWhen(() -> {
            helper.assertTrue(near.getPetDirective() == PetDirective.FOLLOW, "the pet nearby did not come to heel");
            helper.assertTrue(far.getPetDirective() == PetDirective.STAY, "the pet out of earshot was called");
        });
    }

    /** The same blow with the order to sit sits a free pet down. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = ANSWER_TICKS + 20)
    public static void blowing_stay_sits_nearby_pets_down(GameTestHelper helper) {
        ServerPlayer owner = ownerAt(helper, new BlockPos(2, STAND, 2));
        AbstractPet pet = owned(helper, owner, new BlockPos(4, STAND, 4), PetDirective.FREE);

        PetWhistle.blow(owner, PetDirective.STAY, SHORT);

        helper.succeedWhen(() ->
            helper.assertTrue(pet.getPetDirective() == PetDirective.STAY, "the pet was not told to sit"));
    }

    /** A blow is a blow: the candy is eaten and the next one has to wait, though nobody was near. */
    @GameTest(template = "floor8", batch = CANDY_BATCH, timeoutTicks = 100)
    public static void the_candy_is_eaten_even_when_no_pet_hears_it(GameTestHelper helper) {
        ServerPlayer owner = ownerAt(helper, new BlockPos(2, STAND, 2));
        ItemStack candy = new ItemStack(InitItems.WHISTLE_CANDY.get(), 3);
        owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, candy);
        helper.assertTrue(WhistleSettings.of(BuiltInRegistries.ITEM.getKey(candy.getItem())).isPresent(),
            "no whistle settings were loaded for the candy");

        candy.getItem().finishUsingItem(candy, helper.getLevel(), owner);

        helper.assertTrue(candy.getCount() == 2, "the candy was not eaten, " + candy.getCount() + " left");
        helper.assertTrue(owner.getCooldowns().isOnCooldown(candy), "the candy can be blown again at once");
        helper.succeed();
    }

    /** The mode is the stack's: sneaking cycles it and the name follows it. */
    @GameTest(template = "floor8", batch = CANDY_BATCH, timeoutTicks = 100)
    public static void sneaking_with_the_candy_cycles_its_order(GameTestHelper helper) {
        ServerPlayer owner = ownerAt(helper, new BlockPos(2, STAND, 2));
        ItemStack candy = new ItemStack(InitItems.WHISTLE_CANDY.get(), 2);
        owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, candy);
        owner.setShiftKeyDown(true);

        candy.use(helper.getLevel(), owner, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(WhistleCandyItem.order(candy) == PetDirective.STAY, "follow did not go to stay");
        candy.use(helper.getLevel(), owner, net.minecraft.world.InteractionHand.MAIN_HAND);
        candy.use(helper.getLevel(), owner, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(WhistleCandyItem.order(candy) == PetDirective.FOLLOW, "free did not go back to follow");
        helper.assertFalse(candy.has(InitDataComponents.WHISTLE_MODE.get()),
            "a candy back at follow carries a note, and will not stack with the rest");
        helper.assertTrue(candy.getCount() == 2, "switching ate the candy");
        helper.succeed();
    }

    /** A wild pet in earshot comes over to look, and is still nobody's when it gets there. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = COME_OVER_TICKS)
    public static void a_wild_pet_comes_over_to_look_and_stays_wild(GameTestHelper helper) {
        ServerPlayer owner = ownerAt(helper, new BlockPos(2, STAND, 2));
        AbstractPet wild = wildPet(helper, new BlockPos(10, STAND, 10));
        PetDirective before = wild.getPetDirective();

        PetWhistle.Heard heard = PetWhistle.blow(owner, PetDirective.FOLLOW, YARD);

        helper.assertTrue(heard.owned() == 0 && heard.wild() == 1, "heard by " + heard);
        helper.succeedWhen(() -> {
            helper.assertTrue(wild.distanceToSqr(owner) < 6.0 * 6.0, "the wild pet did not come over");
            helper.assertFalse(wild.isTame(), "the whistle tamed the pet");
            helper.assertTrue(wild.getPetDirective() == before, "the whistle gave a wild pet an order");
        });
    }

    /** Someone else's pet, or a wild one told to sit, is not moved by a whistle that is not for it. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void a_wild_pet_pays_no_mind_to_an_order_to_sit(GameTestHelper helper) {
        ServerPlayer owner = ownerAt(helper, new BlockPos(2, STAND, 2));
        AbstractPet wild = wildPet(helper, new BlockPos(5, STAND, 5));

        PetWhistle.Heard heard = PetWhistle.blow(owner, PetDirective.STAY, SHORT);

        helper.assertTrue(heard.nobody(), "heard by " + heard);
        helper.assertFalse(wild.getBrain().hasMemoryValue(InitMemory.WHISTLE_HEARD.get()),
            "a wild pet was made curious by an order to sit");
        helper.succeed();
    }

    /** A pet at its desk, writing, is not got up by a whistle. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = ANSWER_TICKS + 20)
    public static void a_pet_at_its_exam_desk_keeps_writing(GameTestHelper helper) {
        ServerPlayer owner = ownerAt(helper, new BlockPos(2, STAND, 2));
        AbstractPet pet = owned(helper, owner, new BlockPos(4, STAND, 4), PetDirective.STAY);
        pet.setActivity(PetActivity.EXAM);

        PetWhistle.Heard heard = PetWhistle.blow(owner, PetDirective.FOLLOW, SHORT);

        helper.assertTrue(heard.nobody(), "heard by " + heard);
        helper.assertFalse(pet.getBrain().hasMemoryValue(InitMemory.WHISTLE_CALL.get()), "an order was left for it");
        helper.runAfterDelay(ANSWER_TICKS, () -> {
            helper.assertTrue(pet.getPetDirective() == PetDirective.STAY, "the pet at its desk was called away");
            helper.succeed();
        });
    }

    /** A player, in survival, standing at {@code rel}. */
    private static ServerPlayer ownerAt(GameTestHelper helper, BlockPos rel) {
        ServerPlayer owner = player(helper);
        owner.setGameMode(GameType.SURVIVAL);
        // By block position: the vec version does not turn with a rotated floor, which put the owner outside it.
        BlockPos at = helper.absolutePos(rel);
        owner.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return owner;
    }

    private static AbstractPet owned(GameTestHelper helper, ServerPlayer owner, BlockPos rel, PetDirective order) {
        AbstractPet pet = wildPet(helper, rel);
        pet.tame(owner);
        pet.setPetDirective(order);
        return pet;
    }
}
