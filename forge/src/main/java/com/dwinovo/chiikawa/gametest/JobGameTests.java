package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.carries;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.ownedPet;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;
import static com.dwinovo.chiikawa.gametest.GameTestKit.wildPet;
import static com.dwinovo.chiikawa.gametest.GameTestKit.worker;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestDontPrefix;
import net.minecraftforge.gametest.GameTestHolder;

/**
 * Jobs: what a pet takes up, from the tool in its hand, and whether it then does the work.
 *
 * <p>The tool is the whole of how a player tells a pet what to be, so these say both halves
 * of it out loud — the right tool makes a farmer and the farmer harvests, the wrong tool
 * makes something else and the crop is left standing.
 */
@GameTestHolder(namespace = Constants.MOD_ID)
@GameTestDontPrefix
public final class JobGameTests {
    private static final String BATCH = "chiikawa_jobs";
    /** Long enough to walk a few blocks, take a swing and be paid for it. */
    private static final int WORK_TICKS = 3600;
    /** How long a pet is watched not doing something before we call it proof. */
    private static final int LEAVE_IT_TICKS = 400;

    /** The floor is at y 1 and its blocks start at x and z 1: the template is laid one in. */
    private static final int GROUND = 1;
    private static final int STAND = GROUND + 1;

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /** A pet is whatever its hands say it is, and stops being it the moment they are emptied. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void the_tool_in_hand_decides_the_job(GameTestHelper helper) {
        AbstractPet pet = wildPet(helper, new BlockPos(3, STAND, 3));

        helper.assertTrue(pet.getPetJobId() == InitRegistry.NONE.get().id(),
            "a pet holding nothing already had a job");

        holding(pet, Items.WOODEN_HOE);
        helper.assertTrue(pet.getPetJobId() == InitRegistry.FARMER_ID,
            "a hoe did not make a farmer");

        holding(pet, Items.IRON_SWORD);
        helper.assertTrue(pet.getPetJobId() == InitRegistry.FENCER_ID,
            "a sword did not make a fencer");

        holding(pet, Items.BOW);
        helper.assertTrue(pet.getPetJobId() == InitRegistry.ARCHER_ID,
            "a bow did not make an archer");

        // A music box in a rabbit's hands is just a music box: playing is Hachiware's,
        // and a job nobody else can take is the sort of rule that gets lost in a refactor.
        holding(pet, InitItems.MUSIC_BOX.get());
        helper.assertTrue(pet.getPetJobId() == InitRegistry.NONE.get().id(),
            "a pet who cannot play took the musician's job anyway");

        holding(pet, Items.AIR);
        helper.assertTrue(pet.getPetJobId() == InitRegistry.NONE.get().id(),
            "an emptied hand left the pet with its old job");
        helper.succeed();
    }

    /** The one job that is not the tool's alone: only Hachiware plays. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 100)
    public static void only_hachiware_takes_the_music_box(GameTestHelper helper) {
        AbstractPet hachiware = helper.spawn(InitEntity.HACHIWARE_PET.get(), new BlockPos(3, STAND, 3));

        holding(hachiware, InitItems.MUSIC_BOX.get());
        helper.assertTrue(hachiware.getPetJobId() == InitRegistry.MUSICIAN_ID,
            "Hachiware with a music box is not a musician");
        helper.succeed();
    }

    /** The whole of a farmer's day: it finds the ripe wheat, takes it, and puts a seed back. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = WORK_TICKS)
    public static void a_farmer_harvests_and_plants_again(GameTestHelper helper) {
        AbstractPet pet = holding(worker(helper, new BlockPos(4, STAND, 4)), Items.WOODEN_HOE);
        BlockPos crop = new BlockPos(7, STAND, 4);
        ripeWheat(helper, crop);

        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.WHEAT, crop);
            helper.assertBlockProperty(crop, CropBlock.AGE, 0);
            helper.assertTrue(carries(pet, Items.WHEAT), "the farmer harvested nothing it kept");
        });
    }

    /** No hoe, no harvest: the crop is left exactly as ripe as it was found. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = LEAVE_IT_TICKS + 100)
    public static void a_pet_with_the_wrong_tool_leaves_the_crop_standing(GameTestHelper helper) {
        holding(ownedPet(helper, new BlockPos(4, STAND, 4)), Items.IRON_SWORD);
        BlockPos crop = new BlockPos(7, STAND, 4);
        ripeWheat(helper, crop);

        helper.runAtTickTime(LEAVE_IT_TICKS, () -> {
            helper.assertBlockProperty(crop, CropBlock.AGE, CropBlock.MAX_AGE);
            helper.succeed();
        });
    }

    /** Wheat grown all the way, on farmland, ready to be taken. */
    private static void ripeWheat(GameTestHelper helper, BlockPos crop) {
        helper.setBlock(crop.below(), Blocks.FARMLAND);
        BlockState ripe = Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE);
        helper.setBlock(crop, ripe);
    }
}
