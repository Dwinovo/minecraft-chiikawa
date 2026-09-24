package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.MIDNIGHT;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.ownedPet;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestDontPrefix;
import net.minecraftforge.gametest.GameTestHolder;

/**
 * Fighting: a pet with a weapon defends its patch, and one without the means to use it
 * does not pretend to.
 *
 * <p>Damage rather than death is what these watch for. A pet that has landed a hit is a
 * pet that found its target, walked to it and swung; waiting for the zombie to fall only
 * makes the case longer and flakier.
 */
@GameTestHolder(namespace = Constants.MOD_ID)
@GameTestDontPrefix
public final class CombatGameTests {
    private static final String BATCH = "chiikawa_combat";
    /**
     * Long enough for a pet to notice, close the distance and land one. An archer picks its
     * moment, so a tight clock here measures patience rather than aim.
     */
    private static final int FIGHT_TICKS = 2400;
    /** How long an archer with an empty quiver is watched before we believe it. */
    private static final int LEAVE_IT_TICKS = 400;

    private static final int STAND = 2;

    /**
     * Night, and not for atmosphere: a zombie stood in the noon sun catches fire and loses
     * health on its own, which would have these cases passing whether or not a pet ever
     * swung at anything. Difficulty has to be real too — peaceful takes the zombie away
     * mid-case and leaves the pet swinging at nothing.
     */
    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, MIDNIGHT);
    }

    /** A sword in hand, a zombie in the yard: the pet goes and deals with it. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = FIGHT_TICKS)
    public static void a_fencer_strikes_what_comes_at_it(GameTestHelper helper) {
        holding(ownedPet(helper, new BlockPos(4, STAND, 4)), Items.IRON_SWORD);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(8, STAND, 4));

        helper.succeedWhen(() -> helper.assertTrue(zombie.isDeadOrDying() || zombie.getHealth() < zombie.getMaxHealth(),
            "the fencer never landed a hit"));
    }

    /**
     * A cow in the same field is a cow, not a target. A fencer that went for whatever moved
     * would clear out a farm it was meant to be guarding.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = LEAVE_IT_TICKS + 100)
    public static void a_fencer_leaves_the_livestock_alone(GameTestHelper helper) {
        holding(ownedPet(helper, new BlockPos(4, STAND, 4)), Items.IRON_SWORD);
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(7, STAND, 4));
        float health = cow.getHealth();

        helper.runAtTickTime(LEAVE_IT_TICKS, () -> {
            helper.assertTrue(cow.isAlive() && cow.getHealth() >= health, "the fencer went for the cow");
            helper.succeed();
        });
    }

    /** A bow and a quiver with something in it. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = FIGHT_TICKS)
    public static void an_archer_with_arrows_shoots(GameTestHelper helper) {
        AbstractPet pet = holding(ownedPet(helper, new BlockPos(3, STAND, 4)), Items.BOW);
        pet.getBackpack().addItem(new ItemStack(Items.ARROW, 16));
        // Well inside what a pet can see, and still far enough that this is a bow shot.
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(10, STAND, 4));
        // Standing still. A zombie left to close the distance turns this into a scuffle,
        // and whether the archer got a shot off first comes down to the day it is having.
        zombie.setNoAi(true);

        helper.succeedWhen(() -> helper.assertTrue(zombie.isDeadOrDying() || zombie.getHealth() < zombie.getMaxHealth(),
            "the archer never hit anything"));
    }

    /**
     * A bow and nothing to put in it. The zombie is left alone — an archer out of arrows
     * gives up rather than walking into reach and hitting it with the bow.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = LEAVE_IT_TICKS + 100)
    public static void an_archer_without_arrows_holds_its_fire(GameTestHelper helper) {
        holding(ownedPet(helper, new BlockPos(3, STAND, 4)), Items.BOW);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(12, STAND, 4));
        float health = zombie.getHealth();

        helper.runAtTickTime(LEAVE_IT_TICKS, () -> {
            helper.assertTrue(zombie.isAlive() && zombie.getHealth() >= health,
                "an archer with an empty quiver hurt something anyway");
            helper.succeed();
        });
    }
}
