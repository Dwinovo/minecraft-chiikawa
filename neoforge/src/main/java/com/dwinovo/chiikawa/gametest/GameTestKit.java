package com.dwinovo.chiikawa.gametest;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.init.InitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

/**
 * What the in-game cases share. They run headless in the game itself
 * ({@code gradlew :neoforge:runGameTestServer}): a pet is spawned the way the game spawns
 * one, given a tool, and left to get on with it while the case watches the world tick by.
 * The exit code is the number of failures, so this reads straight into CI.
 *
 * <p>The cases live here rather than in {@code common} because the game decides which
 * namespace a test belongs to from {@code @GameTestHolder}, which is NeoForge's own
 * annotation — a case declared in common would be filed under {@code minecraft} and
 * dropped by the namespace filter. Nearly everything they exercise is common code all the
 * same; what is loader-shaped — services, networking, registration — is covered by the
 * smoke runs on both loaders.
 *
 * <p>The floors come from the data pack ({@code chiikawa:floor16}, {@code chiikawa:floor8},
 * written by {@code GameTestStructureProvider}), so there is no folder of SNBT beside the
 * run configuration to keep in step.
 */
public final class GameTestKit {
    /** Midday: the hours a pet works. */
    static final long NOON = 6000L;
    /** The dead of night: when mushrooms are picked and a bed is worth having. */
    static final long MIDNIGHT = 18000L;
    /** How far from where a pet fell a case looks for what it dropped. */
    private static final double DROP_SEARCH = 6.0;
    /** Long enough for a batch to run under one sky. */
    private static final int CLEAR_WEATHER_TICKS = 24000;

    private GameTestKit() {
    }

    /**
     * Settles the world a batch runs in. Every batch does this for itself rather than
     * trusting what the last one left: batches run in an order nobody chose, and a peaceful
     * difficulty or a thunderstorm inherited from elsewhere turns a case into a pet standing
     * about doing nothing while the clock runs out.
     */
    static void settleWorld(ServerLevel level, Difficulty difficulty, long dayTime) {
        level.getServer().setDifficulty(difficulty, true);
        level.setDayTime(dayTime);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
        level.setWeatherParameters(CLEAR_WEATHER_TICKS, 0, false, false);
    }

    /**
     * The kind of pet a case about work wants: one whose personality leans towards working.
     * Which pet is not a detail — a personality weighs wandering against working, so a case
     * that spawns a daydreamer fails on the days the pet fancies a stroll. What a lazy pet
     * does is worth a case of its own rather than a coin toss inside everyone else's.
     */
    static AbstractPet worker(GameTestHelper helper, BlockPos rel) {
        return pet(helper, InitEntity.SHISA_PET.get(), rel, true);
    }

    /** The same hard worker, with nobody to answer to. */
    static AbstractPet wildWorker(GameTestHelper helper, BlockPos rel) {
        return pet(helper, InitEntity.SHISA_PET.get(), rel, false);
    }

    /** A wild pet, for a case about what a pet is rather than what it gets done. */
    static AbstractPet wildPet(GameTestHelper helper, BlockPos rel) {
        return pet(helper, InitEntity.USAGI_PET.get(), rel, false);
    }

    /** A tamed pet, likewise. */
    static AbstractPet ownedPet(GameTestHelper helper, BlockPos rel) {
        return pet(helper, InitEntity.USAGI_PET.get(), rel, true);
    }

    /** A wild pet of a particular character, for a case about who that character is. */
    static AbstractPet wild(GameTestHelper helper, EntityType<? extends AbstractPet> type, BlockPos rel) {
        return pet(helper, type, rel, false);
    }

    /** A tamed pet of a particular character, likewise. */
    static AbstractPet owned(GameTestHelper helper, EntityType<? extends AbstractPet> type, BlockPos rel) {
        return pet(helper, type, rel, true);
    }

    /**
     * A player in the case's level, as the server sees one: an owner, a customer, a
     * stranger. Vanilla marks the only way to make one for removal and gives this version
     * nothing to use instead, so every case gets its player here and the day it goes, this
     * is the one line that changes.
     *
     * <p>The player starts in creative mode, and the server cannot send it a mod's own
     * packets: a case about eating or paying sets survival, and a case about a screen calls
     * the handler's logic rather than the handler.
     */
    @SuppressWarnings("removal")
    static ServerPlayer player(GameTestHelper helper) {
        return helper.makeMockServerPlayerInLevel();
    }

    /**
     * Spawned straight onto the floor rather than through the game's own spawning, which
     * would deal the pet a tool from its personality — a case says what a pet holds. Free
     * roaming rather than at heel, because there is no owner walking about to follow, and a
     * pet at heel does not go looking for work.
     */
    static AbstractPet pet(GameTestHelper helper, EntityType<? extends AbstractPet> type,
                                   BlockPos rel, boolean owned) {
        AbstractPet pet = helper.spawn(type, rel);
        pet.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        if (owned) {
            pet.tame(owner(helper));
        }
        pet.setPetDirective(PetDirective.FREE);
        return pet;
    }

    /** Somebody for a pet to belong to. Never spawned: a pet only needs the name on its tag. */
    static Player owner(GameTestHelper helper) {
        return helper.makeMockPlayer(GameType.SURVIVAL);
    }

    /** Puts a tool in the pet's hand, which is what decides the job it takes. */
    static AbstractPet holding(AbstractPet pet, Item tool) {
        pet.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(tool));
        return pet;
    }

    /** Whether the pet has any of this item about it, in hand or in its backpack. */
    static boolean carries(AbstractPet pet, Item item) {
        for (int slot = 0; slot < pet.getBackpack().getContainerSize(); slot++) {
            if (pet.getBackpack().getItem(slot).is(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The doll a dead pet left near {@code rel}, with everything it was carrying written
     * into it.
     */
    static ItemStack dollNear(GameTestHelper helper, BlockPos rel, Item doll) {
        return helper.getLevel()
            .getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(rel)).inflate(DROP_SEARCH))
            .stream()
            .map(ItemEntity::getItem)
            .filter(stack -> stack.is(doll))
            .findFirst()
            .orElseThrow(() -> new AssertionError("the pet died without leaving a doll"));
    }

    /** How many of an item the pet has, for a case that cares whether it got paid twice. */
    static int count(AbstractPet pet, Item item) {
        int found = 0;
        for (int slot = 0; slot < pet.getBackpack().getContainerSize(); slot++) {
            ItemStack stack = pet.getBackpack().getItem(slot);
            if (stack.is(item)) {
                found += stack.getCount();
            }
        }
        return found;
    }
}
