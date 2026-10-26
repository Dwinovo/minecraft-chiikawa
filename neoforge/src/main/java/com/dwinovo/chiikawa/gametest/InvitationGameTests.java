package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;
import static com.dwinovo.chiikawa.gametest.GameTestKit.worker;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.data.PetInteractionData;
import com.dwinovo.chiikawa.data.WorkData;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.brain.PetMind;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.init.InitEntity;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What a pet answers when another asks it to play a scene, without the other pet there to ask: the invitations are
 * told to the pet as the asking pet would tell them, and what the pet makes of them is read from what it does and what
 * it says it has said yes to.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InvitationGameTests {
    private static final String BATCH = "chiikawa_invitation";
    private static final int STAND = 2;
    /** Long enough for the pet to read what it is told, answer and decide what to do about it. */
    private static final int ANSWER_TICKS = 5;
    private static final int LONG_WAIT = 400;
    private static final int WORK_TICKS = 3600;

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /**
     * A pet on its way to sow a field is on an errand that matters as much as work does, which is more than a scene: it says
     * no, and goes on sowing, as a pet at work was never pulled off it for a visit.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WORK_TICKS)
    public static void a_pet_at_work_is_not_called_away_to_play(GameTestHelper helper) {
        AbstractPet farmer = holding(worker(helper, new BlockPos(3, STAND, 4)), Items.WOODEN_HOE);
        farmer.getBackpack().addItem(new ItemStack(Items.WHEAT_SEEDS, 8));
        helper.setBlock(new BlockPos(12, STAND - 1, 4),
            Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE));

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(farmer.getMind().errands().isOn(WorkData.PLANT), "the farmer never set off to sow"))
            .thenExecute(() -> ask(helper, farmer, UUID.randomUUID()))
            .thenIdle(ANSWER_TICKS)
            .thenExecuteFor(ANSWER_TICKS, () -> {
                helper.assertTrue(farmer.getMind().errands().engagedWith(now(helper)).isEmpty(), "the farmer said yes to a visit");
                helper.assertFalse(farmer.getMind().errands().isOn(ErrandIds.COOPERATE), "the farmer stopped to play along");
                helper.assertTrue(farmer.getMind().errands().isOn(WorkData.PLANT), "the farmer was called away from its sowing");
            })
            .thenSucceed();
    }

    /** A pet that has said yes to one pet has nothing to say yes to another with: it waits for the first. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = 4 * ANSWER_TICKS + LONG_WAIT)
    public static void a_pet_that_has_said_yes_is_not_asked_away_by_a_second(GameTestHelper helper) {
        AbstractPet chiikawa = GameTestKit.wild(helper, InitEntity.CHIIKAWA_PET.get(), new BlockPos(8, STAND, 8));
        // Told to sit so it never sets off itself, to be the one asking.
        AbstractPet momonga = GameTestKit.owned(helper, InitEntity.MOMONGA_PET.get(), new BlockPos(4, STAND, 8));
        momonga.setPetDirective(PetDirective.STAY);
        UUID first = momonga.getUUID();
        UUID second = UUID.randomUUID();

        helper.startSequence()
            .thenIdle(ANSWER_TICKS)
            .thenExecute(() -> {
                ask(helper, chiikawa, first);
                ask(helper, chiikawa, second);
            })
            .thenIdle(ANSWER_TICKS)
            .thenExecuteFor(ANSWER_TICKS, () -> {
                helper.assertTrue(chiikawa.getMind().errands().engagedWith(now(helper)).filter(first::equals).isPresent(),
                    "Chiikawa did not wait for the pet that asked first");
                helper.assertTrue(chiikawa.getMind().errands().isOn(ErrandIds.COOPERATE), "Chiikawa did not stop to play along");
            })
            .thenSucceed();
    }

    /** Asks the pet to play, as a pet pottering about that has thought of it would, and waits for an answer for a while. */
    private static void ask(GameTestHelper helper, AbstractPet pet, UUID asker) {
        PetMind.post(pet, new Message.Invitation(asker, PetInteractionData.CLING, 0,
            ErrandSettings.of(ErrandIds.SOCIALIZE).orElseThrow().priority(), now(helper) + LONG_WAIT));
    }

    private static long now(GameTestHelper helper) {
        return helper.getLevel().getGameTime();
    }
}
