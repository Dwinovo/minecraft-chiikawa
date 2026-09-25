package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.owned;
import static com.dwinovo.chiikawa.gametest.GameTestKit.ownedPet;
import static com.dwinovo.chiikawa.gametest.GameTestKit.player;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.Vec3;

/**
 * What an owner's word is worth, and what is left when a pet is gone.
 *
 * <p>Sitting is the one instruction a player gives when they mean "whatever you can see,
 * leave it" — a pet that wanders off to a crop because it noticed one has stopped being a
 * pet you can put somewhere.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DirectiveGameTests {
    private static final String BATCH = "chiikawa_directive";
    /** How long a sitting pet is watched sitting before we believe it will keep sitting. */
    private static final int SIT_TICKS = 600;
    private static final int DEATH_TICKS = 200;
    /** Long enough for a pet to get round to looking up several times over. */
    private static final int GLANCE_TICKS = 400;
    /** A pet shifts about where it sits; this is more than fidgeting and less than a walk. */
    private static final double WANDERED = 2.0;

    private static final int STAND = 2;

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /** Told to sit, with ripe wheat in plain sight: it sits, and the wheat stays ripe. */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = SIT_TICKS + 100)
    public static void a_sitting_pet_leaves_the_work_alone(GameTestHelper helper) {
        AbstractPet pet = holding(ownedPet(helper, new BlockPos(4, STAND, 4)), Items.WOODEN_HOE);
        pet.setPetDirective(PetDirective.STAY);
        BlockPos seat = pet.blockPosition();

        BlockPos crop = new BlockPos(7, STAND, 4);
        helper.setBlock(crop.below(), Blocks.FARMLAND);
        helper.setBlock(crop, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE));

        helper.runAtTickTime(SIT_TICKS, () -> {
            helper.assertBlockProperty(crop, CropBlock.AGE, CropBlock.MAX_AGE);
            helper.assertTrue(pet.blockPosition().distSqr(seat) < WANDERED * WANDERED,
                Component.literal("a sitting pet got up and went to work"));
            helper.succeed();
        });
    }

    /**
     * Sitting still is not the same as ignoring everyone: a sitting pet still looks at who
     * comes by, as its character would. Momonga, who wants nothing more than to be looked
     * at, soon turns to a player standing near it.
     */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = GLANCE_TICKS)
    public static void a_sitting_pet_looks_at_who_comes_by(GameTestHelper helper) {
        AbstractPet pet = owned(helper, InitEntity.MOMONGA_PET.get(), new BlockPos(2, STAND, 2));
        pet.setPetDirective(PetDirective.STAY);
        ServerPlayer visitor = player(helper);
        visitor.setGameMode(GameType.SURVIVAL);
        visitor.snapTo(helper.absoluteVec(new Vec3(5.5, STAND, 5.5)));

        helper.succeedWhen(() -> helper.assertTrue(pet.getBrain().getMemory(MemoryModuleType.LOOK_TARGET)
                .filter(look -> look instanceof EntityTracker tracker && tracker.getEntity() == visitor)
                .isPresent(),
            Component.literal("the sitting pet never looked at the player beside it")));
    }

    /**
     * A pet that dies leaves a doll and nothing else. What it was holding is in the doll,
     * not on the floor — the alternative is an owner hunting through the grass for the hoe
     * they gave it.
     */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = DEATH_TICKS)
    public static void a_dead_pet_keeps_its_tool_in_the_doll(GameTestHelper helper) {
        AbstractPet pet = holding(ownedPet(helper, new BlockPos(3, STAND, 3)), Items.WOODEN_HOE);
        pet.hurt(pet.damageSources().generic(), pet.getMaxHealth() * 2.0F);

        helper.runAtTickTime(DEATH_TICKS / 2, () -> {
            helper.assertTrue(pet.isDeadOrDying(), Component.literal("the pet shrugged off a killing blow"));
            helper.assertItemEntityNotPresent(Items.WOODEN_HOE, new BlockPos(3, STAND, 3), 6.0);
            helper.assertItemEntityPresent(InitItems.USAGI_DOLL.get(), new BlockPos(3, STAND, 3), 6.0);
            helper.succeed();
        });
    }
}
