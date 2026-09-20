package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.ownedPet;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Where a pet comes from and what happens after it is gone: the tool a wild one is born
 * with, and the doll that brings a dead one back with everything it had.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LifeGameTests {
    private static final String BATCH = "chiikawa_life";
    private static final int RITUAL_TICKS = 600;

    private static final int STAND = 2;

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /**
     * A pet the world spawned for itself arrives with a tool and a life of its own. Which
     * tool is the personality's business and may be either of two, so the case asks only
     * that its hands are not empty — a wild pet holding nothing is a pet with no job, and
     * nothing to do all day.
     */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 200)
    public static void a_wild_pet_is_born_with_a_tool(GameTestHelper helper) {
        BlockPos at = helper.absolutePos(new BlockPos(3, STAND, 3));
        AbstractPet pet = InitEntity.USAGI_PET.get().spawn(helper.getLevel(), at, MobSpawnType.NATURAL);

        helper.assertTrue(pet != null, "nothing spawned");
        helper.assertFalse(pet.getMainHandItem().isEmpty(), "a wild pet turned up empty-handed");
        helper.assertTrue(pet.getPetDirective() == PetDirective.FREE,
            "a wild pet was not left to its own devices");
        helper.succeed();
    }

    /**
     * The whole of dying and coming back: a pet with a hoe is killed, leaves a doll, and the
     * doll on a cake brings it back still holding the hoe. Somebody has to be able to undo
     * an accident, and undoing it has to give back what was lost, or the doll is a souvenir
     * rather than a second chance.
     */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = RITUAL_TICKS)
    public static void a_doll_on_a_cake_brings_the_pet_back_with_its_tool(GameTestHelper helper) {
        AbstractPet pet = holding(ownedPet(helper, new BlockPos(3, STAND, 3)), Items.WOODEN_HOE);
        pet.hurt(pet.damageSources().generic(), pet.getMaxHealth() * 2.0F);

        BlockPos cake = new BlockPos(5, STAND, 3);
        helper.setBlock(cake, Blocks.CAKE);

        helper.runAtTickTime(40, () -> {
            ItemStack doll = dollNear(helper, new BlockPos(3, STAND, 3));
            Player player = GameTestKit.owner(helper);
            player.setItemInHand(InteractionHand.MAIN_HAND, doll);
            helper.useBlock(cake, player);
        });

        helper.succeedWhen(() -> {
            AbstractPet revived = helper.getLevel()
                .getEntitiesOfClass(AbstractPet.class, new AABB(helper.absolutePos(cake)).inflate(6.0))
                .stream()
                .filter(AbstractPet::isAlive)
                .findFirst()
                .orElse(null);
            helper.assertTrue(revived != null, "nothing came back from the cake");
            helper.assertTrue(revived.getMainHandItem().is(Items.WOODEN_HOE),
                "the pet came back without the hoe it died with");
        });
    }

    /** The doll the dead pet left, with everything it was carrying written into it. */
    private static ItemStack dollNear(GameTestHelper helper, BlockPos rel) {
        List<ItemEntity> dropped = helper.getLevel()
            .getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(rel)).inflate(6.0));
        return dropped.stream()
            .map(ItemEntity::getItem)
            .filter(stack -> stack.is(InitItems.USAGI_DOLL.get()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("the pet died without leaving a doll"));
    }
}
