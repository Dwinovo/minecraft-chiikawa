package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.player;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;
import static com.dwinovo.chiikawa.gametest.GameTestKit.wildPet;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.PetFollowKeeper;
import com.dwinovo.chiikawa.entity.brain.constraint.AnchorDistances;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Catching up. A pet that is told to follow and then left far behind is fetched by the
 * server, not by itself — which is the only way it works for a pet that has stopped
 * thinking, and that is exactly the pet an owner loses.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FollowGameTests {
    private static final String BATCH = "chiikawa_follow";
    private static final int STAND = 2;
    /** Where the owner stands. */
    private static final BlockPos HERE = new BlockPos(4, STAND, 4);
    /** Well past the distance a following pet is allowed to fall behind. */
    private static final BlockPos LEFT_BEHIND = new BlockPos(30, STAND, 30);

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /**
     * The case the whole thing is for: the pet's own AI is switched off, which is what a
     * chunk that is loaded but not ticking amounts to, and it still comes back.
     */
    @GameTest(template = "floor32", batch = BATCH, timeoutTicks = 200)
    public static void a_pet_that_is_not_thinking_is_still_fetched(GameTestHelper helper) {
        ServerPlayer owner = owner(helper);
        AbstractPet pet = following(helper, owner);
        pet.setNoAi(true);
        strand(helper, pet);

        PetFollowKeeper.fetchStrays(helper.getLevel());

        helper.assertTrue(caughtUp(pet, owner), "the pet was left behind with nobody to notice");
        helper.succeed();
    }

    /** Told to sit is told to sit: being far away is then the owner's own doing. */
    @GameTest(template = "floor32", batch = BATCH, timeoutTicks = 200)
    public static void a_pet_told_to_sit_stays_sitting(GameTestHelper helper) {
        ServerPlayer owner = owner(helper);
        AbstractPet pet = following(helper, owner);
        pet.setPetDirective(PetDirective.STAY);
        strand(helper, pet);

        PetFollowKeeper.fetchStrays(helper.getLevel());

        helper.assertFalse(caughtUp(pet, owner), "a pet that was told to stay put was dragged along anyway");
        helper.succeed();
    }

    /** A pet on free roam keeps to its own patch rather than being pulled to the owner. */
    @GameTest(template = "floor32", batch = BATCH, timeoutTicks = 200)
    public static void a_free_pet_is_not_pulled_along(GameTestHelper helper) {
        ServerPlayer owner = owner(helper);
        AbstractPet pet = following(helper, owner);
        pet.setPetDirective(PetDirective.FREE);
        pet.setNoAi(true);
        strand(helper, pet);

        PetFollowKeeper.fetchStrays(helper.getLevel());

        helper.assertFalse(caughtUp(pet, owner), "a pet left to roam was fetched as if it were at heel");
        helper.succeed();
    }

    /** And a pet that belongs to nobody is nobody's to fetch. */
    @GameTest(template = "floor32", batch = BATCH, timeoutTicks = 200)
    public static void a_wild_pet_is_left_where_it_is(GameTestHelper helper) {
        ServerPlayer owner = owner(helper);
        AbstractPet pet = wildPet(helper, HERE);
        pet.setNoAi(true);
        strand(helper, pet);

        PetFollowKeeper.fetchStrays(helper.getLevel());

        helper.assertFalse(caughtUp(pet, owner), "a wild pet was fetched to somebody it does not belong to");
        helper.succeed();
    }

    /** Somebody standing in the test area for a pet to belong to and fall behind. */
    private static ServerPlayer owner(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        owner.setPos(helper.absoluteVec(HERE.getCenter()));
        return owner;
    }

    /** That player's pet, at heel. */
    private static AbstractPet following(GameTestHelper helper, ServerPlayer owner) {
        AbstractPet pet = wildPet(helper, HERE);
        pet.tame(owner);
        pet.setPetDirective(PetDirective.FOLLOW);
        return pet;
    }

    /** Carts the pet off to the far corner of the floor. */
    private static void strand(GameTestHelper helper, AbstractPet pet) {
        BlockPos away = helper.absolutePos(LEFT_BEHIND);
        pet.teleportTo(away.getX() + 0.5, away.getY(), away.getZ() + 0.5);
    }

    /** Whether the pet is near enough its owner to count as having caught up. */
    private static boolean caughtUp(AbstractPet pet, ServerPlayer owner) {
        return pet.distanceToSqr(owner) < AnchorDistances.FOLLOW_TELEPORT * AnchorDistances.FOLLOW_TELEPORT;
    }
}
