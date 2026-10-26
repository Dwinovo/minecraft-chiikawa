package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.addSong;
import static com.dwinovo.chiikawa.gametest.GameTestKit.count;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.owned;
import static com.dwinovo.chiikawa.gametest.GameTestKit.readySong;
import static com.dwinovo.chiikawa.gametest.GameTestKit.worker;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.block.LaborBoardBlockEntity;
import com.dwinovo.chiikawa.data.PetTaskTypeData;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.job.JobIds;
import com.dwinovo.chiikawa.init.InitDataComponents;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitBlocks;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.music.MusicBoxSelection;
import com.dwinovo.chiikawa.music.PlaybackMode;
import com.dwinovo.chiikawa.music.ServerMusicLibrary;
import com.dwinovo.chiikawa.music.ServerMusicSystem;
import com.dwinovo.chiikawa.task.BoardLevels;
import com.dwinovo.chiikawa.task.BoardSlips;
import com.dwinovo.chiikawa.task.BoardSlot;
import com.dwinovo.chiikawa.task.PetTaskTypes;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The labor board, end to end: a pet that may not weed for free walks over, takes the
 * day's slip, does the work and comes away with emeralds.
 *
 * <p>This is the one case that has to hold for the whole of the mod's economy to mean
 * anything, and it is the one with the most between the first tick and the last — seeing,
 * reserving, walking, claiming, counting the work, being paid.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BoardGameTests {
    private static final String BATCH = "chiikawa_board";
    /** Everything above, at the speed a pet does it. */
    private static final int WORK_TICKS = 6000;

    private static final int STAND = 2;
    /** Grass to pull: more than the largest weeding slip asks for, so the work cannot run out. */
    private static final int WEED_PATCH = 6;
    private static final int WEED_CORNER = 2;
    /** How long two pets are watched, which is longer than one takes to walk over and claim. */
    private static final int WATCH_TICKS = 1200;
    /** The song the musician's box selects. */
    private static final String SONG = "chiikawa_gametest_board_song";
    private static final int SONG_SECONDS = 1;
    /** A song is imported in its own time, off the server thread, which the server does not wait for. */
    private static final int SONG_IMPORT_TICKS = 100000;

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /** From a bare hoe to a handful of emeralds, with nothing said to the pet along the way. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WORK_TICKS)
    public static void a_board_pays_a_farmer_for_pulling_weeds(GameTestHelper helper) {
        BlockPos board = weedingBoard(helper);
        helper.setBlock(board, InitBlocks.LABOR_BOARD.get());

        weedPatch(helper);

        AbstractPet pet = holding(worker(helper, new BlockPos(4, STAND, 4)), Items.WOODEN_HOE);
        helper.succeedWhen(() -> helper.assertTrue(count(pet, Items.EMERALD) > 0,
            "the farmer never came away with anything for its trouble"));
    }

    /**
     * Paid and straight back for more. The loop only means anything if it is a loop: a pet
     * that does one job and then stands about for the rest of the day is a pet an owner has
     * to nudge, which is the thing the board was for.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WORK_TICKS)
    public static void a_farmer_goes_back_for_the_next_slip(GameTestHelper helper) {
        BlockPos board = boardWithASecondFarmerSlip(helper);
        helper.setBlock(board, InitBlocks.LABOR_BOARD.get());
        weedPatch(helper);

        AbstractPet pet = holding(worker(helper, new BlockPos(4, STAND, 4)), Items.WOODEN_HOE);
        AtomicBoolean paid = new AtomicBoolean();
        helper.succeedWhen(() -> {
            if (count(pet, Items.EMERALD) > 0) {
                paid.set(true);
            }
            helper.assertTrue(paid.get(), "the farmer has not been paid for the first slip yet");
            // Being paid clears the slip, so one in hand afterwards is a second one.
            helper.assertTrue(pet.getTask().isPresent(), "the farmer stopped after one job");
        });
    }

    /** Grass over the pet's corner of the floor, enough for any weeding slip the board rolls. */
    private static void weedPatch(GameTestHelper helper) {
        for (int x = 0; x < WEED_PATCH; x++) {
            for (int z = 0; z < WEED_PATCH; z++) {
                BlockPos weed = new BlockPos(WEED_CORNER + x, STAND, WEED_CORNER + z);
                helper.setBlock(weed.below(), Blocks.GRASS_BLOCK);
                helper.setBlock(weed, Blocks.SHORT_GRASS);
            }
        }
    }

    private static boolean onWeedPatch(int x, int z) {
        return x >= WEED_CORNER && x < WEED_CORNER + WEED_PATCH && z >= WEED_CORNER && z < WEED_CORNER + WEED_PATCH;
    }

    /**
     * Paid with nowhere to put it. A pet whose bag is full still gets what it earned — it
     * lands at its feet rather than quietly going nowhere, which is the difference between
     * a full bag and a robbery.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WORK_TICKS)
    public static void a_farmer_with_a_full_bag_is_paid_at_its_feet(GameTestHelper helper) {
        helper.setBlock(weedingBoard(helper), InitBlocks.LABOR_BOARD.get());
        weedPatch(helper);

        AbstractPet pet = holding(worker(helper, new BlockPos(4, STAND, 4)), Items.WOODEN_HOE);
        // Everything but the hand it holds the hoe in, packed with something an emerald
        // cannot join and a farmer will not pick up again.
        for (int slot = 1; slot < pet.getBackpack().getContainerSize(); slot++) {
            pet.getBackpack().setItem(slot, new ItemStack(Items.COBBLESTONE, Items.COBBLESTONE.getDefaultMaxStackSize()));
        }

        helper.succeedWhen(() -> helper.assertItemEntityPresent(Items.EMERALD, new BlockPos(4, STAND, 4), 10.0));
    }

    /**
     * One slip, two farmers: one of them gets it and the other does not. A slip taken twice
     * is work paid for twice, and a pair of pets walking to the same board and both coming
     * away with the same job is exactly what the reservation is there to stop.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WATCH_TICKS + 200)
    public static void two_farmers_cannot_take_the_same_slip(GameTestHelper helper) {
        helper.setBlock(boardWithOneFarmerSlip(helper), InitBlocks.LABOR_BOARD.get());
        weedPatch(helper);

        AbstractPet first = holding(worker(helper, new BlockPos(3, STAND, 4)), Items.WOODEN_HOE);
        AbstractPet second = holding(worker(helper, new BlockPos(5, STAND, 4)), Items.WOODEN_HOE);
        AtomicBoolean taken = new AtomicBoolean();

        helper.startSequence()
            .thenExecuteFor(WATCH_TICKS, () -> {
                boolean firstHasOne = first.getTask().isPresent();
                boolean secondHasOne = second.getTask().isPresent();
                helper.assertFalse(firstHasOne && secondHasOne,
                    "both farmers walked away with the day's only slip");
                if (firstHasOne || secondHasOne) {
                    taken.set(true);
                }
            })
            .thenExecute(() -> helper.assertTrue(taken.get(), "neither farmer ever took the slip"))
            .thenSucceed();
    }

    /**
     * A board shows its day's slips as plates, and the one a pet takes comes down, so the
     * players nearby can see how much work is left. What they are sent is the board's
     * {@code hanging} places, and a claim has to take one away.
     */
    /**
     * Telling a player about the board changes nothing about it. The game asks while it is
     * sending the chunk's changed blocks out, and a board that put its day's slips up then —
     * and told the chunk so — lost the chunk track of that part of itself: no block broken
     * there ever reached a player again.
     */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 20)
    public static void sending_a_board_to_players_changes_nothing(GameTestHelper helper) {
        BlockPos at = new BlockPos(3, STAND, 3);
        helper.setBlock(at, InitBlocks.LABOR_BOARD.get());
        LaborBoardBlockEntity board = (LaborBoardBlockEntity) helper.getBlockEntity(at);
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        CompoundTag before = board.saveWithoutMetadata(registries);

        board.getUpdateTag(registries);

        helper.assertTrue(before.equals(board.saveWithoutMetadata(registries)),
            "asking for what players see changed the board: " + before + " became " + board.saveWithoutMetadata(registries));
        helper.succeed();
    }

    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void a_board_puts_its_plates_up_by_itself(GameTestHelper helper) {
        BlockPos at = new BlockPos(3, STAND, 3);
        helper.setBlock(at, InitBlocks.LABOR_BOARD.get());
        LaborBoardBlockEntity board = (LaborBoardBlockEntity) helper.getBlockEntity(at);

        // Nothing asks it: no pet about, no player at it.
        helper.succeedWhen(() -> helper.assertTrue(
            board.hanging() == (1 << BoardLevels.current().slipsAt(board.boardLevel())) - 1,
            "a board nobody has looked at still hangs no plates: " + Integer.toBinaryString(board.hanging())));
    }

    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WATCH_TICKS + 200)
    public static void a_plate_comes_down_when_a_pet_takes_its_slip(GameTestHelper helper) {
        BlockPos at = weedingBoard(helper);
        helper.setBlock(at, InitBlocks.LABOR_BOARD.get());
        LaborBoardBlockEntity board = (LaborBoardBlockEntity) helper.getBlockEntity(at);
        int all = (1 << board.today().size()) - 1;
        helper.assertTrue(board.hanging() == all, "a new day's board does not hang all its plates: " + board.hanging());
        weedPatch(helper);

        AbstractPet farmer = holding(worker(helper, new BlockPos(3, STAND, 4)), Items.WOODEN_HOE);

        helper.succeedWhen(() -> {
            helper.assertTrue(farmer.getTask().isPresent(), "the farmer has not taken a slip yet");
            helper.assertTrue(Integer.bitCount(board.hanging()) == Integer.bitCount(all) - 1,
                "the farmer took a slip and its plate is still up: " + Integer.toBinaryString(board.hanging()));
        });
    }

    /**
     * A slip that asks for music is not taken by a musician whose box has no song to play: the slip would be the pet's
     * for the day and it could not work on it, and no other musician could have it. The board leaves it up, and it
     * is not held for the pet either.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WATCH_TICKS + 200)
    public static void a_musician_with_an_empty_box_leaves_the_music_slip(GameTestHelper helper) {
        BlockPos at = boardWithAMusicianSlip(helper);
        helper.setBlock(at, InitBlocks.LABOR_BOARD.get());
        LaborBoardBlockEntity board = (LaborBoardBlockEntity) helper.getBlockEntity(at);
        AbstractPet musician = owned(helper, InitEntity.HACHIWARE_PET.get(), new BlockPos(4, STAND, 4));
        musician.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(InitItems.MUSIC_BOX.get()));

        helper.startSequence()
            .thenExecuteFor(WATCH_TICKS, () -> {
                helper.assertTrue(musician.getTask().isEmpty(), "the musician took a slip it could not play for");
                helper.assertFalse(board.isReservedBy(musician), "the musician held a slip it could not play for");
            })
            .thenExecute(() -> helper.assertTrue(board.today().stream().noneMatch(BoardSlot::claimed),
                "the board gave away a slip to a musician with no song"))
            .thenSucceed();
    }

    /** The same board and the same musician with a song selected: the slip is taken, so the case above is about the song. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = SONG_IMPORT_TICKS)
    public static void a_musician_with_a_song_takes_the_music_slip(GameTestHelper helper) {
        ServerMusicLibrary library = ServerMusicSystem.library(helper.getLevel().getServer());
        addSong(library, SONG, SONG_SECONDS);
        helper.setBlock(boardWithAMusicianSlip(helper), InitBlocks.LABOR_BOARD.get());
        AtomicReference<AbstractPet> musician = new AtomicReference<>();

        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(readySong(library, SONG).isPresent(), "the song was never imported"))
            .thenExecute(() -> {
                ItemStack box = new ItemStack(InitItems.MUSIC_BOX.get());
                box.set(InitDataComponents.MUSIC_BOX_SELECTION.get(),
                    new MusicBoxSelection(readySong(library, SONG).orElseThrow().trackId(), SONG, 1));
                box.set(InitDataComponents.MUSIC_BOX_MODE.get(), PlaybackMode.ONCE);
                musician.set(owned(helper, InitEntity.HACHIWARE_PET.get(), new BlockPos(4, STAND, 4)));
                musician.get().setItemSlot(EquipmentSlot.MAINHAND, box);
            })
            .thenWaitUntil(() -> helper.assertTrue(musician.get().getTask().isPresent(),
                "the musician never took the music slip"))
            .thenSucceed();
    }

    /**
     * Where to put the board so that the first thing it offers a farmer today is weeding.
     *
     * <p>A board's day is rolled from the world seed, the day and its own position, so what
     * it offers is settled before anything is placed. Rather than reach into the block and
     * plant a slip — which would have the case testing a board nobody else could get — this
     * asks the same roll the board will ask, at each spot in turn, until it finds one that
     * offers the work this case is about.
     *
     * <p>The <i>first</i> farmer's slip, not any of them: picking mushrooms is a farmer's
     * work too, and a pet takes the first slip it can rather than the one a case had in
     * mind. A board that offers that first leaves the pet holding a slip it cannot finish
     * at noon, and the case waits out its clock for nothing.
     */
    static BlockPos weedingBoard(GameTestHelper helper) {
        return boardWhere(helper, JobIds.FARMER, farmerSlips -> !farmerSlips.isEmpty()
            && farmerSlips.get(0).slip().type().equals(PetTaskTypeData.WEEDING),
            "offers a farmer weeding first");
    }

    /**
     * A board offering a farmer weeding and then something else. Without that second slip
     * there is nothing to go back for, and the case fails for want of work rather than for
     * want of a pet willing to do it.
     */
    private static BlockPos boardWithASecondFarmerSlip(GameTestHelper helper) {
        return boardWhere(helper, JobIds.FARMER, farmerSlips -> farmerSlips.size() >= 2
            && farmerSlips.get(0).slip().type().equals(PetTaskTypeData.WEEDING),
            "offers a farmer weeding and another slip after it");
    }

    /** A board with a street performance slip for a musician, which is all a musician's slips are. */
    private static BlockPos boardWithAMusicianSlip(GameTestHelper helper) {
        return boardWhere(helper, JobIds.MUSICIAN, musicianSlips -> !musicianSlips.isEmpty(),
            "offers a musician a slip");
    }

    /** A board with one slip for a farmer and no second one to fall back on. */
    private static BlockPos boardWithOneFarmerSlip(GameTestHelper helper) {
        return boardWhere(helper, JobIds.FARMER, farmerSlips -> farmerSlips.size() == 1
            && farmerSlips.get(0).slip().type().equals(PetTaskTypeData.WEEDING),
            "offers a farmer exactly one slip, for weeding");
    }

    /**
     * The first spot on the floor whose day, rolled, answers {@code wanted}. Every spot off
     * the weed patch is tried: the world's seed is new each run, and the rarest day a case
     * asks for — one farmer's slip and no second — comes up on one board in fifteen, so a
     * handful of spots leaves a case failing now and then for want of a board.
     *
     * @param job the job whose slips are looked at
     * @param wanted asked of the day's slips a pet of that job could take, in the order a pet meets them
     */
    private static BlockPos boardWhere(GameTestHelper helper, ResourceLocation job, Predicate<List<BoardSlot>> wanted,
            String what) {
        ServerLevel level = helper.getLevel();
        long day = level.getDayTime() / Level.TICKS_PER_DAY;
        for (int x = 1; x < 15; x++) {
            for (int z = 1; z < 15; z++) {
                if (onWeedPatch(x, z)) {
                    continue;
                }
                BlockPos rel = new BlockPos(x, STAND, z);
                long seed = BoardSlips.seed(level.getSeed(), day, helper.absolutePos(rel));
                List<BoardSlot> jobSlips = BoardSlips.roll(seed, PetTaskTypes.all(), BoardLevels.current(), BoardLevels.FIRST_LEVEL)
                    .stream()
                    .filter(slot -> slot.slip().capability().equals(job))
                    .toList();
                if (wanted.test(jobSlips)) {
                    return rel;
                }
            }
        }
        throw new AssertionError("nowhere on this floor " + what + " today — "
            + "the roll, the weights or the slip types changed");
    }
}
