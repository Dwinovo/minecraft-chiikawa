package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.MIDNIGHT;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;
import static com.dwinovo.chiikawa.gametest.GameTestKit.wildPet;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.brain.combat.PetCombat;
import com.dwinovo.chiikawa.init.InitMemory;
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
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Standing up for its owner, and knowing when not to stand anywhere near something.
 *
 * <p>These are the cases a fight beside a player actually consists of: the pet is at heel
 * rather than off on its own, the thing it is fighting is after the owner rather than the
 * pet, and some of what turns up is not worth walking towards at all.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GuardGameTests {
    private static final String BATCH = "chiikawa_guard";
    private static final int STAND = 2;
    private static final BlockPos HERE = new BlockPos(4, STAND, 4);
    /** The middle of the big floor, with room on every side to back away across. */
    private static final BlockPos MIDDLE = new BlockPos(16, STAND, 16);
    /** Long enough to notice, walk over and land one. */
    private static final int FIGHT_TICKS = 1200;
    /** Long enough to get out of the way of something. */
    private static final int BACK_OFF_TICKS = 200;

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, MIDNIGHT);
    }

    /**
     * The case the whole thing is for: owner and pet walking together, something goes for
     * the owner, and the pet deals with it. A pet at heel used to watch.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = FIGHT_TICKS)
    public static void a_pet_at_heel_fights_what_goes_for_its_owner(GameTestHelper helper) {
        ServerPlayer owner = owner(helper);
        AbstractPet pet = heeling(helper, owner);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(9, STAND, 4));
        zombie.setTarget(owner);

        helper.succeedWhen(() -> helper.assertTrue(hurt(zombie),
            "the pet walked at its owner's heel while a zombie went for them"));
    }

    /** And it weighs in on a fight the owner picked, rather than waiting to be hit. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = FIGHT_TICKS)
    public static void a_pet_joins_the_fight_its_owner_started(GameTestHelper helper) {
        ServerPlayer owner = owner(helper);
        AbstractPet pet = heeling(helper, owner);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(9, STAND, 4));
        zombie.setNoAi(true);
        zombie.hurt(owner.damageSources().playerAttack(owner), 1.0F);
        float afterOwner = zombie.getHealth();

        helper.succeedWhen(() -> helper.assertTrue(zombie.getHealth() < afterOwner,
            "the pet left its owner to it"));
    }

    /**
     * A creeper is not a fight a pet with a sword can win: its reach is a block and a half,
     * well inside the distance at which a creeper starts swelling, so there is no way for
     * it to land a hit without lighting the fuse. It keeps its distance instead.
     */
    @GameTest(template = "floor32", batch = BATCH, timeoutTicks = BACK_OFF_TICKS + 100)
    public static void a_pet_with_a_sword_keeps_away_from_a_creeper(GameTestHelper helper) {
        ServerPlayer owner = owner(helper, MIDDLE);
        AbstractPet pet = heeling(helper, owner, MIDDLE);
        Creeper creeper = helper.spawn(EntityType.CREEPER, MIDDLE.offset(2, 0, 0));
        creeper.setNoAi(true);

        helper.runAtTickTime(BACK_OFF_TICKS, () -> {
            helper.assertTrue(pet.isAlive(), "the pet did not live through standing next to a creeper");
            helper.assertTrue(pet.distanceTo(creeper) >= PetCombat.FUSE_RADIUS - 1.0,
                "the pet stood " + (int) pet.distanceTo(creeper) + " blocks from a creeper"
                    + ", doing " + doing(pet));
            helper.assertTrue(creeper.getHealth() == creeper.getMaxHealth(),
                "the pet picked a fight with a creeper, which is how a pet stops being a pet");
            helper.succeed();
        });
    }

    /** Badly hurt, it breaks off rather than trading blows until it drops. */
    @GameTest(template = "floor32", batch = BATCH, timeoutTicks = BACK_OFF_TICKS + 100)
    public static void a_badly_hurt_pet_breaks_off(GameTestHelper helper) {
        ServerPlayer owner = owner(helper, MIDDLE);
        AbstractPet pet = heeling(helper, owner, MIDDLE);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, MIDDLE.offset(2, 0, 0));
        zombie.setNoAi(true);
        zombie.setTarget(owner);
        pet.setHealth(pet.getMaxHealth() * 0.2F);
        double before = pet.distanceTo(zombie);

        helper.runAtTickTime(BACK_OFF_TICKS, () -> {
            helper.assertTrue(pet.distanceTo(zombie) > before + 2.0,
                "a pet on its last few hearts stayed " + (int) pet.distanceTo(zombie)
                    + " blocks from the zombie, doing " + doing(pet));
            helper.succeed();
        });
    }

    /** An archer with something in its face steps back to where a bow is any use. */
    @GameTest(template = "floor32", batch = BATCH, timeoutTicks = BACK_OFF_TICKS + 100)
    public static void an_archer_backs_up_to_where_its_bow_works(GameTestHelper helper) {
        ServerPlayer owner = owner(helper, MIDDLE);
        AbstractPet pet = holding(heeling(helper, owner, MIDDLE), Items.BOW);
        pet.getBackpack().addItem(new ItemStack(Items.ARROW, 16));
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, MIDDLE.offset(1, 0, 0));
        zombie.setNoAi(true);
        zombie.setTarget(owner);

        helper.runAtTickTime(BACK_OFF_TICKS, () -> {
            helper.assertTrue(pet.distanceTo(zombie) >= PetCombat.BOW_NEAR - 1.0,
                "the archer kept drawing with a zombie " + (int) pet.distanceTo(zombie)
                    + " blocks away, doing " + doing(pet));
            helper.succeed();
        });
    }

    /** Somebody for the pet to walk beside. */
    private static ServerPlayer owner(GameTestHelper helper) {
        return owner(helper, HERE);
    }

    private static ServerPlayer owner(GameTestHelper helper, BlockPos where) {
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        owner.setGameMode(GameType.SURVIVAL);
        owner.setPos(helper.absoluteVec(where.getCenter()));
        return owner;
    }

    /** That player's pet, at heel with a sword. */
    private static AbstractPet heeling(GameTestHelper helper, ServerPlayer owner) {
        return heeling(helper, owner, HERE);
    }

    private static AbstractPet heeling(GameTestHelper helper, ServerPlayer owner, BlockPos where) {
        AbstractPet pet = holding(wildPet(helper, where), Items.IRON_SWORD);
        pet.tame(owner);
        pet.setPetDirective(PetDirective.FOLLOW);
        return pet;
    }

    /** What the pet thinks it is up to, for a failure message that says something. */
    private static String doing(AbstractPet pet) {
        String intent = pet.getBrain().getMemory(InitMemory.CURRENT_INTENT.get())
            .map(running -> running.id().toString())
            .orElse("nothing");
        String walking = pet.getBrain().getMemory(MemoryModuleType.WALK_TARGET)
            .map(target -> target.getTarget().currentBlockPosition().toShortString())
            .orElse("nowhere");
        return intent + " (walking to " + walking + ", target "
            + pet.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).map(t -> t.getName().getString()).orElse("none")
            + ")";
    }

    private static boolean hurt(LivingEntity entity) {
        return entity.isDeadOrDying() || entity.getHealth() < entity.getMaxHealth();
    }
}
