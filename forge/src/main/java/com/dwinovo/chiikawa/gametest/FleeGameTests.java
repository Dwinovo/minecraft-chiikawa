package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.MIDNIGHT;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.player;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;
import static com.dwinovo.chiikawa.gametest.GameTestKit.wildPet;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.CombatStance;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.PetRules;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.decide.DecisionLog;
import com.dwinovo.chiikawa.entity.brain.errand.run.Running;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A pet that is hurt and will not fight back runs: to hide behind its owner, or at home, whatever it has been told, a pet
 * told to sit included, which goes back to where it sat when it is safe and sits there again.
 *
 * <p>The game's own hurt-by memory is what tells the pet it was hurt, and its sensor looks only every second, so a case
 * waits a second for the pet to know.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FleeGameTests {
    private static final String BATCH = "chiikawa_flee";
    private static final int STAND = 2;
    /** Long enough to be hurt, know it, run and hide. */
    private static final int HIDE_TICKS = 400;
    /** Long enough to come out of hiding, once it is safe, and walk back across the yard. */
    private static final int SAFE_TICKS = 700;
    /** The step of the flight at which the pet cowers. */
    private static final int COWERING = 1;
    private static final int CROSSING = 8;

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, MIDNIGHT);
    }

    /**
     * Told not to fight, a pet that is hit runs to its owner and gets behind them, farther from what hit it than they are,
     * and does not strike back though it holds a sword.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = HIDE_TICKS)
    public static void a_pet_told_not_to_fight_hides_behind_its_owner_when_hit(GameTestHelper helper) {
        ServerPlayer owner = owner(helper, new BlockPos(11, STAND, 8));
        AbstractPet pet = holding(heeling(helper, owner, new BlockPos(6, STAND, 8)), Items.IRON_SWORD);
        pet.setRules(PetRules.DEFAULT.withStance(CombatStance.NONE));
        Zombie zombie = zombie(helper, new BlockPos(3, STAND, 8));
        pet.hurt(zombie.damageSources().mobAttack(zombie), 1.0F);

        helper.succeedWhen(() -> {
            helper.assertTrue(cowering(pet), "the pet did not run and hide, doing " + pet.getIntent());
            helper.assertTrue(pet.distanceTo(zombie) > owner.distanceTo(zombie),
                "the pet hid " + (int) pet.distanceTo(zombie) + " blocks from the zombie, with its owner " + (int) owner.distanceTo(zombie));
            helper.assertTrue(pet.distanceTo(owner) < 4.0, "the pet hid " + (int) pet.distanceTo(owner) + " blocks from its owner");
            helper.assertTrue(zombie.getHealth() == zombie.getMaxHealth(), "the pet struck back though it was told not to fight");
        });
    }

    /**
     * Told to sit, a pet that is hit gets up and runs to its owner, and the order is not touched: when it is safe it walks
     * back to the place it sat and is sitting there again.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = SAFE_TICKS)
    public static void a_sitting_pet_hit_runs_to_its_owner_and_sits_again_where_it_sat(GameTestHelper helper) {
        ServerPlayer owner = owner(helper, new BlockPos(12, STAND, 8));
        BlockPos seat = new BlockPos(4, STAND, 8);
        AbstractPet pet = holding(heeling(helper, owner, seat), Items.IRON_SWORD);
        pet.setPetDirective(PetDirective.STAY);
        Zombie zombie = zombie(helper, new BlockPos(2, STAND, 8));
        pet.hurt(zombie.damageSources().mobAttack(zombie), 1.0F);

        helper.startSequence()
            .thenWaitUntil(() -> {
                helper.assertTrue(pet.getPetDirective() == PetDirective.STAY, "running changed what it was told");
                helper.assertTrue(cowering(pet), "the pet did not run and hide, doing " + pet.getIntent());
                helper.assertTrue(pet.distanceTo(owner) < 4.0, "the pet hid " + (int) pet.distanceTo(owner) + " blocks from its owner");
            })
            .thenWaitUntil(() -> {
                helper.assertTrue(pet.getPetDirective() == PetDirective.STAY, "running changed what it was told");
                helper.assertFalse(pet.getMind().errands().isOn(ErrandIds.FLEE), "the pet is still hiding, doing " + pet.getIntent());
                helper.assertTrue(pet.blockPosition().distSqr(helper.absolutePos(seat)) <= CROSSING,
                    "the pet is " + (int) Math.sqrt(pet.blockPosition().distSqr(helper.absolutePos(seat))) + " blocks from its seat");
            })
            .thenExecute(() -> {
                helper.assertTrue(pet.getPetDirective() == PetDirective.STAY, "the pet is no longer told to sit");
                helper.assertTrue(zombie.getHealth() == zombie.getMaxHealth(), "the pet struck back though it was told to sit");
            })
            .thenSucceed();
    }

    /**
     * With no owner about and nowhere to go but home, a pet with nothing to fight with runs there. The pet is hurt as it lands
     * on the floor and sets off in the air, where the brain's mover finds no way to anywhere and says so; that is not a
     * reason to stop where it stands.
     */
    @GameTest(template = "floor32", batch = BATCH, timeoutTicks = HIDE_TICKS)
    public static void an_unarmed_wild_pet_hit_runs_home(GameTestHelper helper) {
        BlockPos home = new BlockPos(8, STAND, 16);
        AbstractPet pet = wildPet(helper, new BlockPos(20, STAND, 16));
        pet.getBrain().setMemory(MemoryModuleType.HOME, GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(home)));
        Zombie zombie = zombie(helper, new BlockPos(24, STAND, 16));
        pet.hurt(zombie.damageSources().mobAttack(zombie), 1.0F);

        helper.succeedWhen(() -> {
            helper.assertTrue(cowering(pet), "the pet did not run and hide, doing " + pet.getIntent());
            helper.assertTrue(pet.blockPosition().distSqr(helper.absolutePos(home)) <= CROSSING,
                "the pet is hiding " + (int) Math.sqrt(pet.blockPosition().distSqr(helper.absolutePos(home))) + " blocks from home");
            helper.assertTrue(zombie.getHealth() == zombie.getMaxHealth(), "the pet hit the zombie with nothing to hit it with");
        });
    }

    /** Hurt by its owner's sword, a pet is hurt by nothing it need run from. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = HIDE_TICKS)
    public static void a_pet_hurt_by_its_owner_does_not_run_from_them(GameTestHelper helper) {
        ServerPlayer owner = owner(helper, new BlockPos(6, STAND, 8));
        AbstractPet pet = heeling(helper, owner, new BlockPos(4, STAND, 8));
        pet.hurt(owner.damageSources().playerAttack(owner), 1.0F);

        helper.runAtTickTime(HIDE_TICKS - 20, () -> {
            helper.assertFalse(pet.getMind().errands().isOn(ErrandIds.FLEE) || pet.getMind().log().entries().stream().anyMatch(entry ->
                entry instanceof DecisionLog.Ended ended && ended.errand().equals(ErrandIds.FLEE)), "the pet ran from its owner's sword");
            helper.succeed();
        });
    }

    /** Whether the pet is cowering: on the flight, past the run. */
    private static boolean cowering(AbstractPet pet) {
        return pet.getMind().errands().running().filter(running -> running.errand().equals(ErrandIds.FLEE))
            .map(Running::stepIndex).filter(step -> step >= COWERING).isPresent();
    }

    private static ServerPlayer owner(GameTestHelper helper, BlockPos where) {
        ServerPlayer owner = player(helper);
        owner.setGameMode(GameType.SURVIVAL);
        owner.setPos(helper.absoluteVec(where.getCenter()));
        return owner;
    }

    /** That player's pet, at heel and with nothing in its hand. */
    private static AbstractPet heeling(GameTestHelper helper, ServerPlayer owner, BlockPos where) {
        AbstractPet pet = wildPet(helper, where);
        pet.tame(owner);
        pet.setPetDirective(PetDirective.FOLLOW);
        return pet;
    }

    /** A zombie that stands where it is, for the pet to be hurt by. */
    private static Zombie zombie(GameTestHelper helper, BlockPos where) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, where);
        zombie.setNoAi(true);
        return zombie;
    }
}
