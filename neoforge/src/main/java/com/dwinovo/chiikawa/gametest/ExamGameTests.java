package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.player;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;
import static com.dwinovo.chiikawa.gametest.GameTestKit.wildPet;
import static com.dwinovo.chiikawa.gametest.GameTestKit.worker;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.block.BoardNotice;
import com.dwinovo.chiikawa.block.ExamResults;
import com.dwinovo.chiikawa.block.LaborBoardBlockEntity;
import com.dwinovo.chiikawa.data.PetTaskTypeData;
import com.dwinovo.chiikawa.data.QualificationData;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.init.InitBlocks;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitTag;
import com.dwinovo.chiikawa.qualification.ExamOpening;
import com.dwinovo.chiikawa.qualification.ExamStage;
import com.dwinovo.chiikawa.qualification.Licence;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The weeding licence, end to end: an owner pays a diamond at a board and every pet of
 * theirs that may sit is called; a called pet sits the exam there and hands a paper in;
 * the morning after it goes back to the board and hears how it did; a pet that cannot go
 * hears by noon wherever it is, and one that never sat down is let off.
 *
 * <p>Each part of the exam is its own batch, since a batch sets the time of day for every
 * case in it: the exam's day, the morning after, and the afternoon after.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ExamGameTests {
    private static final String EXAM_DAY = "chiikawa_exam_day";
    private static final String RESULTS_MORNING = "chiikawa_exam_results";
    private static final String RESULTS_AFTERNOON = "chiikawa_exam_afternoon";
    /** Any day will do: an exam is held the day its owner opens it. */
    private static final long EXAM_DAY_NUMBER = 3L;
    private static final long MORNING = 2000L;
    private static final long AFTERNOON = 8000L;
    /** Long enough to walk to a seat and write a paper, about 26 seconds. */
    private static final int EXAM_TICKS = 1200;
    private static final int RESULTS_TICKS = 600;

    private static final int STAND = 2;

    @BeforeBatch(batch = EXAM_DAY)
    public static void examDay(ServerLevel level) {
        settleWorld(level, Difficulty.PEACEFUL, EXAM_DAY_NUMBER * Level.TICKS_PER_DAY + MORNING);
    }

    @BeforeBatch(batch = RESULTS_MORNING)
    public static void resultsMorning(ServerLevel level) {
        settleWorld(level, Difficulty.PEACEFUL, (EXAM_DAY_NUMBER + 1) * Level.TICKS_PER_DAY + MORNING);
    }

    @BeforeBatch(batch = RESULTS_AFTERNOON)
    public static void resultsAfternoon(ServerLevel level) {
        settleWorld(level, Difficulty.PEACEFUL, (EXAM_DAY_NUMBER + 1) * Level.TICKS_PER_DAY + AFTERNOON);
    }

    /**
     * One diamond for the sitting, however many go: the pets that have practised are called,
     * and one that has not, or is told to stay, is not.
     */
    @GameTest(template = "floor8", batch = EXAM_DAY, timeoutTicks = 100)
    public static void opening_an_exam_calls_every_pet_that_may_sit_for_one_diamond(GameTestHelper helper) {
        BlockPos boardPos = new BlockPos(3, STAND, 1);
        helper.setBlock(boardPos, InitBlocks.LABOR_BOARD.get());
        ServerPlayer owner = player(helper);
        owner.getInventory().add(new ItemStack(Items.DIAMOND, 2));
        AbstractPet first = ownersPet(helper, owner, new BlockPos(1, STAND, 5), true);
        AbstractPet second = ownersPet(helper, owner, new BlockPos(5, STAND, 5), true);
        AbstractPet unpractised = ownersPet(helper, owner, new BlockPos(3, STAND, 6), false);
        AbstractPet staying = ownersPet(helper, owner, new BlockPos(3, STAND, 4), true);
        staying.setPetDirective(PetDirective.STAY);

        Optional<ExamOpening.Refusal> refusal = ExamOpening.open(owner, board(helper, boardPos), QualificationData.WEEDING);

        helper.assertTrue(refusal.isEmpty(), "the exam was not opened: " + refusal);
        helper.assertTrue(called(first) && called(second), "a practised pet was not called");
        helper.assertFalse(called(unpractised), "a pet that has not practised was called");
        helper.assertFalse(called(staying), "a pet told to stay was called");
        helper.assertTrue(owner.getInventory().countItem(Items.DIAMOND) == 1, "the fee was not one diamond");
        helper.succeed();
    }

    /** An owner with nobody to send pays nothing. */
    @GameTest(template = "floor8", batch = EXAM_DAY, timeoutTicks = 100)
    public static void nobody_to_call_costs_nothing(GameTestHelper helper) {
        BlockPos boardPos = new BlockPos(3, STAND, 1);
        helper.setBlock(boardPos, InitBlocks.LABOR_BOARD.get());
        ServerPlayer owner = player(helper);
        owner.getInventory().add(new ItemStack(Items.DIAMOND, 1));
        ownersPet(helper, owner, new BlockPos(3, STAND, 5), false);

        Optional<ExamOpening.Refusal> refusal = ExamOpening.open(owner, board(helper, boardPos), QualificationData.WEEDING);

        helper.assertTrue(refusal.equals(Optional.of(ExamOpening.Refusal.NOBODY)), "refused for " + refusal);
        helper.assertTrue(owner.getInventory().countItem(Items.DIAMOND) == 1, "a diamond went on an empty room");
        helper.succeed();
    }

    /** Called: it takes a seat at the board and hands a paper in, kept for the morning. */
    @GameTest(template = "floor16", batch = EXAM_DAY, timeoutTicks = EXAM_TICKS)
    public static void a_called_pet_sits_the_exam_and_hands_a_paper_in(GameTestHelper helper) {
        BlockPos boardPos = new BlockPos(8, STAND, 3);
        helper.setBlock(boardPos, InitBlocks.LABOR_BOARD.get());
        AbstractPet pet = worker(helper, new BlockPos(8, STAND, 11));
        pet.licences().set(QualificationData.WEEDING, Licence.NONE.practised().called(at(helper, boardPos), EXAM_DAY_NUMBER));

        helper.succeedWhen(() -> {
            Licence licence = pet.licences().get(QualificationData.WEEDING);
            helper.assertTrue(licence.paper().isPresent(), "the pet has not handed a paper in");
            helper.assertTrue(licence.held() == 0, "the result was heard on the day of the exam");
            helper.assertTrue(board(helper, boardPos).exam().results().posted(EXAM_DAY_NUMBER + 1).stream()
                    .anyMatch(sitting -> sitting.pet().equals(pet.getUUID()) && sitting.rank() == 5),
                "the board has not kept the paper to post in the morning");
        });
    }

    /** Called away before it hands in: it gives its seat up and has sat nothing. */
    @GameTest(template = "floor16", batch = EXAM_DAY, timeoutTicks = EXAM_TICKS)
    public static void a_pet_called_away_from_the_exam_has_sat_nothing(GameTestHelper helper) {
        BlockPos boardPos = new BlockPos(8, STAND, 3);
        helper.setBlock(boardPos, InitBlocks.LABOR_BOARD.get());
        AbstractPet pet = worker(helper, new BlockPos(8, STAND, 11));
        pet.licences().set(QualificationData.WEEDING, Licence.NONE.practised().called(at(helper, boardPos), EXAM_DAY_NUMBER));

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(pet.getActivity() == PetActivity.EXAM,
                "the pet never sat down to the paper"))
            .thenExecute(() -> pet.setPetDirective(PetDirective.STAY))
            .thenIdle(40)
            .thenExecute(() -> {
                helper.assertTrue(pet.licences().get(QualificationData.WEEDING).paper().isEmpty(),
                    "a pet called away still handed a paper in");
                helper.assertFalse(board(helper, boardPos).exam().seats().anyTaken(helper.getLevel().getGameTime()),
                    "the seat was not given up");
            })
            .thenSucceed();
    }

    /** While an exam opened at a board is on, the board has the exam notice pinned up. */
    @GameTest(template = "floor8", batch = EXAM_DAY, timeoutTicks = 100)
    public static void a_board_pins_up_the_exam_notice_while_an_exam_is_on(GameTestHelper helper) {
        BlockPos boardPos = new BlockPos(3, STAND, 3);
        helper.setBlock(boardPos, InitBlocks.LABOR_BOARD.get());
        LaborBoardBlockEntity board = board(helper, boardPos);
        helper.assertTrue(board.notice() == BoardNotice.NONE, "a board with no exam on has a notice up");
        board.exam().opened(EXAM_DAY_NUMBER);
        board.examChanged();

        helper.succeedWhen(() -> helper.assertTrue(board.notice() == BoardNotice.EXAM,
            "the board has no exam notice up while an exam is on"));
    }

    /** The morning after it goes back to the board it sat at, reads the results and hears it passed. */
    @GameTest(template = "floor16", batch = RESULTS_MORNING, timeoutTicks = RESULTS_TICKS)
    public static void the_morning_after_a_pet_sees_its_results_at_the_board(GameTestHelper helper) {
        BlockPos boardPos = new BlockPos(8, STAND, 3);
        helper.setBlock(boardPos, InitBlocks.LABOR_BOARD.get());
        AbstractPet pet = worker(helper, new BlockPos(8, STAND, 11));
        pet.licences().set(QualificationData.WEEDING, new Licence(0, 0, 0, false,
            new ExamStage.Sat(at(helper, boardPos), EXAM_DAY_NUMBER, true)));

        helper.succeedWhen(() -> {
            Licence licence = pet.licences().get(QualificationData.WEEDING);
            helper.assertTrue(licence.held() == 1, "the pet has not heard it passed grade 5");
            BlockPos board = helper.absolutePos(boardPos);
            helper.assertTrue(pet.distanceToSqr(board.getX() + 0.5, pet.getY(), board.getZ() + 0.5) < 5.0 * 5.0,
                "the pet heard its results without going to the board");
        });
    }

    /** The morning after, a board where an exam was sat has the results pinned up. */
    @GameTest(template = "floor8", batch = RESULTS_MORNING, timeoutTicks = 100)
    public static void a_board_pins_up_the_results_the_morning_after(GameTestHelper helper) {
        BlockPos boardPos = new BlockPos(3, STAND, 3);
        helper.setBlock(boardPos, InitBlocks.LABOR_BOARD.get());
        LaborBoardBlockEntity board = board(helper, boardPos);
        board.exam().results().record(new ExamResults.Sitting(UUID.randomUUID(), "Chiikawa", QualificationData.WEEDING,
            5, true, EXAM_DAY_NUMBER));
        board.examChanged();

        helper.succeedWhen(() -> helper.assertTrue(board.notice() == BoardNotice.RESULTS,
            "the board has no results up the morning after an exam"));
    }

    /** Called yesterday and never sat down: let off, and free to be called again. */
    @GameTest(template = "floor8", batch = RESULTS_MORNING, timeoutTicks = 100)
    public static void a_pet_that_missed_its_exam_is_let_off(GameTestHelper helper) {
        BlockPos boardPos = new BlockPos(3, STAND, 1);
        helper.setBlock(boardPos, InitBlocks.LABOR_BOARD.get());
        AbstractPet pet = worker(helper, new BlockPos(3, STAND, 5));
        pet.licences().set(QualificationData.WEEDING, Licence.NONE.practised().called(at(helper, boardPos), EXAM_DAY_NUMBER));

        helper.succeedWhen(() -> {
            Licence licence = pet.licences().get(QualificationData.WEEDING);
            helper.assertTrue(licence.exam() instanceof ExamStage.None, "the pet is still called to yesterday's exam");
            helper.assertTrue(licence.practice() == 1, "missing the exam cost the pet its practice");
        });
    }

    /** One told to sit hears by noon where it sits: a fail, and one more fail to try again after. */
    @GameTest(template = "floor8", batch = RESULTS_AFTERNOON, timeoutTicks = 100)
    public static void a_pet_that_missed_the_morning_hears_its_results_where_it_is(GameTestHelper helper) {
        AbstractPet pet = worker(helper, new BlockPos(3, STAND, 3));
        pet.setPetDirective(PetDirective.STAY);
        pet.licences().set(QualificationData.WEEDING, new Licence(0, 0, 1, false,
            new ExamStage.Sat(at(helper, new BlockPos(3, STAND, 1)), EXAM_DAY_NUMBER, false)));

        helper.succeedWhen(() -> {
            Licence licence = pet.licences().get(QualificationData.WEEDING);
            helper.assertTrue(licence.paper().isEmpty(), "the pet has not heard its results");
            helper.assertTrue(licence.held() == 0 && licence.fails() == 2, "a fail was not counted");
        });
    }

    /** Read before the exam: the book is spent. A pet that has read one and not sat since takes no other. */
    @GameTest(template = "floor8", batch = EXAM_DAY, timeoutTicks = 100)
    public static void a_pet_reads_the_book_once_before_each_exam(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));
        pet.tame(owner);
        // A mock player turns up in creative, where nothing in a hand is ever spent.
        owner.setGameMode(GameType.SURVIVAL);
        owner.setShiftKeyDown(false);
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(InitItems.WEEDING_BOOK.get(), 2));

        pet.mobInteract(owner, InteractionHand.MAIN_HAND);
        helper.assertTrue(pet.licences().get(QualificationData.WEEDING).read(), "the pet did not read the book");
        helper.assertTrue(owner.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1, "the book was not spent");

        pet.mobInteract(owner, InteractionHand.MAIN_HAND);
        helper.assertTrue(owner.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1,
            "a pet that had read the book already took another");
        helper.succeed();
    }

    /** A licensed pet is paid more for the same slip: a coin more for each grade it holds. */
    @GameTest(template = "floor8", batch = EXAM_DAY, timeoutTicks = 100)
    public static void a_licensed_pet_is_paid_more_for_weeding(GameTestHelper helper) {
        AbstractPet unlicensed = worker(helper, new BlockPos(2, STAND, 2));
        AbstractPet gradeOne = worker(helper, new BlockPos(5, STAND, 5));
        gradeOne.licences().set(QualificationData.WEEDING, Licence.holding(5));

        // The weeding slip pays one or two coins; five grades add a coin each.
        int plain = weedingPay(helper, unlicensed);
        int licensed = weedingPay(helper, gradeOne);
        helper.assertTrue(plain >= 1 && plain <= 2, "a pet with no licence was paid " + plain);
        helper.assertTrue(licensed >= 6 && licensed <= 7, "a grade 1 pet was paid " + licensed);
        helper.succeed();
    }

    /** A pet of {@code owner}'s left to itself, that has done a weeding slip or not. */
    private static AbstractPet ownersPet(GameTestHelper helper, ServerPlayer owner, BlockPos rel, boolean practised) {
        AbstractPet pet = wildPet(helper, rel);
        pet.tame(owner);
        pet.setPetDirective(PetDirective.FREE);
        if (practised) {
            pet.licences().set(QualificationData.WEEDING, Licence.NONE.practised());
        }
        return pet;
    }

    private static boolean called(AbstractPet pet) {
        return pet.licences().get(QualificationData.WEEDING).call().isPresent();
    }

    /** What a finished weeding slip pays this pet, rolled as a finished slip is. */
    private static int weedingPay(GameTestHelper helper, AbstractPet pet) {
        ServerLevel level = helper.getLevel();
        LootTable reward = level.getServer().reloadableRegistries().getLootTable(PetTaskTypeData.reward(PetTaskTypeData.WEEDING));
        LootParams params = new LootParams.Builder(level)
            .withParameter(LootContextParams.ORIGIN, pet.position())
            .withParameter(LootContextParams.THIS_ENTITY, pet)
            .create(LootContextParamSets.GIFT);
        return reward.getRandomItems(params).stream()
            .filter(stack -> stack.is(InitTag.CURRENCY))
            .mapToInt(ItemStack::getCount)
            .sum();
    }

    private static GlobalPos at(GameTestHelper helper, BlockPos rel) {
        return GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(rel));
    }

    private static LaborBoardBlockEntity board(GameTestHelper helper, BlockPos pos) {
        return (LaborBoardBlockEntity) helper.getBlockEntity(pos);
    }
}
