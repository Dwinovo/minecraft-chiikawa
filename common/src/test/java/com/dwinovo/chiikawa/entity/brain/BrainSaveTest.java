package com.dwinovo.chiikawa.entity.brain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.testing.TestErrands;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * What vanilla's brain makes of a saved brain it does not entirely know, which is what an old save is to a newer
 * build and a new save to an older one. Memories are saved by id in one map; the map is read entry by entry, and
 * that decides whether a stranger in the map costs the pet that entry or all of its memories. Before 26.1 the
 * entries were put together as one result and a stranger cost all of them, which the names of these cases still
 * say; since 26.1 the game keeps what it could read of a map it could not read whole, as the pet's save is read here.
 */
class BrainSaveTest {
    private static final ResourceKey<Level> OVERWORLD = Level.OVERWORLD;
    private static final GlobalPos HOME = GlobalPos.of(OVERWORLD, new BlockPos(3, 64, -7));
    private static final long SLEPT = 123456L;
    /** The memories a build knows: the ones it lists, as {@code AbstractPet.MEMORY_TYPES} does. */
    private static final List<MemoryModuleType<?>> KNOWN = List.of(MemoryModuleType.HOME, MemoryModuleType.LAST_SLEPT);

    @BeforeAll
    static void bootstrapped() {
        TestErrands.load();
    }

    @Test
    void aSavedBrainComesBackWhole() {
        Brain<LivingEntity> back = loaded(KNOWN, saved(KNOWN));

        assertEquals(Optional.of(HOME), back.getMemory(MemoryModuleType.HOME));
        assertEquals(Optional.of(SLEPT), back.getMemory(MemoryModuleType.LAST_SLEPT));
    }

    @Test
    void aMemoryIdNobodyRegisteredWipesAllTheMemoriesOfThePet() {
        CompoundTag memories = memoriesOf(saved(KNOWN));
        CompoundTag retired = new CompoundTag();
        retired.putBoolean("value", true);
        memories.put("chiikawa:retired_memory", retired);

        Brain<LivingEntity> back = loaded(KNOWN, wrapped(memories));

        assertEquals(Optional.of(HOME), back.getMemory(MemoryModuleType.HOME), "the stranger takes nothing down with it");
        assertEquals(Optional.of(SLEPT), back.getMemory(MemoryModuleType.LAST_SLEPT));
    }

    @Test
    void aMemoryRegisteredButNotInTheBrainIsLeftOutAloneAndTheRestKept() {
        CompoundTag memories = memoriesOf(saved(KNOWN));
        List<MemoryModuleType<?>> wider = List.of(MemoryModuleType.HOME, MemoryModuleType.LAST_SLEPT,
            MemoryModuleType.LAST_WOKEN);
        Brain<LivingEntity> source = loaded(wider, wrapped(memories));
        source.setMemory(MemoryModuleType.LAST_WOKEN, 99L);
        CompoundTag withExtra = memoriesOf(serialized(source));
        assertTrue(withExtra.contains("minecraft:last_woken"), "the stranger is in the save");

        Brain<LivingEntity> back = loaded(KNOWN, wrapped(withExtra));

        assertEquals(Optional.of(HOME), back.getMemory(MemoryModuleType.HOME));
        assertEquals(Optional.of(SLEPT), back.getMemory(MemoryModuleType.LAST_SLEPT));
        assertFalse(memoriesOf(serialized(back)).contains("minecraft:last_woken"), "it has nowhere to be kept, and is not saved again");
        assertThrows(IllegalStateException.class, () -> back.getMemory(MemoryModuleType.LAST_WOKEN), "nor may it be asked for");
    }

    @Test
    void aMemoryWhoseValueNoLongerReadsWipesAllTheMemoriesOfThePetToo() {
        CompoundTag memories = memoriesOf(saved(KNOWN));
        CompoundTag broken = new CompoundTag();
        broken.putString("value", "not a time");
        memories.put("minecraft:last_slept", broken);

        Brain<LivingEntity> back = loaded(KNOWN, wrapped(memories));

        assertEquals(Optional.of(HOME), back.getMemory(MemoryModuleType.HOME));
        assertEquals(Optional.empty(), back.getMemory(MemoryModuleType.LAST_SLEPT), "only the memory that no longer reads is lost");
    }

    private static Tag saved(List<MemoryModuleType<?>> memories) {
        Brain<LivingEntity> brain = loaded(memories, wrapped(new CompoundTag()));
        brain.setMemory(MemoryModuleType.HOME, HOME);
        brain.setMemory(MemoryModuleType.LAST_SLEPT, SLEPT);
        return serialized(brain);
    }

    private static Tag serialized(Brain<LivingEntity> brain) {
        return Brain.Packed.CODEC.encodeStart(NbtOps.INSTANCE, brain.pack()).getOrThrow();
    }

    /**
     * A brain over the given memories, made from a save the way the game makes a mob's: what could be read of the
     * save is kept, and a save of which nothing could is no memories.
     */
    private static Brain<LivingEntity> loaded(List<MemoryModuleType<?>> memories, Tag saved) {
        Brain.Packed packed = Brain.Packed.CODEC.parse(NbtOps.INSTANCE, saved).resultOrPartial(error -> { })
            .orElse(Brain.Packed.EMPTY);
        return new Brain<LivingEntity>(memories, List.of(), List.of(), packed.memories(), RandomSource.create()) { };
    }

    private static CompoundTag memoriesOf(Tag brain) {
        return ((CompoundTag) brain).getCompoundOrEmpty("memories");
    }

    private static CompoundTag wrapped(CompoundTag memories) {
        CompoundTag brain = new CompoundTag();
        brain.put("memories", memories);
        return brain;
    }
}
