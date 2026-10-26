package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.MIDNIGHT;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.ownedPet;
import static com.dwinovo.chiikawa.gametest.GameTestKit.player;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;
import static com.dwinovo.chiikawa.gametest.GameTestKit.wildPet;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.data.WorkData;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.CombatStance;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.PetRules;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

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
    /** Long enough to be sure a pet that was going to do something has not done it. */
    private static final int WATCH_TICKS = 200;
    /** What a hunt matters as, and what a guard does, as the shipped data has them. */
    private static final int HUNT_PRIORITY = 50;
    private static final int GUARD_PRIORITY = 80;

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, MIDNIGHT);
    }

    /**
     * Told to help only in retaliation, a pet at heel does what a wolf does: it leaves a zombie alone that leaves its owner
     * alone, however near, and goes for it the moment it is after the owner.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WATCH_TICKS + FIGHT_TICKS)
    public static void a_retaliating_pet_leaves_a_zombie_alone_that_leaves_its_owner_alone(GameTestHelper helper) {
        ServerPlayer owner = owner(helper);
        AbstractPet pet = holding(ownedAndHeeling(helper, owner), Items.IRON_SWORD);
        pet.setRules(PetRules.DEFAULT.withStance(CombatStance.RETALIATE));
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(7, STAND, 4));
        zombie.setNoAi(true);

        helper.startSequence()
            .thenExecuteFor(WATCH_TICKS, () -> {
                helper.assertTrue(zombie.getHealth() == zombie.getMaxHealth(), "a retaliating pet went for a zombie that was no one's trouble");
                helper.assertFalse(pet.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET),
                    "a retaliating pet had its eye on a zombie that was no one's trouble");
            })
            .thenExecute(() -> zombie.setTarget(owner))
            .thenWaitUntil(() -> helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(),
                "a retaliating pet left a zombie to its owner that was after them"))
            .thenSucceed();
    }

    /**
     * Let loose, a pet with a hunting slip hunts even when it has been told to help only in retaliation: the slip is what it
     * was sent for.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = FIGHT_TICKS)
    public static void a_free_fencer_with_a_slay_slip_hunts(GameTestHelper helper) {
        AbstractPet pet = holding(ownedPet(helper, HERE), Items.IRON_SWORD);
        pet.setRules(PetRules.DEFAULT.withStance(CombatStance.RETALIATE));
        pet.setTask(HuntGameTests.hunting());
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(9, STAND, 4));
        zombie.setNoAi(true);

        helper.succeedWhen(() -> helper.assertTrue(hurt(zombie), "a free fencer with a slip for monsters left a zombie alone"));
    }

    /** And without the slip, a free fencer told to retaliate has no business with a zombie that is no one's trouble. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WATCH_TICKS + 100)
    public static void a_free_fencer_that_only_retaliates_does_not_hunt_without_a_slip(GameTestHelper helper) {
        AbstractPet pet = holding(ownedPet(helper, HERE), Items.IRON_SWORD);
        pet.setRules(PetRules.DEFAULT.withStance(CombatStance.RETALIATE));
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(9, STAND, 4));
        zombie.setNoAi(true);

        helper.runAtTickTime(WATCH_TICKS, () -> {
            helper.assertTrue(zombie.getHealth() == zombie.getMaxHealth(), "a free fencer hunted with no slip and no leave to");
            helper.succeed();
        });
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

    private static ServerPlayer owner(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        owner.setGameMode(GameType.SURVIVAL);
        owner.setPos(helper.absoluteVec(HERE.getCenter()));
        return owner;
    }

    /** That player's pet, at heel. */
    private static AbstractPet ownedAndHeeling(GameTestHelper helper, ServerPlayer owner) {
        AbstractPet pet = wildPet(helper, HERE);
        pet.tame(owner);
        pet.setPetDirective(PetDirective.FOLLOW);
        return pet;
    }

    private static boolean hurt(LivingEntity entity) {
        return entity.isDeadOrDying() || entity.getHealth() < entity.getMaxHealth();
    }
}
