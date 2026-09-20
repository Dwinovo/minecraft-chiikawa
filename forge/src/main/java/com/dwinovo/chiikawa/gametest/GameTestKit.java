package com.dwinovo.chiikawa.gametest;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.init.InitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;

/**
 * What the in-game cases share. They run headless in the game itself
 * ({@code gradlew :forge:runGameTestServer}): a pet is spawned the way the game spawns
 * one, given a tool, and left to get on with it while the case watches the world tick by.
 * The exit code is the number of failures, so this reads straight into CI.
 *
 * <p>The cases live here rather than in {@code common} because the game decides which
 * namespace a test belongs to from {@code @GameTestHolder}, which is Forge's own
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

    /** A wild pet on the floor at {@code rel}, carrying nothing at all. */
    static AbstractPet wildPet(GameTestHelper helper, BlockPos rel) {
        AbstractPet pet = helper.spawn(InitEntity.USAGI_PET.get(), rel);
        // Spawned straight in rather than through finalizeSpawn: that draws a tool from the
        // pet's personality, and a case is here to say what the pet holds.
        pet.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        pet.setPetDirective(PetDirective.FREE);
        return pet;
    }

    /**
     * A tamed pet on the floor at {@code rel}, free to roam from where it stands. Free
     * roaming rather than following, because a case has no owner walking about for it to
     * follow, and a pet at heel does not go looking for work.
     */
    static AbstractPet ownedPet(GameTestHelper helper, BlockPos rel) {
        AbstractPet pet = helper.spawn(InitEntity.USAGI_PET.get(), rel);
        pet.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        pet.tame(owner(helper));
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
