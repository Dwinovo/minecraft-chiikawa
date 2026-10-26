package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.MIDNIGHT;
import static com.dwinovo.chiikawa.gametest.GameTestKit.count;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.ownedPet;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.data.WorkData;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.brain.errand.Outcome;
import com.dwinovo.chiikawa.entity.brain.errand.decide.DecisionLog;
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
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Fighting: a pet with a weapon defends its patch, and one without the means to use it
 * does not pretend to.
 *
 * <p>Damage rather than death is what these watch for. A pet that has landed a hit is a
 * pet that found its target, walked to it and swung; waiting for the zombie to fall only
 * makes the case longer and flakier.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CombatGameTests {
    private static final String BATCH = "chiikawa_combat";
    /**
     * Long enough for a pet to notice, close the distance and land one. An archer picks its
     * moment, so a tight clock here measures patience rather than aim.
     */
    private static final int FIGHT_TICKS = 2400;
    private static final int ARROWS = 16;
    /** Longer than a draw and a shot, so that an arrow still to be loosed would have been. */
    private static final int DRAW_WATCH_TICKS = 40;
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
     * A fight is about the foe the pet set off for, and over when that one is down: the pet is done with it, rests from
     * nothing, and sets off for the next the moment it is named, so a field of zombies is cleared one after another.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = FIGHT_TICKS)
    public static void a_fencer_goes_on_to_the_next_foe_when_one_falls(GameTestHelper helper) {
        AbstractPet pet = holding(ownedPet(helper, new BlockPos(4, STAND, 4)), Items.IRON_SWORD);
        Zombie first = helper.spawn(EntityType.ZOMBIE, new BlockPos(8, STAND, 4));
        Zombie second = helper.spawn(EntityType.ZOMBIE, new BlockPos(8, STAND, 8));
        for (Zombie zombie : new Zombie[] {first, second}) {
            zombie.setNoAi(true);
            zombie.setHealth(1.0F);
        }

        helper.succeedWhen(() -> {
            helper.assertTrue(!first.isAlive() && !second.isAlive(), "the fencer left a zombie standing");
            helper.assertTrue(pet.getMind().log().entries().stream().anyMatch(entry -> entry instanceof DecisionLog.Ended ended
                && ended.errand().equals(WorkData.MELEE) && ended.outcome() instanceof Outcome.Done),
                "the fight with the first zombie did not end done");
            helper.assertFalse(pet.getMind().log().entries().stream().anyMatch(entry -> entry instanceof DecisionLog.Ended ended
                && ended.errand().equals(WorkData.MELEE) && ended.outcome() instanceof Outcome.GaveUp),
                "the fencer gave up a fight on the way");
        });
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
     * Told to sit with the bow half drawn, an archer lowers it and does not shoot. The arrow that would have flown is
     * still in the quiver and the zombie is untouched, however long it is watched.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = FIGHT_TICKS)
    public static void an_archer_told_to_sit_mid_draw_does_not_shoot(GameTestHelper helper) {
        AbstractPet pet = holding(ownedPet(helper, new BlockPos(3, STAND, 4)), Items.BOW);
        pet.getBackpack().addItem(new ItemStack(Items.ARROW, ARROWS));
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(10, STAND, 4));
        zombie.setNoAi(true);

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(pet.isUsingItem(), "the archer never drew its bow"))
            .thenExecute(() -> pet.setPetDirective(PetDirective.STAY))
            .thenWaitUntil(() -> helper.assertFalse(pet.isUsingItem(), "the archer told to sit kept its bow raised"))
            .thenExecuteFor(DRAW_WATCH_TICKS, () -> {
                helper.assertFalse(pet.isUsingItem(), "the archer told to sit raised its bow again");
                helper.assertTrue(count(pet, Items.ARROW) == ARROWS, "the archer told to sit loosed an arrow");
                helper.assertTrue(zombie.getHealth() == zombie.getMaxHealth(), "the archer told to sit hit the zombie");
            })
            .thenSucceed();
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
