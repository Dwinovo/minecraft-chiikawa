package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.MIDNIGHT;
import static com.dwinovo.chiikawa.gametest.GameTestKit.count;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.player;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;
import static com.dwinovo.chiikawa.gametest.GameTestKit.wildWorker;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.data.PetTaskTypeData;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.dwinovo.chiikawa.task.PetTask;
import com.dwinovo.chiikawa.task.PetWorkCounters;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What counts towards a hunting slip. A kill in a real fight is rarely a clean one —
 * the owner is swinging too, arrows are in the air, other pets are helping — and the
 * slip has to agree with what the player watched happen.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HuntGameTests {
    private static final String BATCH = "chiikawa_hunt";
    private static final int STAND = 2;
    /** How many monsters the slip in these cases asks for. */
    private static final int QUARRY = 2;
    /** A scratch: enough to make the owner the one who hurt it last before the pet swings. */
    private static final float SCRATCH = 2.0F;

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, MIDNIGHT);
    }

    /** The pet on its own: it swung, it counted. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 200)
    public static void a_monster_the_pet_puts_down_counts(GameTestHelper helper) {
        AbstractPet pet = hunter(helper);

        felledBy(helper, pet, 0);
        felledBy(helper, pet, 1);

        helper.assertTrue(pet.getTask().isEmpty(), "the slip was not finished by the monsters it asked for");
        helper.assertTrue(count(pet, Items.EMERALD) > 0, "a finished hunting slip paid nothing");
        helper.succeed();
    }

    /**
     * The owner got a hit in first and the pet finished it. This is what a fight beside
     * your own pet looks like, and the kill is still the pet's — it dealt the blow that
     * put the thing down.
     */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 200)
    public static void a_monster_the_owner_softened_up_still_counts(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        owner.setGameMode(GameType.SURVIVAL);
        AbstractPet pet = hunter(helper);
        pet.tame(owner);

        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(5, STAND, 5));
        zombie.hurt(owner.damageSources().playerAttack(owner), SCRATCH);
        zombie.hurt(pet.damageSources().mobAttack(pet), zombie.getMaxHealth() * 2.0F);

        helper.assertTrue(progress(pet) == 1,
            "the pet finished off a monster its owner had hit and the slip counted " + progress(pet));
        helper.succeed();
    }

    /** An arrow of the pet's own counts the same as its sword. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 200)
    public static void a_monster_the_pets_arrow_puts_down_counts(GameTestHelper helper) {
        AbstractPet pet = holding(hunter(helper), Items.BOW);
        pet.getBackpack().addItem(new ItemStack(Items.ARROW, 8));
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(5, STAND, 5));
        zombie.setHealth(1.0F);

        pet.performRangedAttack(zombie, 1.0F);

        helper.succeedWhen(() -> helper.assertTrue(progress(pet) == 1 || pet.getTask().isEmpty(),
            "an arrow of the pet's own killed a monster and the slip counted " + progress(pet)));
    }

    /** Somebody else's kill is somebody else's: a pet counts what it put down itself. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 200)
    public static void a_monster_another_pet_puts_down_does_not_count(GameTestHelper helper) {
        AbstractPet pet = hunter(helper);
        AbstractPet other = wildWorker(helper, new BlockPos(3, STAND, 6));

        felledBy(helper, other, 0);

        helper.assertTrue(progress(pet) == 0, "another pet's kill counted towards this pet's slip");
        helper.succeed();
    }

    /** What a pet was not sent after does not count: a cow is not a monster. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 200)
    public static void a_harmless_animal_does_not_count(GameTestHelper helper) {
        AbstractPet pet = hunter(helper);

        helper.spawn(EntityType.COW, new BlockPos(5, STAND, 5))
            .hurt(pet.damageSources().mobAttack(pet), 100.0F);

        helper.assertTrue(progress(pet) == 0, "a cow counted towards a hunting slip");
        helper.succeed();
    }

    /** And neither does a monster that simply fell over. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 200)
    public static void a_monster_that_dies_of_something_else_does_not_count(GameTestHelper helper) {
        AbstractPet pet = hunter(helper);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(5, STAND, 5));

        zombie.hurt(zombie.damageSources().fall(), zombie.getMaxHealth() * 2.0F);

        helper.assertTrue(progress(pet) == 0, "a monster nobody killed counted towards the slip");
        helper.succeed();
    }

    /** A pet carrying a hunting slip, sword in hand. */
    private static AbstractPet hunter(GameTestHelper helper) {
        AbstractPet pet = holding(wildWorker(helper, new BlockPos(3, STAND, 3)), Items.IRON_SWORD);
        pet.setTask(hunting());
        return pet;
    }

    /** One monster, put down by this pet and nobody else. */
    private static void felledBy(GameTestHelper helper, AbstractPet pet, int offset) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(5 + offset, STAND, 5));
        zombie.hurt(pet.damageSources().mobAttack(pet), zombie.getMaxHealth() * 2.0F);
    }

    /** How far along the pet's slip is; a finished slip has been handed in already. */
    private static int progress(AbstractPet pet) {
        return pet.getTask().map(PetTask::progress).orElse(-1);
    }

    /** The slip an upgraded board puts up for a fencer. */
    private static PetTask hunting() {
        ResourceLocation fencer = InitRegistry.PET_JOB_REGISTRY.getKey(InitRegistry.FENCER.get());
        return new PetTask(PetTaskTypeData.MELEE_HUNTING, fencer, PetWorkCounters.SLAY, PetTask.NO_ICON, QUARRY,
            PetTaskTypeData.reward(PetTaskTypeData.MELEE_HUNTING), 0);
    }
}
