package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.MIDNIGHT;
import static com.dwinovo.chiikawa.gametest.GameTestKit.addSong;
import static com.dwinovo.chiikawa.gametest.GameTestKit.carries;
import static com.dwinovo.chiikawa.gametest.GameTestKit.ownedPet;
import static com.dwinovo.chiikawa.gametest.GameTestKit.player;
import static com.dwinovo.chiikawa.gametest.GameTestKit.readySong;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;

import com.dwinovo.chiikawa.block.DeskBooking;
import com.dwinovo.chiikawa.block.ExamDeskBlockEntity;
import com.dwinovo.chiikawa.block.LaborBoardBlockEntity;
import com.dwinovo.chiikawa.camera.PhotoResults;
import com.dwinovo.chiikawa.camera.PhotoScout;
import com.dwinovo.chiikawa.data.PetInteractionData;
import com.dwinovo.chiikawa.data.QualificationData;
import com.dwinovo.chiikawa.data.WorkData;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.interact.PetInteractHandler;
import com.dwinovo.chiikawa.entity.brain.PetMind;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.entity.brain.errand.run.Running;
import com.dwinovo.chiikawa.entity.brain.errand.step.Meeting;
import com.dwinovo.chiikawa.gifting.GiftOccasion;
import com.dwinovo.chiikawa.gifting.Presents;
import com.dwinovo.chiikawa.init.InitBlocks;
import com.dwinovo.chiikawa.init.InitDataComponents;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.music.MusicBoxSelection;
import com.dwinovo.chiikawa.music.MusicTrackView;
import com.dwinovo.chiikawa.music.PlaybackMode;
import com.dwinovo.chiikawa.music.ServerMusicLibrary;
import com.dwinovo.chiikawa.music.ServerMusicSystem;
import com.dwinovo.chiikawa.photo.PhotoSystem;
import com.dwinovo.chiikawa.qualification.ExamStage;
import com.dwinovo.chiikawa.qualification.Licence;
import com.dwinovo.chiikawa.shop.Wallet;
import com.dwinovo.chiikawa.social.InteractionPlan;
import com.dwinovo.chiikawa.social.PetInteraction;
import com.dwinovo.chiikawa.social.PetInteractions;
import com.dwinovo.chiikawa.task.BoardSlot;
import com.dwinovo.chiikawa.whistle.WhistleHeard;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

/**
 * For each errand, a setup in which it is the best thing its pet can do and a way to tell whether
 * it was carried out cleanly. The contract cases in {@link ErrandContractGameTests} are made from these, so
 * an errand that adds its setup here gets its three cases.
 *
 * <p>Each setup is a plain static method of a helper and returns a plain record, so a branch that
 * registers its cases another way rewrites that registration and leaves these as they are.
 */
final class ErrandScenarios {
    private static final int STAND = 2;
    /** How far off the owner is, east, along the row of yards. */
    private static final int OWNER_AWAY = 40;
    private static final int FILM = 3;
    /** The step of a present left by the owner's bed at which the pet waits for the owner to wake, and for how long before they do. */
    private static final int BED_WAIT_STEP = 1;
    private static final int BED_WAIT_TICKS = 100;
    /** When an owner who sleeps through the night wakes, as the level's clock reads it: a day's start. */
    private static final long DAWN = 24000L;
    private static final int COOKIES = 3;
    /** Enough for anything on the list, so what the pet does at the counter is about wanting rather than affording. */
    private static final int WAGES = 8;
    /** How far along the floor a pet is from the board it is to take a slip from: a walk of many ticks. */
    private static final int BOARD_AWAY = 7;
    /** The step of a photo at which the shutter goes: after the walk and the aim. */
    private static final int SHUTTER_STEP = 2;
    /**
     * Where a farmer's work is, across the yard from the farmer: a walk of a few seconds, and well inside what the farmer
     * sensor sees, which is five blocks each way from where the pet stands. A pet strolls about while it makes up its mind,
     * and work at the very edge of that is work out of sight as soon as it has strolled the wrong way.
     */
    private static final BlockPos WORK = new BlockPos(7, STAND, 7);
    private static final BlockPos FARMER = new BlockPos(4, STAND, 4);
    private static final int SEEDS = 8;
    private static final int CROP = 2;
    /** Where a fighter stands, and the foe east of it: a walk of many ticks for a sword, and within a bow's range. */
    private static final BlockPos FIGHTER = new BlockPos(4, STAND, 4);
    private static final BlockPos FOE = new BlockPos(12, STAND, 4);
    private static final BlockPos ARCHER = new BlockPos(3, STAND, 4);
    private static final BlockPos TARGET = new BlockPos(13, STAND, 4);
    private static final int ARROWS = 16;
    /** The pocket of a pet's backpack that a spare weapon is put in: the first that is a pocket for things. */
    private static final int SPARE_POCKET = 2;
    /** How long the game remembers a blow for, and a little over: after this long the pet is hurt again, for there to be something to run from. */
    private static final int FORGETTING_TICKS = 65;
    /** A song of silence, short enough to be played to its end in a case that waits for one. */
    private static final String SONG = "chiikawa_gametest_contract";
    private static final int SONG_SECONDS = 1;
    /** How long a whistle holds a pet's interest, longer than the cases take to cut the errand short at each of its steps. */
    private static final int WHISTLE_INTEREST_TICKS = 600;
    /** Long enough for a whistle the pet has just been told of to have reached its memory. */
    private static final int WHISTLE_ECHO_TICKS = 5;
    /** Ask, wait for the answer, walk over, hand over, play: and for the other, wait for the guest, play. */
    private static final int SOCIALIZE_STEPS = 5;
    private static final int COOPERATE_STEPS = 2;

    /**
     * A pet set up to be on one errand.
     *
     * @param pet the pet, which sets off on its own
     * @param steps how many steps the errand's routine has, which the cases cut it between
     * @param reported the report the errand's steps have made, by the name its settings give it, by the time the errand was
     *                 last seen at this step: a rest the errand begins when something happens in the middle of it, and
     *                 which however it ends it still keeps
     * @param untidy what the errand may not leave behind that is its own to the world and not to the pet, such as an item
     *               half taken: the problem, or nothing if all is as it should be; told the errand as it was last seen
     *               running, since what is left behind may be fair where the errand had got to
     * @param again what makes the errand startable again once it was cut short and checked, for an errand that rests for
     *              longer than the case lasts after every way it ends, such as one that two pets leave alone for a day, or
     *              that starts on something the cut took from the pet: forgets the rest, or gives the pet that back, which
     *              the case has seen is as the errand's data says
     */
    record Scenario(AbstractPet pet, int steps, Function<Running, Optional<String>> reported,
                    Function<Running, Optional<String>> untidy, Runnable again) {
        /** A scenario of an errand that is startable again as soon as its rest is over. */
        Scenario(AbstractPet pet, int steps, Function<Running, Optional<String>> reported,
                 Function<Running, Optional<String>> untidy) {
            this(pet, steps, reported, untidy, () -> {
            });
        }

        /** A scenario of an errand whose steps report nothing. */
        Scenario(AbstractPet pet, int steps, Function<Running, Optional<String>> untidy) {
            this(pet, steps, ran -> Optional.empty(), untidy);
        }
    }

    /**
     * A setup the contract cases are made from. An errand has one, and the ones that go about it in more than one way,
     * such as taking a present to an owner who is awake and to one who is asleep, have a setup for each.
     *
     * @param name what tells the setup from the errand's others, in the names of the cases
     * @param errand the errand the pet is set up for
     * @param instant whether the errand is done in the tick it begins, so that there is no step to cut it at and it is
     *                never seen running: its cases are the ones that ask what it offers and see it done, see
     *                {@link ErrandContractGameTests}
     * @param setUp the setup
     */
    record Case(String name, ResourceLocation errand, boolean instant, Function<GameTestHelper, Scenario> setUp) {
    }

    private static final Map<ResourceLocation, Function<GameTestHelper, Scenario>> BY_ERRAND = Map.ofEntries(
        Map.entry(ErrandIds.PICK_UP_ITEM, ErrandScenarios::pickUpItem),
        Map.entry(ErrandIds.TAKE_PHOTO, ErrandScenarios::takePhoto),
        Map.entry(ErrandIds.OFFER_GIFT, ErrandScenarios::offerGift),
        Map.entry(ErrandIds.GIFT_OWNER, ErrandScenarios::giftOwner),
        Map.entry(ErrandIds.SHOP, ErrandScenarios::shop),
        Map.entry(ErrandIds.TAKE_TASK, ErrandScenarios::takeTask),
        Map.entry(ErrandIds.TAKE_EXAM, ErrandScenarios::takeExam),
        Map.entry(ErrandIds.CHECK_RESULTS, ErrandScenarios::checkResults),
        Map.entry(WorkData.HARVEST, ErrandScenarios::harvest),
        Map.entry(WorkData.PLANT, ErrandScenarios::plant),
        Map.entry(WorkData.DELIVER, ErrandScenarios::deliver),
        Map.entry(WorkData.WEED, ErrandScenarios::weed),
        Map.entry(WorkData.PICK_MUSHROOM, ErrandScenarios::pickMushroom),
        Map.entry(WorkData.MELEE, ErrandScenarios::melee),
        Map.entry(WorkData.RANGED, ErrandScenarios::ranged),
        Map.entry(WorkData.HUNT_MELEE, ErrandScenarios::huntMelee),
        Map.entry(WorkData.HUNT_RANGED, ErrandScenarios::huntRanged),
        Map.entry(ErrandIds.DEFEND_SELF, ErrandScenarios::defendSelf),
        Map.entry(ErrandIds.FLEE, ErrandScenarios::flee),
        Map.entry(WorkData.PLAY_MUSIC, ErrandScenarios::playMusic),
        Map.entry(ErrandIds.FOLLOW_OWNER, ErrandScenarios::followOwner),
        Map.entry(ErrandIds.WANDER, ErrandScenarios::wander),
        Map.entry(ErrandIds.STAY, ErrandScenarios::stay),
        Map.entry(ErrandIds.SOCIALIZE, ErrandScenarios::socialize),
        Map.entry(ErrandIds.COOPERATE, ErrandScenarios::cooperate),
        Map.entry(ErrandIds.ANSWER_WHISTLE, ErrandScenarios::answerWhistle));

    /** The errands that are done in the tick they begin. */
    private static final Set<ResourceLocation> INSTANT = Set.of(ErrandIds.OFFER_GIFT);

    /** The setups beside an errand's own, for the other way it goes about it. */
    private static final List<Case> ALSO = List.of(
        new Case("gift_owner_bedside", ErrandIds.GIFT_OWNER, false, ErrandScenarios::giftOwnerBedside));

    private ErrandScenarios() {
    }

    /** @return the errands that have a scenario, which are all of them */
    static Set<ResourceLocation> ids() {
        return BY_ERRAND.keySet();
    }

    /** @return every setup the contract cases are made from: each errand's own, then those for its other ways */
    static List<Case> cases() {
        List<Case> cases = new ArrayList<>();
        BY_ERRAND.forEach((errand, setUp) -> cases.add(new Case(errand.getPath(), errand, INSTANT.contains(errand), setUp)));
        cases.addAll(ALSO);
        return cases;
    }

    /** @return the pet and what the errand touches, set up in the case's level */
    static Scenario setUp(ResourceLocation errand, GameTestHelper helper) {
        Function<GameTestHelper, Scenario> setUp = BY_ERRAND.get(errand);
        if (setUp == null) {
            throw new IllegalArgumentException(errand + " has no scenario");
        }
        return setUp.apply(helper);
    }

    /**
     * A pet at heel and its owner across the yard, just short of the reach of the area it acts in and so a walk of a few seconds.
     * Nothing of the errand is left in the world.
     */
    private static Scenario followOwner(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        BlockPos across = helper.absolutePos(new BlockPos(14, STAND, 8));
        owner.teleportTo(across.getX() + 0.5, across.getY(), across.getZ() + 0.5);
        AbstractPet pet = GameTestKit.wild(helper, InitEntity.USAGI_PET.get(), new BlockPos(3, STAND, 8));
        pet.tame(owner);
        pet.setPetDirective(PetDirective.FOLLOW);
        return new Scenario(pet, 1, ran -> Optional.empty());
    }

    /**
     * A wild pet in an empty yard and a whistle blown across it, which the pet is told of as the blow would tell it, and
     * again whenever its curiosity has run out, so that the errand is under way for as long as the case cuts it short. It
     * walks over and looks at the spot. Whatever step it is cut short at, the walk and the gaze are gone with it.
     */
    private static Scenario answerWhistle(GameTestHelper helper) {
        AbstractPet pet = GameTestKit.wildPet(helper, new BlockPos(4, STAND, 8));
        GlobalPos blown = GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(new BlockPos(11, STAND, 8)));
        long[] told = {-WHISTLE_ECHO_TICKS};
        helper.onEachTick(() -> {
            long now = helper.getLevel().getGameTime();
            if (!pet.getBrain().hasMemoryValue(WhistleHeard.type()) && now - told[0] > WHISTLE_ECHO_TICKS) {
                told[0] = now;
                PetMind.post(pet, new Message.WhistleHeard(blown, now, now + WHISTLE_INTEREST_TICKS));
            }
        });
        return new Scenario(pet, 2, ran -> Optional.empty());
    }

    /**
     * A pet left to itself in an empty yard, which potters about: every habit is one step. Whatever habit it is cut short in,
     * the walk and the gaze it set are gone with it and the pet has not been told anything it does not hold to.
     */
    private static Scenario wander(GameTestHelper helper) {
        AbstractPet pet = GameTestKit.owned(helper, InitEntity.USAGI_PET.get(), new BlockPos(8, STAND, 8));
        pet.setPetDirective(PetDirective.FREE);
        return new Scenario(pet, 1, ran -> Optional.empty());
    }

    /**
     * A pet told to sit, which looks around where it sits. Whatever habit it is cut short in, it has not moved from its seat
     * and is still sitting: the sitting is the directive's, and an errand ending is not a reason to stand up.
     */
    private static Scenario stay(GameTestHelper helper) {
        AbstractPet pet = GameTestKit.owned(helper, InitEntity.USAGI_PET.get(), new BlockPos(8, STAND, 8));
        pet.setPetDirective(PetDirective.STAY);
        Vec3 seat = pet.position();
        return new Scenario(pet, 1, ran -> {
            if (pet.getPetDirective() != PetDirective.STAY) {
                return Optional.of("the pet is no longer told to sit");
            }
            return pet.position().distanceToSqr(seat) > 1.0E-6 ? Optional.of("the pet moved from its seat") : Optional.empty();
        });
    }

    /**
     * Wheat on the floor some blocks off, to a pet with nothing else to do: far enough to be a walk of many
     * ticks, near enough for the pet to notice it. The wheat is whole and where it was until the pet has it.
     */
    private static Scenario pickUpItem(GameTestHelper helper) {
        AbstractPet pet = ownedPet(helper, new BlockPos(4, STAND, 8));
        ItemEntity wheat = helper.spawnItem(Items.WHEAT, new BlockPos(10, STAND, 8));
        int count = wheat.getItem().getCount();
        return new Scenario(pet, 2, ran -> {
            if (carries(pet, Items.WHEAT)) {
                return Optional.of("the pet has the wheat, though the errand was not done");
            }
            if (!wheat.isAlive() || wheat.getItem().getCount() != count) {
                return Optional.of("the wheat on the floor was touched, though the pet does not have it");
            }
            return Optional.empty();
        });
    }

    /**
     * A pet with three cookies in its bag, a present in mind and its owner across the yard. The errand takes a cookie out of
     * the bag and is done in the tick it begins, so there is no step to cut it at: the pet is then carrying the present,
     * and the errand that takes it to the owner is the next. The case that asks what the errand offers keeps the pet's mind
     * off, or the pet would have taken the present before it was asked.
     */
    private static Scenario offerGift(GameTestHelper helper) {
        ServerPlayer owner = ownerAcross(helper);
        AbstractPet pet = GameTestKit.wild(helper, InitEntity.USAGI_PET.get(), new BlockPos(3, STAND, 3));
        pet.tame(owner);
        pet.setPetDirective(PetDirective.FREE);
        pet.getBackpack().setItem(AbstractPet.BAG_SLOT + 1, new ItemStack(Items.COOKIE, COOKIES));
        PetMind.post(pet, new Message.Occasion(GiftOccasion.FILM_FULL, Optional.empty()));
        return new Scenario(pet, 1, ran -> Optional.empty());
    }

    /**
     * A pet carrying a cookie to its owner across the yard, a walk of many ticks. Whatever is cut short, the cookie is back in
     * the bag and the owner has none, unless the present was handed over; and what makes the errand startable again is a
     * cookie in the paws once more, since the pet has no other thought of a present.
     */
    private static Scenario giftOwner(GameTestHelper helper) {
        ServerPlayer owner = ownerAcross(helper);
        AbstractPet pet = GameTestKit.wild(helper, InitEntity.USAGI_PET.get(), new BlockPos(3, STAND, 3));
        pet.tame(owner);
        pet.setPetDirective(PetDirective.FREE);
        pet.setPendingGift(new ItemStack(Items.COOKIE));
        return new Scenario(pet, 2,
            ran -> owner.getInventory().countItem(Items.COOKIE) > 0 ? Optional.of(Presents.GIVEN) : Optional.empty(),
            ran -> {
                if (GameTestKit.count(pet, Items.COOKIE) != 1) {
                    return Optional.of("the bag has " + GameTestKit.count(pet, Items.COOKIE) + " cookies, not the one it was carrying");
                }
                if (owner.getInventory().countItem(Items.COOKIE) > 0) {
                    return Optional.of("the owner was given the cookie, though the present was not handed over");
                }
                return Optional.empty();
            }, () -> carryACookie(pet));
    }

    /**
     * The same pet and present for an owner asleep in a bed some blocks off, in the night, which is when an owner sleeps: the
     * pet walks to the bed, lies down by it and waits. It is the owner waking that lets it go on, as it would in the morning,
     * and the owner wakes when the pet has waited a while, so that the case can cut it short at the wait and then at the last
     * step. Whatever is cut short, the cookie is back in the bag and not on the floor by the bed.
     */
    private static Scenario giftOwnerBedside(GameTestHelper helper) {
        settleWorld(helper.getLevel(), Difficulty.NORMAL, MIDNIGHT);
        BlockPos head = new BlockPos(10, STAND, 10);
        BlockState bed = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.EAST);
        helper.setBlock(head.west(), bed.setValue(BedBlock.PART, BedPart.FOOT));
        helper.setBlock(head, bed.setValue(BedBlock.PART, BedPart.HEAD));
        ServerPlayer owner = player(helper);
        BlockPos in = helper.absolutePos(head);
        owner.teleportTo(in.getX() + 0.5, in.getY() + 0.6, in.getZ() + 0.5);
        owner.startSleeping(in);
        AbstractPet pet = GameTestKit.wild(helper, InitEntity.USAGI_PET.get(), new BlockPos(3, STAND, 3));
        pet.tame(owner);
        pet.setPetDirective(PetDirective.FREE);
        pet.setPendingGift(new ItemStack(Items.COOKIE));
        long[] lyingSince = {-1};
        helper.onEachTick(() -> {
            long now = helper.getLevel().getGameTime();
            boolean waiting = pet.getMind().errands().running().filter(running -> running.errand().equals(ErrandIds.GIFT_OWNER)
                && running.stepIndex() == BED_WAIT_STEP).isPresent();
            lyingSince[0] = waiting ? (lyingSince[0] < 0 ? now : lyingSince[0]) : -1;
            if (waiting && owner.isSleeping() && now - lyingSince[0] >= BED_WAIT_TICKS) {
                helper.getLevel().setDayTime(DAWN);
                owner.stopSleepInBed(false, true);
            }
        });
        return new Scenario(pet, 3,
            ran -> pet.getPendingGift().isEmpty() && GameTestKit.count(pet, Items.COOKIE) == 0
                ? Optional.of(Presents.GIVEN) : Optional.empty(),
            ran -> {
                if (GameTestKit.count(pet, Items.COOKIE) != 1) {
                    return Optional.of("the bag has " + GameTestKit.count(pet, Items.COOKIE) + " cookies, not the one it was carrying");
                }
                if (!helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds(), item -> item.getItem().is(Items.COOKIE)).isEmpty()) {
                    return Optional.of("a cookie was left on the floor, though the owner never woke to it");
                }
                return Optional.empty();
            }, () -> {
                settleWorld(helper.getLevel(), Difficulty.NORMAL, MIDNIGHT);
                if (!owner.isSleeping()) {
                    owner.startSleeping(in);
                }
                lyingSince[0] = -1;
                carryACookie(pet);
            });
    }

    /** The pet has a cookie in its paws and nothing else, as it does when it has set off with one. */
    private static void carryACookie(AbstractPet pet) {
        pet.getBackpack().clearContent();
        pet.setPendingGift(new ItemStack(Items.COOKIE));
    }

    /** The owner, across the yard from where the pet stands, a walk of many ticks and within the area a free pet acts in. */
    private static ServerPlayer ownerAcross(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        BlockPos across = helper.absolutePos(new BlockPos(10, STAND, 10));
        owner.teleportTo(across.getX() + 0.5, across.getY(), across.getZ() + 0.5);
        return owner;
    }

    /**
     * A pet with a purse of emeralds and nothing to do but spend them, and a shop across the yard: a walk of many ticks to
     * the counter. A purchase is money paid for goods in the bag, both or neither, so what is left is never one without the
     * other.
     */
    private static Scenario shop(GameTestHelper helper) {
        helper.setBlock(new BlockPos(13, STAND, 4), InitBlocks.SHOP.get());
        AbstractPet pet = GameTestKit.worker(helper, new BlockPos(3, STAND, 4));
        pet.getBackpack().addItem(new ItemStack(Items.EMERALD, WAGES));
        return new Scenario(pet, 2, ran -> {
            boolean paid = Wallet.count(pet.getBackpack()) < WAGES;
            boolean goods = false;
            for (int slot = 0; slot < pet.getBackpack().getContainerSize(); slot++) {
                ItemStack stack = pet.getBackpack().getItem(slot);
                goods |= !stack.isEmpty() && !Wallet.isMoney(stack);
            }
            if (paid && !goods) {
                return Optional.of("money was paid, though the pet came away with nothing for it");
            }
            if (goods && !paid) {
                return Optional.of("the pet has goods it did not pay for");
            }
            return Optional.empty();
        });
    }

    /**
     * A farmer with a hoe and a labor board some blocks off that has a weeding slip first: a walk of many ticks to the board.
     * The pet holds the slip on the board or has taken it, and the board and the pet agree about it: a slip is up on the
     * board or on the pet, never both and never neither.
     */
    private static Scenario takeTask(GameTestHelper helper) {
        BlockPos at = BoardGameTests.weedingBoard(helper);
        helper.setBlock(at, InitBlocks.LABOR_BOARD.get());
        LaborBoardBlockEntity board = (LaborBoardBlockEntity) helper.getBlockEntity(at);
        BlockPos from = new BlockPos(at.getX() >= BOARD_AWAY + 1 ? at.getX() - BOARD_AWAY : at.getX() + BOARD_AWAY, STAND, at.getZ());
        AbstractPet pet = GameTestKit.holding(GameTestKit.worker(helper, from), Items.WOODEN_HOE);
        return new Scenario(pet, 3, ran -> {
            if (board.isReservedBy(pet)) {
                return Optional.of("the pet still holds a slip on the board");
            }
            long taken = board.today().stream().filter(BoardSlot::claimed).count();
            long carried = pet.getTask().isPresent() ? 1 : 0;
            if (taken != carried) {
                return Optional.of(taken > carried ? "a slip is marked taken on the board, though the pet carries none"
                    : "the pet carries a slip the board does not have marked taken");
            }
            return Optional.empty();
        });
    }

    /**
     * A pet signed up for an exam at a desk across the yard, on the day it is held. The paper is handed in whole or not
     * at all, the licence and the desk together, and the desk stays booked for the pet whatever becomes of its errand:
     * a pet called away before it hands in can come back to it while the exam is on.
     */
    private static Scenario takeExam(GameTestHelper helper) {
        ExamDeskBlockEntity desk = ExamGameTests.placeDesk(helper);
        AbstractPet pet = ExamGameTests.signedUp(helper, desk, new BlockPos(4, STAND, 8));
        return new Scenario(pet, 5, ran -> {
            boolean paperIn = pet.licences().get(QualificationData.WEEDING).paper().isPresent();
            boolean resultOnDesk = desk.booking().flatMap(DeskBooking::passed).isPresent();
            if (paperIn != resultOnDesk) {
                return Optional.of(paperIn ? "the pet's licence has a paper in, though the desk has none of it"
                    : "the desk has the paper, though the pet's licence has not handed it in");
            }
            if (desk.booking().filter(booking -> booking.pet().equals(pet.getUUID())).isEmpty()) {
                return Optional.of("the desk is no longer booked for the pet that was sitting at it");
            }
            return Optional.empty();
        });
    }

    /**
     * A pet that sat an exam yesterday and passed, on the morning the results are up, a few blocks from the desk. The results
     * are heard whole or not at all: the licence has gone up a grade and the paper is gone, or neither has happened.
     */
    private static Scenario checkResults(GameTestHelper helper) {
        settleWorld(helper.getLevel(), Difficulty.NORMAL, (ExamGameTests.EXAM_DAY_NUMBER + 1) * Level.TICKS_PER_DAY + ExamGameTests.MORNING);
        ExamDeskBlockEntity desk = ExamGameTests.placeDesk(helper);
        AbstractPet pet = GameTestKit.worker(helper, new BlockPos(4, STAND, 8));
        pet.licences().set(QualificationData.WEEDING, new Licence(0, 0, 0, false,
            new ExamStage.Sat(ExamGameTests.at(helper, ExamGameTests.DESK), ExamGameTests.EXAM_DAY_NUMBER, true)));
        desk.book(new DeskBooking(pet.getUUID(), "Shisa", QualificationData.WEEDING, 5, ExamGameTests.EXAM_DAY_NUMBER,
            Optional.of(true)));
        return new Scenario(pet, 3, ran -> {
            Licence licence = pet.licences().get(QualificationData.WEEDING);
            if (licence.paper().isPresent() == (licence.held() == 1)) {
                return Optional.of(licence.held() == 1 ? "the pet has the grade, and the paper it was granted for"
                    : "the pet has no grade, and the paper it should have been granted for is gone");
            }
            return Optional.empty();
        });
    }

    /**
     * Hachiware with a camera and film, its owner out of sight but near enough for their game to draw the picture, and
     * a friend to photograph who has nothing to do. The view is left out by having been to every biome there is, so the
     * friend is the one moment, and the errand has all its four steps: the walk, the aim, the shutter and the hold.
     * Nothing is asked for and nobody turns to the camera until the shutter has gone.
     */
    private static Scenario takePhoto(GameTestHelper helper) {
        ServerPlayer owner = player(helper);
        BlockPos far = helper.absolutePos(new BlockPos(8, STAND, 8)).offset(OWNER_AWAY, 0, 0);
        owner.teleportTo(far.getX() + 0.5, far.getY(), far.getZ() + 0.5);
        AbstractPet pet = GameTestKit.pet(helper, InitEntity.HACHIWARE_PET.get(), new BlockPos(4, STAND, 4), false);
        pet.tame(owner);
        pet.setPetDirective(PetDirective.FREE);
        pet.getBackpack().addItem(new ItemStack(InitItems.CAMERA.get()));
        pet.getBackpack().addItem(new ItemStack(InitItems.FILM.get(), FILM));
        pet.getBrain().setMemory(InitMemory.PHOTOGRAPHED_BIOMES.get(), PhotoScout.biomeOf(pet).map(List::of).orElseThrow());
        AbstractPet friend = GameTestKit.pet(helper, InitEntity.CHIIKAWA_PET.get(), new BlockPos(11, STAND, 4), false);
        friend.setNoAi(true);
        PhotoSystem photos = PhotoSystem.of(helper.getLevel().getServer());
        Set<UUID> asked = photos.waiting();
        return new Scenario(pet, 4, ran -> ran.stepIndex() > SHUTTER_STEP ? Optional.of(PhotoResults.TAKEN) : Optional.empty(), ran -> {
            boolean shutterPressed = ran.stepIndex() >= SHUTTER_STEP;
            if (!shutterPressed && !asked.containsAll(photos.waiting())) {
                return Optional.of("a photo was asked for before the shutter");
            }
            if (!shutterPressed && friend.getBrain().hasMemoryValue(MemoryModuleType.LOOK_TARGET)) {
                return Optional.of("the friend was turned to the camera before the shutter");
            }
            return Optional.empty();
        });
    }

    /**
     * A ripe wheat crop across the yard and a farmer with a hoe. A crop is reaped whole or not at all: it stands ripe and the
     * pet has no wheat, or it has been replanted young and the pet has the wheat.
     */
    private static Scenario harvest(GameTestHelper helper) {
        helper.setBlock(WORK.below(), moistFarmland());
        helper.setBlock(WORK, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE));
        AbstractPet pet = GameTestKit.holding(GameTestKit.worker(helper, FARMER), Items.WOODEN_HOE);
        return new Scenario(pet, 2, ran -> {
            BlockState crop = helper.getBlockState(WORK);
            boolean ripe = crop.is(Blocks.WHEAT) && crop.getValue(CropBlock.AGE) == CropBlock.MAX_AGE;
            boolean reaped = carries(pet, Items.WHEAT);
            if (ripe && reaped) {
                return Optional.of("the pet has the wheat, though the crop is still standing ripe");
            }
            if (!ripe && !reaped) {
                return Optional.of("the crop was reaped, though the pet has no wheat");
            }
            return Optional.empty();
        });
    }

    /**
     * Bare watered farmland across the yard and a farmer with seeds. A seed is planted or it is not: the crop stands and
     * one seed is gone from the bag, or neither.
     */
    private static Scenario plant(GameTestHelper helper) {
        helper.setBlock(WORK.below(), moistFarmland());
        AbstractPet pet = GameTestKit.holding(GameTestKit.worker(helper, FARMER), Items.WOODEN_HOE);
        pet.getBackpack().addItem(new ItemStack(Items.WHEAT_SEEDS, SEEDS));
        return new Scenario(pet, 2, ran -> {
            boolean planted = helper.getBlockState(WORK).is(Blocks.WHEAT);
            int used = SEEDS - GameTestKit.count(pet, Items.WHEAT_SEEDS);
            if (planted && used != 1) {
                return Optional.of("a crop is planted, though " + used + " seeds are gone from the bag");
            }
            if (!planted && used != 0) {
                return Optional.of(used + " seeds are gone from the bag, though no crop is planted");
            }
            return Optional.empty();
        });
    }

    /**
     * A farmer with some wheat to put away and a chest across the yard. Whatever is cut short, no wheat is lost or made:
     * what is not in the chest is in the bag, and the chest's lid is down.
     */
    private static Scenario deliver(GameTestHelper helper) {
        helper.setBlock(WORK, Blocks.CHEST);
        AbstractPet pet = GameTestKit.holding(GameTestKit.worker(helper, FARMER), Items.WOODEN_HOE);
        pet.getBackpack().addItem(new ItemStack(Items.WHEAT, CROP));
        return new Scenario(pet, 3 + CROP, ran -> {
            ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(WORK);
            int stored = 0;
            for (int slot = 0; slot < ((Container) chest).getContainerSize(); slot++) {
                ItemStack there = ((Container) chest).getItem(slot);
                stored += there.is(Items.WHEAT) ? there.getCount() : 0;
            }
            int total = stored + GameTestKit.count(pet, Items.WHEAT);
            if (total != CROP) {
                return Optional.of("the wheat in the chest and in the bag is " + total + ", not the " + CROP + " the pet set off with");
            }
            if (lidIsUp(helper.getLevel(), chest)) {
                return Optional.of("the chest was left open");
            }
            return Optional.empty();
        });
    }

    /** Grass across the yard, and a farmer with a hoe and no owner to pay it, which is who pulls weeds unasked. */
    private static Scenario weed(GameTestHelper helper) {
        helper.setBlock(WORK.below(), Blocks.GRASS_BLOCK);
        helper.setBlock(WORK, Blocks.SHORT_GRASS);
        AbstractPet pet = GameTestKit.holding(GameTestKit.wildWorker(helper, FARMER), Items.WOODEN_HOE);
        return new Scenario(pet, 2, ran -> {
            BlockState there = helper.getBlockState(WORK);
            return there.is(Blocks.SHORT_GRASS) || there.isAir() ? Optional.empty()
                : Optional.of("the weed was left as " + there.getBlock() + ", neither standing nor pulled");
        });
    }

    /**
     * A mushroom across the yard on a night, and a farmer with a hoe and no owner to pay it. A mushroom is picked whole
     * or not at all: it stands and the pet has none, or it is gone and the pet has it.
     */
    private static Scenario pickMushroom(GameTestHelper helper) {
        settleWorld(helper.getLevel(), Difficulty.NORMAL, GameTestKit.MIDNIGHT);
        helper.setBlock(WORK, Blocks.RED_MUSHROOM);
        AbstractPet pet = GameTestKit.holding(GameTestKit.wildWorker(helper, FARMER), Items.WOODEN_HOE);
        return new Scenario(pet, 2, ran -> {
            boolean standing = helper.getBlockState(WORK).is(Blocks.RED_MUSHROOM);
            boolean picked = carries(pet, Items.RED_MUSHROOM);
            if (standing && picked) {
                return Optional.of("the pet has the mushroom, though it is still standing");
            }
            if (!standing && !picked) {
                return Optional.of("the mushroom is gone, though the pet does not have it");
            }
            return Optional.empty();
        });
    }

    /**
     * A pet with a sword and a zombie across the yard, standing where it is and with one hit left in it, that has hurt the
     * pet: a pet let loose fights what hurt it as a guard, not as a hunt, and the zombie is its to answer as long as the pet
     * remembers it. It is night, since a zombie that burns in the sun is put down by the sun and not by the pet. A fight
     * leaves nothing in the world to put right but what any errand does: the pet is not using anything, and has nowhere
     * left to walk to.
     */
    private static Scenario melee(GameTestHelper helper) {
        AbstractPet pet = fencer(helper);
        hurtBy(pet, foe(helper, FOE));
        return new Scenario(pet, 2, ran -> Optional.empty());
    }

    /** The same pet and the same zombie, which has not hurt the pet: it is a pet let loose clearing what comes by, a hunt. */
    private static Scenario huntMelee(GameTestHelper helper) {
        AbstractPet pet = fencer(helper);
        foe(helper, FOE);
        return new Scenario(pet, 2, ran -> Optional.empty());
    }

    private static AbstractPet fencer(GameTestHelper helper) {
        settleWorld(helper.getLevel(), Difficulty.NORMAL, GameTestKit.MIDNIGHT);
        return GameTestKit.holding(ownedPet(helper, FIGHTER), Items.IRON_SWORD);
    }

    /**
     * A pet with a bow and a quiver and a zombie across the yard, in range and standing where it is, that has hurt the pet.
     * However it is cut short, even with the bow at its full draw, no arrow flies: the quiver is as it was and the bow is
     * down.
     */
    private static Scenario ranged(GameTestHelper helper) {
        AbstractPet pet = archer(helper);
        hurtBy(pet, foe(helper, TARGET));
        return new Scenario(pet, 3, ran -> untouchedQuiver(pet));
    }

    /** The same pet and the same zombie, which has not hurt the pet: a hunt. */
    private static Scenario huntRanged(GameTestHelper helper) {
        AbstractPet pet = archer(helper);
        foe(helper, TARGET);
        return new Scenario(pet, 3, ran -> untouchedQuiver(pet));
    }

    private static AbstractPet archer(GameTestHelper helper) {
        settleWorld(helper.getLevel(), Difficulty.NORMAL, GameTestKit.MIDNIGHT);
        AbstractPet pet = GameTestKit.holding(ownedPet(helper, ARCHER), Items.BOW);
        pet.getBackpack().addItem(new ItemStack(Items.ARROW, ARROWS));
        return pet;
    }

    private static Optional<String> untouchedQuiver(AbstractPet pet) {
        int left = GameTestKit.count(pet, Items.ARROW);
        return left == ARROWS ? Optional.empty()
            : Optional.of((ARROWS - left) + " arrows are gone from the quiver, though the pet was cut short before it loosed one");
    }

    /**
     * A farmer with a sword in its bag and a zombie across the yard that has hurt it, standing where it is and with one hit
     * left in it: it takes the sword up and fights, and is cut short in the swap, in the footwork and in the swing. Wherever
     * it is, it has its hoe back in its hand and the sword in its bag, as it was.
     */
    private static Scenario defendSelf(GameTestHelper helper) {
        settleWorld(helper.getLevel(), Difficulty.NORMAL, GameTestKit.MIDNIGHT);
        AbstractPet pet = GameTestKit.holding(ownedPet(helper, FIGHTER), Items.WOODEN_HOE);
        pet.getBackpack().setItem(SPARE_POCKET, new ItemStack(Items.IRON_SWORD));
        hurtBy(pet, foe(helper, FOE));
        return new Scenario(pet, 3, ran -> {
            if (!pet.getMainHandItem().is(Items.WOODEN_HOE)) {
                return Optional.of("the pet is holding " + pet.getMainHandItem() + ", its hoe is in the bag");
            }
            return pet.getBackpack().getItem(SPARE_POCKET).is(Items.IRON_SWORD) ? Optional.empty()
                : Optional.of("the sword is not back in the bag");
        });
    }

    /**
     * A pet with nothing to fight with at its owner's heel and a zombie that keeps hurting it from across the yard, as it
     * is hurt again whenever the game has forgotten the last time: it runs to its owner and hides behind them, and
     * cowers, and whatever step it is cut short at, it is not left running, cowering or looking away.
     */
    private static Scenario flee(GameTestHelper helper) {
        settleWorld(helper.getLevel(), Difficulty.NORMAL, GameTestKit.MIDNIGHT);
        ServerPlayer owner = player(helper);
        BlockPos across = helper.absolutePos(new BlockPos(14, STAND, 8));
        owner.teleportTo(across.getX() + 0.5, across.getY(), across.getZ() + 0.5);
        AbstractPet pet = GameTestKit.wild(helper, InitEntity.USAGI_PET.get(), new BlockPos(5, STAND, 8));
        pet.tame(owner);
        pet.setPetDirective(PetDirective.FOLLOW);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, STAND, 8));
        zombie.setNoAi(true);
        long[] hitAt = {-FORGETTING_TICKS};
        helper.onEachTick(() -> {
            long now = helper.getLevel().getGameTime();
            if (now - hitAt[0] >= FORGETTING_TICKS && !pet.getBrain().hasMemoryValue(MemoryModuleType.HURT_BY)) {
                hitAt[0] = now;
                pet.setHealth(pet.getMaxHealth());
                pet.hurt(zombie.damageSources().mobAttack(zombie), 1.0F);
            }
        });
        return new Scenario(pet, 2, ran -> Optional.empty());
    }

    /**
     * Hachiware with a music box set to play the one song over and over, the song written to the server's music folder the
     * way a player adds one and imported in its own time: the box is put in the pet's hand once the song is ready. However
     * it is cut short, no stream of the song is left going for the pet.
     */
    private static Scenario playMusic(GameTestHelper helper) {
        ServerMusicLibrary library = ServerMusicSystem.library(helper.getLevel().getServer());
        addSong(library, SONG, SONG_SECONDS);
        AbstractPet pet = GameTestKit.owned(helper, InitEntity.HACHIWARE_PET.get(), new BlockPos(8, STAND, 8));
        boolean[] given = {false};
        helper.onEachTick(() -> readySong(library, SONG).filter(track -> !given[0]).ifPresent(track -> {
            given[0] = true;
            ItemStack box = new ItemStack(InitItems.MUSIC_BOX.get());
            box.set(InitDataComponents.MUSIC_BOX_SELECTION.get(), new MusicBoxSelection(track.trackId(), SONG, 1));
            box.set(InitDataComponents.MUSIC_BOX_MODE.get(), PlaybackMode.REPEAT_ONE);
            pet.setItemSlot(EquipmentSlot.MAINHAND, box);
        }));
        return new Scenario(pet, 1, ran -> readySong(library, SONG).map(MusicTrackView::trackId)
            .filter(track -> ServerMusicSystem.streams(helper.getLevel().getServer()).isPlaying(pet, track))
            .map(track -> "the song is still being streamed for the pet"));
    }

    /**
     * Rakko, who goes over to Chiikawa with a snack from its own bag, and Chiikawa, who is asked and plays along: the pair
     * of the treat scene, both wild and pottering about, Rakko with the idea of going over to Chiikawa for as long as it is
     * not on the way. Chiikawa's health goes up by one snack's worth for every cookie it eats, so the cookies can be told:
     * each is in Rakko's bag or eaten, never lost and never both. Whichever of the two is cut short, neither is left
     * engaged, posed or waiting for the other.
     *
     * <p>The pet the scenario is for is made first, so that it ticks first and its partner hears in the same tick that it
     * is not coming after all.
     */
    private static final class Treat {
        private static final BlockPos RAKKO = new BlockPos(4, STAND, 8);
        private static final BlockPos CHIIKAWA = new BlockPos(8, STAND, 8);
        private static final float HEALTHY = 20.0F;
        private static final double HEARTY = 100.0;

        private final AbstractPet rakko;
        private final AbstractPet chiikawa;

        private Treat(GameTestHelper helper, boolean rakkoFirst) {
            AbstractPet first = rakkoFirst ? makeRakko(helper) : makeChiikawa(helper);
            AbstractPet second = rakkoFirst ? makeChiikawa(helper) : makeRakko(helper);
            this.rakko = rakkoFirst ? first : second;
            this.chiikawa = rakkoFirst ? second : first;
            helper.onEachTick(this::thinkOfGoingOver);
        }

        private static AbstractPet makeRakko(GameTestHelper helper) {
            AbstractPet rakko = GameTestKit.wild(helper, InitEntity.RAKKO_PET.get(), RAKKO);
            rakko.getBackpack().setItem(AbstractPet.BAG_SLOT + 1, new ItemStack(Items.COOKIE, COOKIES));
            return rakko;
        }

        private static AbstractPet makeChiikawa(GameTestHelper helper) {
            AbstractPet chiikawa = GameTestKit.wild(helper, InitEntity.CHIIKAWA_PET.get(), CHIIKAWA);
            chiikawa.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEARTY);
            chiikawa.setHealth(HEALTHY);
            return chiikawa;
        }

        /** Gives Rakko the idea its sensor would have had, whenever it has none and is not acting on one. */
        private void thinkOfGoingOver() {
            if (rakko.getMind().errands().isOn(ErrandIds.SOCIALIZE) || rakko.getBrain().hasMemoryValue(InitMemory.INTERACTION_PLAN.get())) {
                return;
            }
            ResourceLocation id = PetInteractionData.TREAT;
            PetInteraction scene = PetInteractions.all().get(id);
            rakko.getBrain().setMemory(InitMemory.INTERACTION_PLAN.get(), new InteractionPlan(id, scene,
                scene.initiatorSide(rakko.getType().builtInRegistryHolder()).orElseThrow(), chiikawa,
                scene.partners().get(scene.partnerRank(chiikawa.getType().builtInRegistryHolder()).getAsInt())));
        }

        private Optional<String> untidy() {
            long now = rakko.level().getGameTime();
            if (chiikawa.getMind().errands().engagedWith(now).isPresent()) {
                return Optional.of("Chiikawa is still said to be waiting for a visitor");
            }
            if (!rakko.getPerformance().isEmpty() || !chiikawa.getPerformance().isEmpty()) {
                return Optional.of("a scene is still being played: " + rakko.getPerformance() + " and " + chiikawa.getPerformance());
            }
            long eaten = Math.round((chiikawa.getHealth() - HEALTHY) / PetInteractHandler.FEED_HEAL);
            int kept = GameTestKit.count(rakko, Items.COOKIE);
            return kept + eaten == COOKIES ? Optional.empty()
                : Optional.of(kept + " cookies are in Rakko's bag and " + eaten + " were eaten, of the " + COOKIES + " it had");
        }

        /** The pair leave the scene alone for a day once it has begun, whichever way it ended. */
        private void forgetTheRest() {
            rakko.getBrain().eraseMemory(InitMemory.SOCIAL_COOLDOWNS.get());
            chiikawa.getBrain().eraseMemory(InitMemory.SOCIAL_COOLDOWNS.get());
        }
    }

    /** The pet that goes over, the first step being to ask the other to play along and the last to play its part. */
    private static Scenario socialize(GameTestHelper helper) {
        Treat treat = new Treat(helper, true);
        return new Scenario(treat.rakko, SOCIALIZE_STEPS, ran -> played(ran, SOCIALIZE_STEPS - 1), ran -> treat.untidy(),
            treat::forgetTheRest);
    }

    /** The pet that is asked, the first step being to wait for the one coming over and the last to play its part. */
    private static Scenario cooperate(GameTestHelper helper) {
        Treat treat = new Treat(helper, false);
        return new Scenario(treat.chiikawa, COOPERATE_STEPS, ran -> played(ran, COOPERATE_STEPS - 1), ran -> treat.untidy(),
            treat::forgetTheRest);
    }

    /** The scene begins with the step that plays it, the last of both, and the pet reports it as that step does. */
    private static Optional<String> played(Running ran, int playStep) {
        return ran.stepIndex() >= playStep ? Optional.of(Meeting.PLAYED) : Optional.empty();
    }

    /** A zombie that stands still and has one hit left in it. */
    private static Zombie foe(GameTestHelper helper, BlockPos at) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, at);
        zombie.setNoAi(true);
        zombie.setHealth(1.0F);
        return zombie;
    }

    /** The pet remembers the zombie as what hurt it, as the game's own sensor would have it a moment after a blow. */
    private static void hurtBy(AbstractPet pet, Zombie zombie) {
        pet.getBrain().setMemory(MemoryModuleType.HURT_BY_ENTITY, zombie);
    }

    /** Watered, or the farmland dries out and turns back to dirt mid-case. */
    private static BlockState moistFarmland() {
        return Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE);
    }

    /**
     * Whether the chest's lid is being held up. The lid is moved by block events, which the level runs at the end of its
     * tick, so what is waiting is run first; the game keeps whether it is up in a field of the chest's lid controller that
     * nothing outside it reads, since on the server the lid never swings.
     */
    private static boolean lidIsUp(ServerLevel level, ChestBlockEntity chest) {
        try {
            java.lang.reflect.Method runBlockEvents = ServerLevel.class.getDeclaredMethod("runBlockEvents");
            runBlockEvents.setAccessible(true);
            runBlockEvents.invoke(level);
            java.lang.reflect.Field controller = ChestBlockEntity.class.getDeclaredField("chestLidController");
            controller.setAccessible(true);
            Object lid = controller.get(chest);
            java.lang.reflect.Field shouldBeOpen = lid.getClass().getDeclaredField("shouldBeOpen");
            shouldBeOpen.setAccessible(true);
            return shouldBeOpen.getBoolean(lid);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("The chest's lid can no longer be read, the case has to be taught where it is now", e);
        }
    }
}
