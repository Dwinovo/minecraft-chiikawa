package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.player;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.data.WorkData;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.brain.errand.Outcome;
import com.dwinovo.chiikawa.entity.brain.errand.decide.DecisionLog;
import com.dwinovo.chiikawa.init.InitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A pet that is taken to its owner without walking there: what it was doing is over, and it does not go back to
 * the place it was taken from for it. The way there is the pet's own, see {@code AbstractPet#teleportToOwner}; what is
 * checked is what the pet makes of it.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TeleportGameTests {
    private static final String BATCH = "chiikawa_teleport";
    private static final int STAND = 2;
    /** Long enough for the pet to notice the zombie and be at it. */
    private static final int FIGHT_TICKS = 200;
    /** Longer than the attack sensor takes to look again, and short of a walk back across the yard. */
    private static final int WATCH_TICKS = 40;
    /** Farther than the attack sensor looks. */
    private static final double BEYOND_THE_SENSOR = 16.0;

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /**
     * A fencer fighting a zombie is brought to its owner on the far side of the yard: the fight is over, said to be so
     * because the pet was moved, and the pet does not set off for the zombie it was fighting, which is where it was
     * and no longer where it is. The pet is watched from the moment it has read that it was moved, not from the moment it
     * was: the owner's end of the yard may not be ticking yet while the chunks are new, and a pet there reads nothing.
     */
    @GameTest(template = "floor32", batch = BATCH, timeoutTicks = FIGHT_TICKS + WATCH_TICKS + 20)
    public static void a_pet_brought_to_its_owner_does_not_go_back_for_the_foe_it_was_fighting(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        owner.setGameMode(GameType.SURVIVAL);
        BlockPos far = helper.absolutePos(new BlockPos(29, STAND, 29));
        owner.setPos(far.getX() + 0.5, far.getY(), far.getZ() + 0.5);
        AbstractPet pet = holding(helper.spawn(InitEntity.CHIIKAWA_PET.get(), new BlockPos(4, STAND, 4)), Items.IRON_SWORD);
        pet.tame(owner);
        pet.setPetDirective(PetDirective.FREE);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(7, STAND, 4));
        zombie.setNoAi(true);
        long[] brought = {-1};

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(pet.getMind().errands().isOn(WorkData.MELEE), "the pet never went for the zombie"))
            .thenExecute(() -> {
                helper.assertTrue(pet.teleportToOwner(helper.getLevel(), owner, "test"), "the pet was not brought to its owner");
                brought[0] = helper.getLevel().getGameTime();
            })
            .thenWaitUntil(() -> helper.assertTrue(endedByTheMove(pet, brought[0]),
                "the fight did not end for the pet having been moved: " + pet.getMind().log().entries()))
            .thenExecuteFor(WATCH_TICKS, () -> {
                helper.assertFalse(pet.getMind().errands().isOn(WorkData.MELEE), "the pet went back to the fight it was taken away from");
                helper.assertTrue(pet.distanceTo(zombie) > BEYOND_THE_SENSOR, "the pet is " + (int) pet.distanceTo(zombie)
                    + " blocks from the zombie");
            })
            .thenSucceed();
    }

    /** @return whether the pet has ended the fight for having been moved since the game time */
    private static boolean endedByTheMove(AbstractPet pet, long since) {
        return pet.getMind().log().entries().stream().anyMatch(entry ->
            entry instanceof DecisionLog.Ended ended && ended.errand().equals(WorkData.MELEE)
                && ended.gameTime() >= since && ended.outcome() instanceof Outcome.Interrupted interrupted
                && interrupted.cause().equals("moved"));
    }
}
