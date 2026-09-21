package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.player;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;
import static com.dwinovo.chiikawa.gametest.GameTestKit.wildPet;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * How a pet and a person come to belong together: feeding one until it trusts you, it
 * keeping up with you afterwards, and its keeping its hands to itself before then.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CareGameTests {
    private static final String BATCH = "chiikawa_care";
    private static final int FOLLOW_TICKS = 1200;
    /** How long a wild pet is watched keeping its hands to itself. */
    private static final int LEAVE_IT_TICKS = 400;
    /**
     * How many cookies it may take. Taming turns on a roll of about one in three, so this
     * is many more tries than it can plausibly need — the case is about whether feeding
     * tames at all, not about how generous the odds are.
     */
    private static final int COOKIES = 60;
    /** Near enough to count as keeping up: the following rule aims for two blocks. */
    private static final double AT_HEEL = 4.0;

    private static final int STAND = 2;

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /** Feed a wild pet enough and it is yours — and a new pet comes to heel, not to work. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 200)
    public static void feeding_a_wild_pet_tames_it(GameTestHelper helper) {
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        Player player = GameTestKit.owner(helper);

        for (int cookie = 0; cookie < COOKIES && !pet.isTame(); cookie++) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COOKIE));
            pet.mobInteract(player, InteractionHand.MAIN_HAND);
        }

        helper.assertTrue(pet.isTame(), "a wild pet ate " + COOKIES + " cookies and still would not be tamed");
        // By name on the tag, not by the owner standing there: this one never entered the
        // world, and a pet asked who its owner is looks them up among the players present.
        helper.assertTrue(player.getUUID().equals(pet.getOwnerUUID()), "the pet was tamed by somebody else");
        helper.assertTrue(pet.getPetDirective() == PetDirective.FOLLOW,
            "a freshly tamed pet did not come to heel");
        helper.succeed();
    }

    /** Its owner walks off; it comes along. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = FOLLOW_TICKS)
    public static void a_pet_follows_its_owner(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        pet.tame(owner);
        pet.setPetDirective(PetDirective.FOLLOW);

        BlockPos away = helper.absolutePos(new BlockPos(13, STAND, 13));
        owner.teleportTo(away.getX() + 0.5, away.getY(), away.getZ() + 0.5);

        helper.succeedWhen(() -> helper.assertTrue(pet.distanceToSqr(owner) < AT_HEEL * AT_HEEL,
            "the pet stayed where it was while its owner walked off"));
    }

    /**
     * Told to sit, it sits, even when its owner walks off. Sitting has to beat following or
     * "wait here" means "wait here until I move", which is no use to anyone leaving a pet
     * behind on purpose.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = LEAVE_IT_TICKS + 100)
    public static void a_sitting_pet_does_not_come_along(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        pet.tame(owner);
        pet.setPetDirective(PetDirective.STAY);
        BlockPos seat = pet.blockPosition();

        BlockPos away = helper.absolutePos(new BlockPos(13, STAND, 13));
        owner.teleportTo(away.getX() + 0.5, away.getY(), away.getZ() + 0.5);

        helper.runAtTickTime(LEAVE_IT_TICKS, () -> {
            helper.assertTrue(pet.blockPosition().distSqr(seat) < AT_HEEL * AT_HEEL,
                "a sitting pet got up and went after its owner");
            helper.succeed();
        });
    }

    /**
     * A wild pet with a sword is a pet with a job, not a threat. Anything else and a
     * player could not walk up to one to feed it in the first place.
     */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = LEAVE_IT_TICKS + 100)
    public static void a_wild_pet_does_not_go_for_the_player(GameTestHelper helper) {
        ServerPlayer bystander = player(helper);
        BlockPos beside = helper.absolutePos(new BlockPos(4, STAND, 3));
        bystander.teleportTo(beside.getX() + 0.5, beside.getY(), beside.getZ() + 0.5);
        holding(wildPet(helper, new BlockPos(3, STAND, 3)), Items.IRON_SWORD);

        float health = bystander.getHealth();
        helper.runAtTickTime(LEAVE_IT_TICKS, () -> {
            helper.assertTrue(bystander.getHealth() >= health, "a wild pet went for the player");
            helper.succeed();
        });
    }
}
