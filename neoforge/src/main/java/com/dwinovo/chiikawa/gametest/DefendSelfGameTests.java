package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.MIDNIGHT;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.ownedPet;
import static com.dwinovo.chiikawa.gametest.GameTestKit.player;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;
import static com.dwinovo.chiikawa.gametest.GameTestKit.wildPet;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.PetRules;
import com.dwinovo.chiikawa.entity.brain.PetMind;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.init.InitRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A pet with a hoe in its hand and a sword in its bag that is hit takes the sword up, fights, and takes the hoe back: a
 * swap that lasts for the fight and no longer, and is never saved.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DefendSelfGameTests {
    private static final String BATCH = "chiikawa_defend";
    private static final int STAND = 2;
    private static final BlockPos HERE = new BlockPos(4, STAND, 4);
    /** The pocket of the backpack the spare sword is in. */
    private static final int POCKET = 2;
    /** Long enough to be hurt, know it, take the sword up, walk over and land the blow. */
    private static final int FIGHT_TICKS = 600;
    /** Long enough to be sure a pet that was going to give something back has done so. */
    private static final int WATCH_TICKS = 40;

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, MIDNIGHT);
    }

    /**
     * Hit by a zombie, a farmer takes the sword out of its bag, fights as a fencer does, and when the zombie has fallen
     * has its hoe in its hand again and the sword in its bag.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = FIGHT_TICKS)
    public static void a_farmer_hit_draws_the_sword_from_its_bag_and_takes_its_hoe_back(GameTestHelper helper) {
        AbstractPet pet = farmerWithASword(helper);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(8, STAND, 4));
        zombie.setNoAi(true);
        zombie.setHealth(1.0F);
        pet.hurt(zombie.damageSources().mobAttack(zombie), 1.0F);

        helper.startSequence()
            .thenWaitUntil(() -> {
                helper.assertTrue(pet.getMainHandItem().is(Items.IRON_SWORD), "the farmer never took the sword up, doing " + pet.getIntent());
                helper.assertTrue(pet.getMind().errands().isOn(ErrandIds.DEFEND_SELF), "the farmer holds the sword and is doing " + pet.getIntent());
                helper.assertTrue(pet.getPetJobId() == InitRegistry.FENCER_ID, "the sword did not make a fencer of the farmer");
            })
            .thenWaitUntil(() -> helper.assertFalse(zombie.isAlive(), "the farmer never put the zombie down"))
            .thenWaitUntil(() -> {
                helper.assertTrue(pet.getMainHandItem().is(Items.WOODEN_HOE), "the farmer did not take its hoe back, holding " + pet.getMainHandItem());
                helper.assertTrue(pet.getBackpack().getItem(POCKET).is(Items.IRON_SWORD), "the sword is not back in the bag");
                helper.assertTrue(pet.getPetJobId() == InitRegistry.FARMER_ID, "the farmer is not a farmer again");
            })
            .thenExecuteFor(WATCH_TICKS, () -> helper.assertTrue(pet.getMainHandItem().is(Items.WOODEN_HOE), "the farmer took the sword up again"))
            .thenSucceed();
    }

    /**
     * A pet saved in the middle of the fight is saved with its hoe in its hand and the sword in the bag, as it was before,
     * and so is the pet that is read from the save; the pet in the world goes on holding the sword.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = FIGHT_TICKS)
    public static void a_farmer_saved_mid_fight_is_saved_holding_its_hoe(GameTestHelper helper) {
        AbstractPet pet = farmerWithASword(helper);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(8, STAND, 4));
        zombie.setNoAi(true);
        // The fight is what is saved, and a zombie that falls would end it.
        zombie.setInvulnerable(true);
        pet.hurt(zombie.damageSources().mobAttack(zombie), 1.0F);

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(pet.getMainHandItem().is(Items.IRON_SWORD) && pet.getMind().errands().isOn(ErrandIds.DEFEND_SELF),
                "the farmer never took the sword up, doing " + pet.getIntent()))
            .thenExecute(() -> {
                CompoundTag saved = new CompoundTag();
                pet.save(saved);
                NonNullList<ItemStack> kept = NonNullList.withSize(AbstractPet.FULL_BACKPACK_SIZE, ItemStack.EMPTY);
                ContainerHelper.loadAllItems(saved.getCompound("Backpack"), kept, helper.getLevel().registryAccess());
                helper.assertTrue(kept.get(AbstractPet.MAINHAND_SLOT).is(Items.WOODEN_HOE), "the save has " + kept.get(AbstractPet.MAINHAND_SLOT) + " in the hand");
                helper.assertTrue(kept.get(POCKET).is(Items.IRON_SWORD), "the save has " + kept.get(POCKET) + " where the sword is kept");

                AbstractPet loaded = (AbstractPet) EntityType.create(saved, helper.getLevel()).orElseThrow();
                helper.assertTrue(loaded.getMainHandItem().is(Items.WOODEN_HOE), "the pet read from the save holds " + loaded.getMainHandItem());
                helper.assertTrue(loaded.getPetJobId() == InitRegistry.FARMER_ID, "the pet read from the save is not a farmer");
                helper.assertTrue(pet.getMainHandItem().is(Items.IRON_SWORD), "saving took the sword out of the pet's hand");
            })
            .thenSucceed();
    }

    /**
     * A present in the paws waits for the fight: the sword is taken up before the present is, though both are the pet's to
     * do at once, and when the zombie has fallen the present is taken to the owner.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = FIGHT_TICKS + 400)
    public static void a_carried_present_waits_for_the_fight(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        owner.setGameMode(GameType.SURVIVAL);
        owner.setPos(helper.absoluteVec(new BlockPos(12, STAND, 4).getCenter()));
        AbstractPet pet = holding(wildPet(helper, HERE), Items.WOODEN_HOE);
        pet.tame(owner);
        pet.setPetDirective(PetDirective.FREE);
        pet.getBackpack().setItem(POCKET, new ItemStack(Items.IRON_SWORD));
        pet.setPendingGift(new ItemStack(Items.COOKIE));
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(7, STAND, 4));
        zombie.setNoAi(true);
        zombie.setInvulnerable(true);
        // In the fight from the first tick, with the zombie on top of the pet and the present in its paws, what the sensor
        // and the game's own hurt-by memory do a moment later anyway.
        pet.getBrain().setMemory(MemoryModuleType.HURT_BY_ENTITY, zombie);
        pet.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, zombie);
        PetMind.nudge(pet, "test");

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(pet.getMind().errands().isOn(ErrandIds.DEFEND_SELF),
                "the pet went for something else than the fight, doing " + pet.getIntent()))
            .thenExecuteFor(WATCH_TICKS, () -> {
                helper.assertTrue(pet.getMind().errands().isOn(ErrandIds.DEFEND_SELF), "the present took the pet from the fight");
                helper.assertFalse(pet.getPendingGift().isEmpty(), "the present was given in the middle of the fight");
            })
            .thenExecute(() -> {
                zombie.setInvulnerable(false);
                zombie.kill();
            })
            .thenWaitUntil(() -> {
                helper.assertTrue(pet.getPendingGift().isEmpty(), "the present was not taken to the owner after the fight");
                helper.assertTrue(owner.getInventory().countItem(Items.COOKIE) > 0, "the owner was not given the present");
            })
            .thenSucceed();
    }

    /**
     * An owner who has said not to swap gets a pet that stays with its hoe: hit, it has no weapon to take up, and nothing to
     * fight with, so it runs.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = FIGHT_TICKS)
    public static void a_farmer_told_not_to_swap_keeps_its_hoe_and_its_sword_in_the_bag(GameTestHelper helper) {
        AbstractPet pet = farmerWithASword(helper);
        pet.setRules(PetRules.DEFAULT.withSwap(false));
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(8, STAND, 4));
        zombie.setNoAi(true);
        pet.hurt(zombie.damageSources().mobAttack(zombie), 1.0F);

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(pet.getMind().errands().isOn(ErrandIds.FLEE), "the pet did not run, doing " + pet.getIntent()))
            .thenExecuteFor(WATCH_TICKS, () -> {
                helper.assertTrue(pet.getMainHandItem().is(Items.WOODEN_HOE), "the pet swapped though it was told not to");
                helper.assertTrue(zombie.getHealth() == zombie.getMaxHealth(), "the pet fought with a hoe");
            })
            .thenSucceed();
    }

    /** A farmer at its owner's heel with a hoe in its hand and a sword in its bag; its owner is not in the world. */
    private static AbstractPet farmerWithASword(GameTestHelper helper) {
        AbstractPet pet = holding(ownedPet(helper, HERE), Items.WOODEN_HOE);
        pet.getBackpack().setItem(POCKET, new ItemStack(Items.IRON_SWORD));
        return pet;
    }
}
