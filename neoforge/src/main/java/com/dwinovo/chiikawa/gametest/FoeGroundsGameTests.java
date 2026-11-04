package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.MIDNIGHT;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.ownedPet;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;
import static com.dwinovo.chiikawa.gametest.GameTestKit.wildPet;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.data.WorkData;
import com.dwinovo.chiikawa.entity.AbstractPet;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * Why a pet fights: the grounds a wolf's target goals give, and what the owner's orders make of them. A pet fights for
 * itself, for its owner and for what comes by, and the third, which it does as a guard at its owner's side, it does as a
 * hunt once it is let loose, where it matters no more than its work does.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FoeGroundsGameTests {
    private static final String BATCH = "chiikawa_grounds";
    private static final int STAND = 2;
    private static final BlockPos HERE = new BlockPos(4, STAND, 4);
    /** Long enough to notice, walk over and land one. */
    private static final int FIGHT_TICKS = 1200;
    /** What a hunt matters as, and what a guard does, as the shipped data has them. */
    private static final int HUNT_PRIORITY = 50;
    private static final int GUARD_PRIORITY = 80;

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, MIDNIGHT);
    }

    /** A free fencer with a hunting slip hunts, as it always has. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = FIGHT_TICKS)
    public static void a_free_fencer_with_a_slay_slip_hunts(GameTestHelper helper) {
        AbstractPet pet = holding(ownedPet(helper, HERE), Items.IRON_SWORD);
        pet.setTask(HuntGameTests.hunting());
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(9, STAND, 4));
        zombie.setNoAi(true);

        helper.succeedWhen(() -> helper.assertTrue(hurt(zombie), "a free fencer with a slip for monsters left a zombie alone"));
    }

    /**
     * A free fencer clearing what comes by does it as a hunt, which matters as work does and no more; what hurts it is a
     * fight of defence, which matters more, and takes it from the hunt.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = FIGHT_TICKS)
    public static void a_free_fencer_clearing_nearby_hostiles_does_so_below_work_priority(GameTestHelper helper) {
        AbstractPet pet = holding(ownedPet(helper, HERE), Items.IRON_SWORD);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(11, STAND, 4));
        zombie.setNoAi(true);
        // The fight is what is watched, and a zombie that falls mid-case would end it.
        zombie.setInvulnerable(true);

        helper.startSequence()
            .thenWaitUntil(() -> {
                helper.assertTrue(pet.getMind().errands().isOn(WorkData.HUNT_MELEE), "the fencer never set off to hunt the zombie");
                helper.assertTrue(pet.getMind().errands().priority().orElse(-1) == HUNT_PRIORITY,
                    "a hunt matters as " + pet.getMind().errands().priority());
            })
            .thenExecute(() -> pet.hurt(zombie.damageSources().mobAttack(zombie), 1.0F))
            .thenWaitUntil(() -> {
                helper.assertTrue(pet.getMind().errands().isOn(WorkData.MELEE), "the fencer went on hunting what was hurting it");
                helper.assertTrue(pet.getMind().errands().priority().orElse(-1) == GUARD_PRIORITY,
                    "a fight of defence matters as " + pet.getMind().errands().priority());
            })
            .thenSucceed();
    }

    /** A wild pet has no leash to be let loose from: what comes by it deals with as a guard. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = FIGHT_TICKS)
    public static void a_wild_fencer_guards_its_patch_against_what_comes_by(GameTestHelper helper) {
        AbstractPet pet = holding(wildPet(helper, HERE), Items.IRON_SWORD);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(9, STAND, 4));
        zombie.setNoAi(true);
        zombie.setInvulnerable(true);

        helper.succeedWhen(() -> {
            helper.assertTrue(pet.getMind().errands().isOn(WorkData.MELEE), "the wild fencer did not set off to guard its patch");
            helper.assertTrue(pet.getMind().errands().priority().orElse(-1) == GUARD_PRIORITY, "a guard matters as "
                + pet.getMind().errands().priority());
        });
    }

    private static boolean hurt(LivingEntity entity) {
        return entity.isDeadOrDying() || entity.getHealth() < entity.getMaxHealth();
    }
}
