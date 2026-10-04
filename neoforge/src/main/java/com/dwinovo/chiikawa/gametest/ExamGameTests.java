package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.player;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;
import static com.dwinovo.chiikawa.gametest.GameTestKit.wildPet;
import static com.dwinovo.chiikawa.gametest.GameTestKit.worker;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.block.DeskBooking;
import com.dwinovo.chiikawa.block.DeskPart;
import com.dwinovo.chiikawa.block.DeskSheet;
import com.dwinovo.chiikawa.block.ExamDeskBlock;
import com.dwinovo.chiikawa.block.ExamDeskBlockEntity;
import com.dwinovo.chiikawa.data.PetTaskTypeData;
import com.dwinovo.chiikawa.data.QualificationData;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.init.InitBlocks;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitTag;
import com.dwinovo.chiikawa.qualification.ExamEnrollment;
import com.dwinovo.chiikawa.qualification.ExamStage;
import com.dwinovo.chiikawa.qualification.Ineligible;
import com.dwinovo.chiikawa.qualification.Licence;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * The weeding licence, end to end: an owner signs one pet up at an exam desk for a diamond;
 * the pet sits at the desk and hands a paper in; the morning after it goes back to the desk
 * and hears how it did. A desk takes one pet at a time; a pet that has not practised or is
 * told to stay cannot be signed up; breaking the desk lets its pet off, and a pet whose desk
 * is gone hears its results where it is. A desk comes with its chair, and goes with it.
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
    /** Any day will do: an exam is sat the day its pet is signed up. */
    private static final long EXAM_DAY_NUMBER = 3L;
    private static final long MORNING = 2000L;
    private static final long AFTERNOON = 8000L;
    /** Long enough to walk to the desk and write a paper, about 26 seconds. */
    private static final int EXAM_TICKS = 1200;
    private static final int RESULTS_TICKS = 600;

    private static final int STAND = 2;
    /** The desk faces north, so its pet sits on the block south of it. */
    private static final BlockPos DESK = new BlockPos(4, STAND, 2);

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

    /** One diamond, one pet: it is sent to the desk, and the desk is booked for it. */
    @GameTest(template = "floor8", batch = EXAM_DAY, timeoutTicks = 100)
    public static void signing_a_pet_up_takes_a_diamond_and_books_the_desk(GameTestHelper helper) {
        ExamDeskBlockEntity desk = placeDesk(helper);
        ServerPlayer owner = player(helper);
        owner.getInventory().add(new ItemStack(Items.DIAMOND, 2));
        AbstractPet pet = ownersPet(helper, owner, new BlockPos(4, STAND, 6), true);

        Optional<ExamEnrollment.Refusal> refusal = ExamEnrollment.signUp(owner, desk, QualificationData.WEEDING, pet.getUUID());

        helper.assertTrue(refusal.isEmpty(), Component.literal("the pet was not signed up: " + refusal));
        helper.assertTrue(pet.licences().get(QualificationData.WEEDING).call()
            .filter(call -> call.desk().equals(at(helper, DESK))).isPresent(), Component.literal("the pet was not sent to the desk"));
        helper.assertTrue(desk.booking().filter(booking -> booking.pet().equals(pet.getUUID())).isPresent(),
            Component.literal("the desk was not booked for the pet"));
        helper.assertTrue(desk.sheet() == DeskSheet.ANSWER, Component.literal("the desk has no answer sheet on it"));
        helper.assertTrue(owner.getInventory().countItem(Items.DIAMOND) == 1, Component.literal("the fee was not one diamond"));
        helper.succeed();
    }

    /** A second pet cannot be signed up at a desk another pet has, and its owner pays nothing. */
    @GameTest(template = "floor8", batch = EXAM_DAY, timeoutTicks = 100)
    public static void a_desk_takes_one_pet_at_a_time(GameTestHelper helper) {
        ExamDeskBlockEntity desk = placeDesk(helper);
        ServerPlayer owner = player(helper);
        owner.getInventory().add(new ItemStack(Items.DIAMOND, 2));
        AbstractPet first = ownersPet(helper, owner, new BlockPos(2, STAND, 6), true);
        AbstractPet second = ownersPet(helper, owner, new BlockPos(6, STAND, 6), true);

        ExamEnrollment.signUp(owner, desk, QualificationData.WEEDING, first.getUUID());
        Optional<ExamEnrollment.Refusal> refusal = ExamEnrollment.signUp(owner, desk, QualificationData.WEEDING, second.getUUID());

        helper.assertTrue(refusal.equals(Optional.of(ExamEnrollment.Refusal.TAKEN)), Component.literal("refused for " + refusal));
        helper.assertTrue(second.licences().get(QualificationData.WEEDING).call().isEmpty(), Component.literal("the second pet was sent too"));
        helper.assertTrue(owner.getInventory().countItem(Items.DIAMOND) == 1, Component.literal("the second sign-up was paid for"));
        helper.succeed();
    }

    /** The desk says why a pet cannot go: one has not practised, one is told to stay. Neither costs anything. */
    @GameTest(template = "floor8", batch = EXAM_DAY, timeoutTicks = 100)
    public static void a_pet_that_has_not_practised_or_is_staying_cannot_be_signed_up(GameTestHelper helper) {
        ExamDeskBlockEntity desk = placeDesk(helper);
        ServerPlayer owner = player(helper);
        owner.getInventory().add(new ItemStack(Items.DIAMOND, 1));
        AbstractPet unpractised = ownersPet(helper, owner, new BlockPos(2, STAND, 6), false);
        AbstractPet staying = ownersPet(helper, owner, new BlockPos(6, STAND, 6), true);
        staying.setPetDirective(PetDirective.STAY);

        ExamEnrollment.Offer offer = ExamEnrollment.offers(owner, desk).get(0);
        helper.assertTrue(whyNot(offer, unpractised).equals(Optional.of(Ineligible.UNPRACTISED)),
            Component.literal("an unpractised pet was offered for " + whyNot(offer, unpractised)));
        helper.assertTrue(whyNot(offer, staying).equals(Optional.of(Ineligible.STAYING)),
            Component.literal("a staying pet was offered for " + whyNot(offer, staying)));
        helper.assertTrue(ExamEnrollment.signUp(owner, desk, QualificationData.WEEDING, unpractised.getUUID())
            .equals(Optional.of(ExamEnrollment.Refusal.INELIGIBLE)), Component.literal("an unpractised pet was signed up"));
        helper.assertTrue(owner.getInventory().countItem(Items.DIAMOND) == 1, Component.literal("a refused sign-up was paid for"));
        helper.succeed();
    }

    /** Signed up: it sits at the desk and hands a paper in, kept for the morning. */
    @GameTest(template = "floor8", batch = EXAM_DAY, timeoutTicks = EXAM_TICKS)
    public static void a_signed_up_pet_sits_at_the_desk_and_hands_a_paper_in(GameTestHelper helper) {
        ExamDeskBlockEntity desk = placeDesk(helper);
        AbstractPet pet = signedUp(helper, desk, new BlockPos(4, STAND, 6));

        helper.succeedWhen(() -> {
            Licence licence = pet.licences().get(QualificationData.WEEDING);
            helper.assertTrue(licence.paper().isPresent(), Component.literal("the pet has not handed a paper in"));
            helper.assertTrue(licence.held() == 0, Component.literal("the result was heard on the day of the exam"));
            helper.assertTrue(desk.booking().flatMap(DeskBooking::passed).isPresent(),
                Component.literal("the desk has not kept the result for the morning"));
        });
    }

    /** Breaking the desk while its pet is at the paper: the pet is let off, and has sat nothing. */
    @GameTest(template = "floor8", batch = EXAM_DAY, timeoutTicks = EXAM_TICKS)
    public static void breaking_the_desk_lets_its_pet_off(GameTestHelper helper) {
        ExamDeskBlockEntity desk = placeDesk(helper);
        AbstractPet pet = signedUp(helper, desk, new BlockPos(4, STAND, 6));

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(pet.getActivity() == PetActivity.EXAM,
                Component.literal("the pet never sat down to the paper")))
            .thenExecute(() -> helper.destroyBlock(DESK))
            .thenWaitUntil(() -> helper.assertTrue(
                pet.licences().get(QualificationData.WEEDING).exam() instanceof ExamStage.None,
                Component.literal("the pet is still signed up at a desk that is gone")))
            .thenExecute(() -> helper.assertTrue(pet.licences().get(QualificationData.WEEDING).practice() == 1,
                Component.literal("losing the desk cost the pet its practice")))
            .thenSucceed();
    }

    /** Breaking the chair takes the desk with it, as breaking either half of a bed takes the other. */
    @GameTest(template = "floor8", batch = EXAM_DAY, timeoutTicks = 100)
    public static void breaking_the_chair_takes_the_desk_with_it(GameTestHelper helper) {
        placeDesk(helper);
        helper.destroyBlock(ExamDeskBlock.chair(DESK, Direction.NORTH));

        helper.succeedWhen(() -> helper.assertBlockNotPresent(InitBlocks.EXAM_DESK.get(), DESK));
    }

    /** The morning after it goes back to its desk, reads the results laid on it, and hears it passed. */
    @GameTest(template = "floor8", batch = RESULTS_MORNING, timeoutTicks = RESULTS_TICKS)
    public static void the_morning_after_a_pet_reads_its_results_at_the_desk(GameTestHelper helper) {
        ExamDeskBlockEntity desk = placeDesk(helper);
        AbstractPet pet = worker(helper, new BlockPos(4, STAND, 6));
        pet.licences().set(QualificationData.WEEDING, new Licence(0, 0, 0, false,
            new ExamStage.Sat(at(helper, DESK), EXAM_DAY_NUMBER, true)));
        desk.book(new DeskBooking(pet.getUUID(), "Shisa", QualificationData.WEEDING, 5, EXAM_DAY_NUMBER, Optional.of(true)));
        helper.assertTrue(desk.sheet() == DeskSheet.PASSED, Component.literal("the desk does not have the results on it"));

        helper.succeedWhen(() -> {
            helper.assertTrue(pet.licences().get(QualificationData.WEEDING).held() == 1,
                Component.literal("the pet has not heard it passed grade 5"));
            BlockPos chair = helper.absolutePos(ExamDeskBlock.chair(DESK, Direction.NORTH));
            helper.assertTrue(pet.distanceToSqr(chair.getX() + 0.5, pet.getY(), chair.getZ() + 0.5) < 2.0 * 2.0,
                Component.literal("the pet heard its results without going to the desk"));
        });
    }

    /** Signed up yesterday and never sat down: let off, and free to be signed up again. */
    @GameTest(template = "floor8", batch = RESULTS_MORNING, timeoutTicks = 100)
    public static void a_pet_that_missed_its_exam_is_let_off(GameTestHelper helper) {
        placeDesk(helper);
        AbstractPet pet = worker(helper, new BlockPos(4, STAND, 6));
        pet.licences().set(QualificationData.WEEDING, Licence.NONE.practised().called(at(helper, DESK), EXAM_DAY_NUMBER));

        helper.succeedWhen(() -> {
            Licence licence = pet.licences().get(QualificationData.WEEDING);
            helper.assertTrue(licence.exam() instanceof ExamStage.None, Component.literal("the pet is still signed up for yesterday's exam"));
            helper.assertTrue(licence.practice() == 1, Component.literal("missing the exam cost the pet its practice"));
        });
    }

    /** Its desk is gone by the morning: it hears its results where it is, without waiting for noon. */
    @GameTest(template = "floor8", batch = RESULTS_MORNING, timeoutTicks = 100)
    public static void a_pet_whose_desk_is_gone_hears_its_results_where_it_is(GameTestHelper helper) {
        AbstractPet pet = worker(helper, new BlockPos(4, STAND, 6));
        pet.licences().set(QualificationData.WEEDING, new Licence(0, 0, 0, false,
            new ExamStage.Sat(at(helper, DESK), EXAM_DAY_NUMBER, true)));

        helper.succeedWhen(() -> helper.assertTrue(pet.licences().get(QualificationData.WEEDING).held() == 1,
            Component.literal("the pet has not heard its results")));
    }

    /** One told to sit hears by noon where it sits: a fail, and one more fail to try again after. */
    @GameTest(template = "floor8", batch = RESULTS_AFTERNOON, timeoutTicks = 100)
    public static void a_pet_that_missed_the_morning_hears_its_results_where_it_is(GameTestHelper helper) {
        placeDesk(helper);
        AbstractPet pet = worker(helper, new BlockPos(3, STAND, 5));
        pet.setPetDirective(PetDirective.STAY);
        pet.licences().set(QualificationData.WEEDING, new Licence(0, 0, 1, false,
            new ExamStage.Sat(at(helper, DESK), EXAM_DAY_NUMBER, false)));

        helper.succeedWhen(() -> {
            Licence licence = pet.licences().get(QualificationData.WEEDING);
            helper.assertTrue(licence.paper().isEmpty(), Component.literal("the pet has not heard its results"));
            helper.assertTrue(licence.held() == 0 && licence.fails() == 2, Component.literal("a fail was not counted"));
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
        helper.assertTrue(pet.licences().get(QualificationData.WEEDING).read(), Component.literal("the pet did not read the book"));
        helper.assertTrue(owner.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1, Component.literal("the book was not spent"));

        pet.mobInteract(owner, InteractionHand.MAIN_HAND);
        helper.assertTrue(owner.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1,
            Component.literal("a pet that had read the book already took another"));
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
        helper.assertTrue(plain >= 1 && plain <= 2, Component.literal("a pet with no licence was paid " + plain));
        helper.assertTrue(licensed >= 6 && licensed <= 7, Component.literal("a grade 1 pet was paid " + licensed));
        helper.succeed();
    }

    /** A desk at {@link #DESK} facing north, and its chair behind it, as placing one puts them. */
    private static ExamDeskBlockEntity placeDesk(GameTestHelper helper) {
        BlockState desk = InitBlocks.EXAM_DESK.get().defaultBlockState().setValue(ExamDeskBlock.FACING, Direction.NORTH);
        helper.setBlock(DESK, desk);
        helper.setBlock(ExamDeskBlock.chair(DESK, Direction.NORTH), desk.setValue(ExamDeskBlock.PART, DeskPart.CHAIR));
        return helper.getBlockEntity(DESK, ExamDeskBlockEntity.class);
    }

    /** A practised pet of a fresh owner's, signed up at the desk. */
    private static AbstractPet signedUp(GameTestHelper helper, ExamDeskBlockEntity desk, BlockPos rel) {
        ServerPlayer owner = player(helper);
        owner.getInventory().add(new ItemStack(Items.DIAMOND, 1));
        AbstractPet pet = ownersPet(helper, owner, rel, true);
        Optional<ExamEnrollment.Refusal> refusal = ExamEnrollment.signUp(owner, desk, QualificationData.WEEDING, pet.getUUID());
        helper.assertTrue(refusal.isEmpty(), Component.literal("the pet was not signed up: " + refusal));
        return pet;
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

    private static Optional<Ineligible> whyNot(ExamEnrollment.Offer offer, AbstractPet pet) {
        return offer.candidates().stream()
            .filter(candidate -> candidate.pet() == pet)
            .findFirst()
            .flatMap(ExamEnrollment.Candidate::whyNot);
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
}
