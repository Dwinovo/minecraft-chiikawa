package com.dwinovo.chiikawa.entity.brain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.testing.TestErrands;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * What vanilla's brain makes of a saved brain it does not entirely know, which is what an old save is to a newer
 * build and a new save to an older one. Memories are saved by id in one map; the map is read entry by entry, but
 * the entries are put together as one result, and that decides whether a stranger in the map costs the pet that
 * entry or all of its memories.
 */
class BrainSaveTest {
    private static final ResourceKey<Level> OVERWORLD = Level.OVERWORLD;
    private static final GlobalPos HOME = GlobalPos.of(OVERWORLD, new BlockPos(3, 64, -7));
    private static final long SLEPT = 123456L;
    /** The brain a build knows: the memories it lists, as {@code AbstractPet.MEMORY_TYPES} does. */
    private static Brain.Provider<LivingEntity> KNOWN;

    @BeforeAll
    static void bootstrapped() {
        TestErrands.load();
        KNOWN = Brain.provider(List.of(MemoryModuleType.HOME, MemoryModuleType.LAST_SLEPT), List.of());
    }

    @Test
    void aSavedBrainComesBackWhole() {
        Brain<LivingEntity> back = KNOWN.makeBrain(new Dynamic<>(NbtOps.INSTANCE, saved(KNOWN)));

        assertEquals(Optional.of(HOME), back.getMemory(MemoryModuleType.HOME));
        assertEquals(Optional.of(SLEPT), back.getMemory(MemoryModuleType.LAST_SLEPT));
    }

    @Test
    void aMemoryIdNobodyRegisteredWipesAllTheMemoriesOfThePet() {
        CompoundTag memories = memoriesOf(saved(KNOWN));
        CompoundTag retired = new CompoundTag();
        retired.putBoolean("value", true);
        memories.put("chiikawa:retired_memory", retired);

        Brain<LivingEntity> back = KNOWN.makeBrain(new Dynamic<>(NbtOps.INSTANCE, wrapped(memories)));

        assertEquals(Optional.empty(), back.getMemory(MemoryModuleType.HOME), "the stranger takes the rest down with it");
        assertEquals(Optional.empty(), back.getMemory(MemoryModuleType.LAST_SLEPT));
    }

    @Test
    void aMemoryRegisteredButNotInTheBrainIsLeftOutAloneAndTheRestKept() {
        CompoundTag memories = memoriesOf(saved(KNOWN));
        Brain.Provider<LivingEntity> wider = Brain.provider(List.of(MemoryModuleType.HOME, MemoryModuleType.LAST_SLEPT,
            MemoryModuleType.LAST_WOKEN), List.of());
        Brain<LivingEntity> source = wider.makeBrain(new Dynamic<>(NbtOps.INSTANCE, wrapped(memories)));
        source.setMemory(MemoryModuleType.LAST_WOKEN, 99L);
        CompoundTag withExtra = memoriesOf(serialized(source));
        assertTrue(withExtra.contains("minecraft:last_woken"), "the stranger is in the save");

        Brain<LivingEntity> back = KNOWN.makeBrain(new Dynamic<>(NbtOps.INSTANCE, wrapped(withExtra)));

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

        Brain<LivingEntity> back = KNOWN.makeBrain(new Dynamic<>(NbtOps.INSTANCE, wrapped(memories)));

        assertEquals(Optional.empty(), back.getMemory(MemoryModuleType.HOME));
        assertEquals(Optional.empty(), back.getMemory(MemoryModuleType.LAST_SLEPT));
    }

    private static Tag saved(Brain.Provider<LivingEntity> provider) {
        Brain<LivingEntity> brain = provider.makeBrain(new Dynamic<>(NbtOps.INSTANCE, wrapped(new CompoundTag())));
        brain.setMemory(MemoryModuleType.HOME, HOME);
        brain.setMemory(MemoryModuleType.LAST_SLEPT, SLEPT);
        return serialized(brain);
    }

    private static Tag serialized(Brain<LivingEntity> brain) {
        return brain.serializeStart(NbtOps.INSTANCE).getOrThrow();
    }

    private static CompoundTag memoriesOf(Tag brain) {
        return ((CompoundTag) brain).getCompound("memories");
    }

    private static CompoundTag wrapped(CompoundTag memories) {
        CompoundTag brain = new CompoundTag();
        brain.put("memories", memories);
        return brain;
    }
}
