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
    /** Long enough to be sure a pet that was going to do something has not done it. */
    private static final int WATCH_TICKS = 200;

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

    /**
     * A pet told to sit sits, whatever is going on. Fighting is now allowed to a pet at
     * heel, and this is the line that did not move: an owner who says stay has said stay.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WATCH_TICKS + 100)
    public static void a_sitting_pet_stays_out_of_it(GameTestHelper helper) {
        ServerPlayer owner = owner(helper);
        AbstractPet pet = heeling(helper, owner);
        pet.setPetDirective(PetDirective.STAY);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(7, STAND, 4));
        zombie.setNoAi(true);
        zombie.setTarget(owner);

        helper.runAtTickTime(WATCH_TICKS, () -> {
            helper.assertTrue(zombie.getHealth() == zombie.getMaxHealth(),
                "a pet that was told to sit went for the zombie anyway");
            helper.succeed();
        });
    }

    /**
     * The owner's own sword catches the pet in a sweep. It is the commonest way a pet is
     * hurt by something it must never hit back at, and the grudge has to be dropped
     * rather than carried around as a target.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WATCH_TICKS + 100)
    public static void a_pet_does_not_turn_on_its_owner(GameTestHelper helper) {
        ServerPlayer owner = owner(helper);
        AbstractPet pet = heeling(helper, owner);

        pet.hurt(owner.damageSources().playerAttack(owner), 1.0F);

        helper.runAtTickTime(WATCH_TICKS, () -> {
            helper.assertFalse(fighting(pet, owner), "the pet squared up to its own owner");
            helper.assertTrue(owner.getHealth() == owner.getMaxHealth(), "the pet hit its owner");
            helper.succeed();
        });
    }

    /** And not on another of the same owner's pets, whose arrows land the same way. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WATCH_TICKS + 100)
    public static void a_pet_does_not_turn_on_a_sibling(GameTestHelper helper) {
        ServerPlayer owner = owner(helper);
        AbstractPet pet = heeling(helper, owner);
        AbstractPet sibling = heeling(helper, owner, new BlockPos(6, STAND, 4));

        pet.hurt(sibling.damageSources().mobAttack(sibling), 1.0F);

        helper.runAtTickTime(WATCH_TICKS, () -> {
            helper.assertFalse(fighting(pet, sibling), "the pet squared up to its own housemate");
            helper.assertTrue(sibling.getHealth() == sibling.getMaxHealth(), "the pet hit its own housemate");
            helper.succeed();
        });
    }

    /**
     * Picked one, finishes it. Re-choosing the nearest every scan is how a pet in a crowd
     * ends up turning towards something new every couple of seconds and killing nothing.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WATCH_TICKS + 200)
    public static void a_pet_finishes_the_one_it_started_on(GameTestHelper helper) {
        ServerPlayer owner = owner(helper);
        AbstractPet pet = heeling(helper, owner);
        Zombie first = helper.spawn(EntityType.ZOMBIE, new BlockPos(8, STAND, 4));
        first.setNoAi(true);
        // Neither of them can be killed: this case is about which one the pet chooses, and
        // a zombie that falls over mid-case would answer that question for it.
        first.setInvulnerable(true);

        helper.runAtTickTime(40, () -> {
            helper.assertTrue(fighting(pet, first), "the pet never took an interest in the first zombie");
            Zombie nearer = helper.spawn(EntityType.ZOMBIE, new BlockPos(5, STAND, 4));
            nearer.setNoAi(true);
            nearer.setInvulnerable(true);
            helper.runAtTickTime(100, () -> {
                helper.assertTrue(fighting(pet, first),
                    "the pet dropped what it was fighting for whatever wandered closer");
                helper.succeed();
            });
        });
    }

    /** What a bow is for: the creeper a pet with a sword can only walk away from. */
    @GameTest(template = "floor32", batch = BATCH, timeoutTicks = FIGHT_TICKS)
    public static void an_archer_answers_a_creeper_from_outside_the_blast(GameTestHelper helper) {
        ServerPlayer owner = owner(helper, MIDDLE);
        AbstractPet pet = holding(heeling(helper, owner, MIDDLE), Items.BOW);
        pet.getBackpack().addItem(new ItemStack(Items.ARROW, 16));
        Creeper creeper = helper.spawn(EntityType.CREEPER, MIDDLE.offset(8, 0, 0));
        creeper.setNoAi(true);

        helper.succeedWhen(() -> {
            helper.assertTrue(creeper.getHealth() < creeper.getMaxHealth(), "the archer never shot the creeper");
            helper.assertTrue(pet.distanceTo(creeper) >= PetCombat.FUSE_RADIUS - 1.0,
                "the archer walked up to the creeper it was shooting");
        });
    }

    /**
     * Backing off is not the same as giving up: what is on top of the pet still gets hit
     * on the way out. This is the whole point of keeping the feet and the hands apart.
     */
    @GameTest(template = "floor32", batch = BATCH, timeoutTicks = BACK_OFF_TICKS + 100)
    public static void a_pet_backing_off_still_hits_what_is_on_top_of_it(GameTestHelper helper) {
        ServerPlayer owner = owner(helper, MIDDLE);
        AbstractPet pet = heeling(helper, owner, MIDDLE);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, MIDDLE.offset(1, 0, 0));
        zombie.setNoAi(true);
        zombie.setTarget(owner);
        pet.setHealth(pet.getMaxHealth() * 0.2F);

        helper.runAtTickTime(BACK_OFF_TICKS, () -> {
            helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(),
                "the pet backed away without so much as a swing at what was on top of it");
            helper.succeed();
        });
    }

    /** And once it has its health back it goes in again, rather than sulking for good. */
    @GameTest(template = "floor32", batch = BATCH, timeoutTicks = 600)
    public static void a_pet_that_has_healed_goes_back_in(GameTestHelper helper) {
        ServerPlayer owner = owner(helper, MIDDLE);
        AbstractPet pet = heeling(helper, owner, MIDDLE);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, MIDDLE.offset(2, 0, 0));
        zombie.setNoAi(true);
        zombie.setTarget(owner);
        pet.setHealth(pet.getMaxHealth() * 0.2F);

        helper.runAtTickTime(BACK_OFF_TICKS, () -> {
            double away = pet.distanceTo(zombie);
            helper.assertTrue(away > 3.0, "the pet never broke off to begin with, so there is nothing to come back from");
            pet.setHealth(pet.getMaxHealth());
            helper.runAtTickTime(300, () -> {
                helper.assertTrue(pet.distanceTo(zombie) < away - 1.0,
                    "a healed pet stayed out of a fight it had broken off, " + (int) pet.distanceTo(zombie)
                        + " blocks out, doing " + doing(pet));
                helper.succeed();
            });
        });
    }

    /** Whether the pet has settled on this one as the thing it is fighting. */
    private static boolean fighting(AbstractPet pet, LivingEntity foe) {
        return pet.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET)
            .map(target -> target == foe)
            .orElse(false);
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
