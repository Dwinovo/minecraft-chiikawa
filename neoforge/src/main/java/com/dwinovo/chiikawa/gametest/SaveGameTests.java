package com.dwinovo.chiikawa.gametest;

import static com.dwinovo.chiikawa.gametest.GameTestKit.NOON;
import static com.dwinovo.chiikawa.gametest.GameTestKit.carries;
import static com.dwinovo.chiikawa.gametest.GameTestKit.holding;
import static com.dwinovo.chiikawa.gametest.GameTestKit.ownedPet;
import static com.dwinovo.chiikawa.gametest.GameTestKit.settleWorld;
import static com.dwinovo.chiikawa.gametest.GameTestKit.worker;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.init.InitMemory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What a pet keeps of its memories when its save holds one the build has no place for. Vanilla reads the saved
 * brain entry by entry but puts the entries together as one result, so the answer is not plain from the save: it
 * decides whether a memory may be retired by taking it out of the pet's list, or has to stay registered for the
 * saves that still hold it, and what an older build makes of the save of a newer one.
 */
@GameTestHolder(Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SaveGameTests {
    private static final String BATCH = "chiikawa_save";
    private static final int STAND = 2;
    private static final int PHOTO_REST = 9000;
    private static final int GIFT_REST = 4000;
    private static final long SHOP_REST_UNTIL = 80_000L;
    private static final ResourceLocation PLAINS = ResourceLocation.withDefaultNamespace("plains");
    private static final GlobalPos HOME = GlobalPos.of(Level.OVERWORLD, new BlockPos(1, 64, 1));
    /** Registered by nobody: what a memory the build has retired, or never had, is to the build. */
    private static final String UNREGISTERED = "chiikawa:retired_memory";
    /** Registered by vanilla, and not in the list of memories a pet has. */
    private static final String UNLISTED = "minecraft:last_slept";
    /** The memories 0.1.3 saved for the blocks a farmer sees, which are registered and not in the list a pet has. */
    private static final List<String> RETIRED_FARM_BLOCKS = List.of("chiikawa:harvest_pos", "chiikawa:plant_pos",
        "chiikawa:container_pos", "chiikawa:weed_pos", "chiikawa:mushroom_pos");

    /**
     * One memory the mod saves, as a pet that has been about has it.
     *
     * @param name what to call it in a failure
     * @param remember gives it to the pet
     * @param remembers whether the pet has it as it was given
     */
    private record Memory(String name, Consumer<AbstractPet> remember, Predicate<AbstractPet> remembers) {
    }

    private static final List<Memory> MEMORIES = List.of(
        new Memory("home", pet -> pet.getBrain().setMemory(MemoryModuleType.HOME, HOME),
            pet -> pet.getBrain().getMemory(MemoryModuleType.HOME).equals(Optional.of(HOME))),
        new Memory("photo_cooldown",
            pet -> pet.getBrain().setMemoryWithExpiry(InitMemory.PHOTO_COOLDOWN.get(), Unit.INSTANCE, PHOTO_REST),
            pet -> pet.getBrain().hasMemoryValue(InitMemory.PHOTO_COOLDOWN.get())),
        new Memory("gift_cooldown",
            pet -> pet.getBrain().setMemoryWithExpiry(InitMemory.GIFT_COOLDOWN.get(), Unit.INSTANCE, GIFT_REST),
            pet -> pet.getBrain().hasMemoryValue(InitMemory.GIFT_COOLDOWN.get())),
        new Memory("errand_rests",
            pet -> pet.getBrain().setMemory(InitMemory.ERRAND_RESTS.get(), Map.of(ErrandIds.SHOP, SHOP_REST_UNTIL)),
            pet -> pet.getBrain().getMemory(InitMemory.ERRAND_RESTS.get()).equals(Optional.of(Map.of(ErrandIds.SHOP, SHOP_REST_UNTIL)))),
        new Memory("photographed_biomes", pet -> pet.getBrain().setMemory(InitMemory.PHOTOGRAPHED_BIOMES.get(), List.of(PLAINS)),
            pet -> pet.getBrain().getMemory(InitMemory.PHOTOGRAPHED_BIOMES.get()).equals(Optional.of(List.of(PLAINS)))));

    @BeforeBatch(batch = BATCH)
    public static void settle(ServerLevel level) {
        settleWorld(level, Difficulty.NORMAL, NOON);
    }

    /** The control: with nothing in the save the pet has no place for, every memory comes back. */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 20)
    public static void a_save_of_a_pet_with_memories_loads_with_all_of_them(GameTestHelper helper) {
        AbstractPet loaded = readBack(helper, remembering(helper), saved -> {
        });

        helper.assertTrue(forgotten(loaded).isEmpty(), "the pet lost " + forgotten(loaded) + " in a save and a load");
        helper.succeed();
    }

    /**
     * A memory id nobody registered in the save takes every other memory of the pet down with it: the home, the
     * cooldowns, the rests, the photographed biomes. So an id a save may hold stays registered, and a build that does not
     * know an id a newer build saved, as an older build does not know the rests the errands keep, loses these.
     */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 20)
    public static void a_save_holding_a_memory_nobody_registered_loads_with_none_of_the_memories(GameTestHelper helper) {
        AbstractPet loaded = readBack(helper, remembering(helper), saved -> {
            CompoundTag retired = new CompoundTag();
            retired.putBoolean("value", true);
            memoriesOf(saved).put(UNREGISTERED, retired);
        });

        helper.assertTrue(forgotten(loaded).size() == MEMORIES.size(),
            "the pet kept all but " + forgotten(loaded) + " of its memories; vanilla was found to drop every one");
        helper.succeed();
    }

    /**
     * A memory that is registered but not in the pet's list is left out alone: the rest of the save loads. So a
     * memory is retired from a pet by taking it out of the list and keeping it registered.
     */
    @GameTest(template = "floor8", batch = BATCH, timeoutTicks = 20)
    public static void a_save_holding_a_registered_memory_the_pet_has_no_place_for_loads_with_the_others(GameTestHelper helper) {
        AbstractPet loaded = readBack(helper, remembering(helper), saved -> {
            CompoundTag slept = new CompoundTag();
            slept.putLong("value", 5L);
            memoriesOf(saved).put(UNLISTED, slept);
        });

        helper.assertTrue(forgotten(loaded).isEmpty(), "the pet lost " + forgotten(loaded) + " for a memory it has no place for");
        helper.assertFalse(memoriesOf(save(loaded)).contains(UNLISTED), "the memory it has no place for was saved again");
        helper.succeed();
    }

    /**
     * The farm blocks a pet of 0.1.3 and before saved were retired for the work sensor's, which are not saved. A farmer
     * loaded from such a save keeps every other memory and drops the old blocks alone, and finds its crop again at the
     * sensor's next scan and goes on harvesting.
     */
    @GameTest(template = "floor16", batch = BATCH, timeoutTicks = 1200)
    public static void a_farmer_loaded_from_a_save_holding_the_retired_farm_blocks_keeps_the_rest_and_goes_on_harvesting(
            GameTestHelper helper) {
        AbstractPet farmer = holding(worker(helper, new BlockPos(4, STAND, 4)), Items.WOODEN_HOE);
        // Its home is where it works, unlike the home the other cases give a pet, which is not in this test's world.
        GlobalPos home = GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(new BlockPos(4, STAND, 4)));
        MEMORIES.stream().filter(memory -> !memory.name().equals("home")).forEach(memory -> memory.remember().accept(farmer));
        farmer.getBrain().setMemory(MemoryModuleType.HOME, home);
        CompoundTag saved = save(farmer);
        for (String retired : RETIRED_FARM_BLOCKS) {
            CompoundTag block = new CompoundTag();
            block.put("value", new IntArrayTag(new int[] {5, 64, 6}));
            memoriesOf(saved).put(retired, block);
        }
        farmer.discard();
        AbstractPet loaded = (AbstractPet) EntityType.create(saved, helper.getLevel()).orElseThrow();
        // The one it was saved from is only removed at the end of the tick, and a level takes no second creature of one id.
        loaded.setUUID(UUID.randomUUID());
        helper.assertTrue(helper.getLevel().addFreshEntity(loaded), "the loaded farmer was not added to the level");

        helper.assertTrue(lostBesidesHome(loaded, home).isEmpty(),
            "the farmer lost " + lostBesidesHome(loaded, home) + " for the farm blocks it had saved");
        for (String retired : RETIRED_FARM_BLOCKS) {
            helper.assertFalse(memoriesOf(save(loaded)).contains(retired), retired + " was saved again");
        }
        BlockPos crop = new BlockPos(7, STAND, 4);
        helper.setBlock(crop.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE));
        helper.setBlock(crop, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE));

        helper.succeedWhen(() -> {
            helper.assertTrue(carries(loaded, Items.WHEAT), "the farmer had not reaped the crop");
            helper.assertTrue(lostBesidesHome(loaded, home).isEmpty(), "the farmer lost " + lostBesidesHome(loaded, home) + " on the way");
        });
    }

    /** @return the names of the memories the farmer no longer has as it was given them, its home being the one it was given */
    private static List<String> lostBesidesHome(AbstractPet pet, GlobalPos home) {
        List<String> lost = new ArrayList<>(forgotten(pet).stream().filter(name -> !name.equals("home")).toList());
        if (!pet.getBrain().getMemory(MemoryModuleType.HOME).equals(Optional.of(home))) {
            lost.add("home");
        }
        return lost;
    }

    private static AbstractPet remembering(GameTestHelper helper) {
        AbstractPet pet = ownedPet(helper, new BlockPos(3, STAND, 3));
        MEMORIES.forEach(memory -> memory.remember().accept(pet));
        return pet;
    }

    /** @return the names of the memories the pet no longer has as it was given them */
    private static List<String> forgotten(AbstractPet pet) {
        return MEMORIES.stream().filter(memory -> !memory.remembers().test(pet)).map(Memory::name).toList();
    }

    /** The pet as the game reads it from a save of the pet, with the save changed first as another build might have. */
    private static AbstractPet readBack(GameTestHelper helper, AbstractPet pet, Consumer<CompoundTag> change) {
        CompoundTag saved = save(pet);
        change.accept(saved);
        return (AbstractPet) EntityType.create(saved, helper.getLevel()).orElseThrow();
    }

    private static CompoundTag save(AbstractPet pet) {
        CompoundTag saved = new CompoundTag();
        pet.save(saved);
        return saved;
    }

    private static CompoundTag memoriesOf(CompoundTag saved) {
        return saved.getCompound("Brain").getCompound("memories");
    }
}
