package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.ownedPet;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.intent.IntentSelector;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What looking at a pet's mind does to it: nothing. The rules of choosing are the unit tests';
 * this is the one thing they cannot reach, the pet itself.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SelectorGameTests {
    private static final String BATCH = "chiikawa_selector";
    private static final int STAND = 2;
    private static final long SEED = 20261020L;

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /** The debug command's look at the candidates takes nothing from the pet's random, so the pet acts as if nobody had looked. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 20)
    public static void looking_at_a_pets_candidates_draws_nothing_from_its_random(GameTestHelper helper) {
        AbstractPet pet = holding(ownedPet(helper, new BlockPos(3, STAND, 3)), Items.WOODEN_HOE);

        pet.getRandom().setSeed(SEED);
        IntentSelector.describe(pet);
        long afterLooking = pet.getRandom().nextLong();
        pet.getRandom().setSeed(SEED);
        long untouched = pet.getRandom().nextLong();

        helper.assertTrue(afterLooking == untouched, "looking at the candidates drew from the pet's random");
        helper.succeed();
    }
}
