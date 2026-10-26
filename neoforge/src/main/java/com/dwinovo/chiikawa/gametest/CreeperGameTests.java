package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.owned;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.Creeper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Creepers keep away from the pets the {@code chiikawa:creepers_avoid} tag names, as they do from cats, and from no
 * others. The pets stand still (no AI), so the distance is the creeper's own doing.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CreeperGameTests {
    private static final String BATCH = "chiikawa_creeper";
    private static final int STAND = 2;
    /** Vanilla's cat goal makes a creeper keep 6 blocks away. */
    private static final double AVOID_DISTANCE = 6.0;
    private static final int START_GAP = 4;
    private static final int WATCH_TICKS = 100;

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /** A creeper that finds itself 4 blocks from Hachiware walks away until it is out of the avoid distance. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WATCH_TICKS)
    public static void a_creeper_keeps_away_from_hachiware(GameTestHelper helper) {
        AbstractPet hachiware = standing(helper, InitEntity.HACHIWARE_PET.get());
        Creeper creeper = helper.spawn(EntityType.CREEPER, new BlockPos(8 + START_GAP, STAND, 8));

        helper.succeedWhen(() -> helper.assertTrue(creeper.distanceTo(hachiware) > AVOID_DISTANCE,
            "the creeper stayed within " + AVOID_DISTANCE + " blocks of Hachiware: " + creeper.distanceTo(hachiware)));
    }

    /**
     * Chiikawa is not in the tag. A creeper strolls at random, so where it ends up says little; what is watched is
     * that its avoid goal never takes it over.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WATCH_TICKS + 20)
    public static void a_creeper_does_not_keep_away_from_chiikawa(GameTestHelper helper) {
        standing(helper, InitEntity.CHIIKAWA_PET.get());
        Creeper creeper = helper.spawn(EntityType.CREEPER, new BlockPos(8 + START_GAP, STAND, 8));

        helper.startSequence()
            .thenExecuteFor(WATCH_TICKS, () -> helper.assertFalse(avoiding(creeper), "the creeper kept away from Chiikawa"))
            .thenSucceed();
    }

    private static AbstractPet standing(GameTestHelper helper, EntityType<? extends AbstractPet> type) {
        AbstractPet pet = owned(helper, type, new BlockPos(8, STAND, 8));
        pet.setNoAi(true);
        return pet;
    }

    /** Whether one of the creeper's avoid goals is running, which the goal selector does not let a case ask directly. */
    private static boolean avoiding(Creeper creeper) {
        try {
            java.lang.reflect.Field field = Mob.class.getDeclaredField("goalSelector");
            field.setAccessible(true);
            return ((GoalSelector) field.get(creeper)).getAvailableGoals().stream()
                .anyMatch(wrapped -> wrapped.isRunning() && wrapped.getGoal() instanceof AvoidEntityGoal<?>);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
