package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.ownedPet;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestLedger;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestStore;
import com.dwinovo.chiikawa.init.InitMemory;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The rests a pet keeps through a save, which the unit tests of the ledger cannot reach: they are
 * kept in the brain's memories, and only a pet written out and read back shows what the memories keep.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RestGameTests {
    private static final String BATCH = "chiikawa_rest";
    private static final int STAND = 2;
    private static final int PHOTO_REST = 9000;
    private static final int GIFT_REST = 4000;
    private static final int SHOP_REST = 700;
    private static final int CROP_REST = 200;
    private static final String CROP = "2,2,2";

    /** Where each errand's rests live, as the errands that go native will say. */
    private static final Map<ResourceLocation, RestStore.Kind> KINDS = Map.of(
        ErrandIds.TAKE_PHOTO, RestStore.memory(InitMemory.PHOTO_COOLDOWN),
        ErrandIds.GIFT_OWNER, RestStore.memory(InitMemory.GIFT_COOLDOWN),
        ErrandIds.SHOP, RestStore.shared(),
        ErrandIds.HARVEST, RestStore.subjects());

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /**
     * A pet saved with the photo and gift cooldowns running and read back is resting for as long as it
     * was, as the ledger tells it; and what the ledger writes to those stores is what the memories said
     * before it. A world saved by 0.1.3 keeps its pets' rests.
     */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 20)
    public static void a_pet_saved_with_photo_and_gift_rests_running_is_resting_as_long_after_it_is_read_back(
            GameTestHelper helper) {
        AbstractPet pet = ownedPet(helper, new BlockPos(3, STAND, 3));
        pet.getBrain().setMemoryWithExpiry(InitMemory.PHOTO_COOLDOWN.get(), Unit.INSTANCE, PHOTO_REST);
        pet.getBrain().setMemoryWithExpiry(InitMemory.GIFT_COOLDOWN.get(), Unit.INSTANCE, GIFT_REST);
        long now = helper.getLevel().getGameTime();
        RestLedger before = ledgerOf(pet);

        AbstractPet loaded = readBack(helper, pet);
        RestLedger after = ledgerOf(loaded);

        helper.assertTrue(before.remaining(ErrandIds.TAKE_PHOTO, now) == PHOTO_REST,
            "the ledger said " + before.remaining(ErrandIds.TAKE_PHOTO, now) + " ticks of photo rest, not " + PHOTO_REST);
        helper.assertTrue(after.remaining(ErrandIds.TAKE_PHOTO, now) == PHOTO_REST,
            "after a save the photo rest was " + after.remaining(ErrandIds.TAKE_PHOTO, now) + " ticks, not " + PHOTO_REST);
        helper.assertTrue(after.remaining(ErrandIds.GIFT_OWNER, now) == GIFT_REST,
            "after a save the gift rest was " + after.remaining(ErrandIds.GIFT_OWNER, now) + " ticks, not " + GIFT_REST);
        helper.assertTrue(after.resting(ErrandIds.TAKE_PHOTO, now) && after.resting(ErrandIds.GIFT_OWNER, now),
            "a pet that was resting is not after a save");
        helper.succeed();
    }

    /** A rest written through the ledger is in the memory the rest of the mod reads. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 20)
    public static void a_rest_the_ledger_writes_to_a_legacy_memory_is_the_memory_the_mod_reads(GameTestHelper helper) {
        AbstractPet pet = ownedPet(helper, new BlockPos(3, STAND, 3));
        long now = helper.getLevel().getGameTime();

        KINDS.get(ErrandIds.TAKE_PHOTO).of(pet.getBrain(), ErrandIds.TAKE_PHOTO).restFor(null, PHOTO_REST, now);

        helper.assertTrue(pet.getBrain().hasMemoryValue(InitMemory.PHOTO_COOLDOWN.get()), "the photo cooldown was not set");
        helper.assertTrue(pet.getBrain().getTimeUntilExpiry(InitMemory.PHOTO_COOLDOWN.get()) == PHOTO_REST,
            "the memory expires in " + pet.getBrain().getTimeUntilExpiry(InitMemory.PHOTO_COOLDOWN.get()));

        KINDS.get(ErrandIds.TAKE_PHOTO).of(pet.getBrain(), ErrandIds.TAKE_PHOTO).restFor(null, 0, now);

        helper.assertFalse(pet.getBrain().hasMemoryValue(InitMemory.PHOTO_COOLDOWN.get()), "an ended rest was left in the memory");
        helper.succeed();
    }

    /** The errands' own shared rests are saved with the pet, and the rests on a subject are not. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 20)
    public static void shared_rests_are_saved_and_rests_on_a_subject_are_not(GameTestHelper helper) {
        AbstractPet pet = ownedPet(helper, new BlockPos(3, STAND, 3));
        long now = helper.getLevel().getGameTime();
        RestLedger ledger = ledgerOf(pet);
        KINDS.get(ErrandIds.SHOP).of(pet.getBrain(), ErrandIds.SHOP).restFor(null, SHOP_REST, now);
        KINDS.get(ErrandIds.HARVEST).of(pet.getBrain(), ErrandIds.HARVEST).restFor(CROP, CROP_REST, now);
        helper.assertTrue(ledger.remaining(ErrandIds.SHOP, now) == SHOP_REST, "the shop rest was not written");
        helper.assertTrue(ledger.resting(ErrandIds.HARVEST, CROP, now), "the rest on the crop was not written");

        RestLedger after = ledgerOf(readBack(helper, pet));

        helper.assertTrue(after.remaining(ErrandIds.SHOP, now) == SHOP_REST,
            "after a save the shop rest was " + after.remaining(ErrandIds.SHOP, now) + " ticks, not " + SHOP_REST);
        helper.assertFalse(after.resting(ErrandIds.HARVEST, CROP, now), "a rest on a subject survived a save");
        helper.succeed();
    }

    private static RestLedger ledgerOf(AbstractPet pet) {
        return new RestLedger(errand -> KINDS.get(errand).of(pet.getBrain(), errand), ErrandSettings::of);
    }

    /** The pet as the game reads it from a save of the pet, not yet in the world. */
    private static AbstractPet readBack(GameTestHelper helper, AbstractPet pet) {
        CompoundTag saved = new CompoundTag();
        pet.save(saved);
        return (AbstractPet) EntityType.create(saved, helper.getLevel()).orElseThrow();
    }
}
