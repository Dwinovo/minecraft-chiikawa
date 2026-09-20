package com.dwinovo.chiikawa.data;

import com.google.common.hash.Hashing;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.SharedConstants;
import net.minecraft.core.Vec3i;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;

/**
 * Writes the in-game cases' floors to {@code data/chiikawa/structure/<id>.nbt}.
 *
 * <p>Generated rather than saved out of a world with a structure block: a floor is a size
 * and a block, and a file nobody can read in a diff is a file that quietly rots. This way
 * the test ground is described in {@link GameTestStructureData} in the same breath as
 * everything else the mod generates.
 *
 * <p>They go in the data pack rather than in a folder of SNBT beside the run configuration
 * because that is the one place both loaders look — the game asks its own structure manager
 * for a test's template before it falls back to anything else.
 */
public final class GameTestStructureProvider implements DataProvider {
    private static final String STRUCTURE_DIRECTORY = "structure";

    private final PackOutput.PathProvider pathProvider;

    public GameTestStructureProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, STRUCTURE_DIRECTORY);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf(GameTestStructureData.all().entrySet().stream()
            .map(floor -> save(cache, floor.getKey(), floor.getValue()))
            .toArray(CompletableFuture[]::new));
    }

    private CompletableFuture<?> save(CachedOutput cache, ResourceLocation id, Vec3i size) {
        byte[] bytes = compressed(floor(size));
        return CompletableFuture.runAsync(() -> {
            try {
                cache.writeIfNeeded(pathProvider.file(id, "nbt"), bytes, Hashing.sha1().hashBytes(bytes));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    /** A structure of the size asked for, solid along its bottom and empty above. */
    private static CompoundTag floor(Vec3i size) {
        CompoundTag structure = new CompoundTag();
        structure.putInt("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());
        structure.put("size", intList(size.getX(), size.getY(), size.getZ()));

        ListTag palette = new ListTag();
        palette.add(NbtUtils.writeBlockState(GameTestStructureData.GROUND));
        structure.put("palette", palette);

        ListTag blocks = new ListTag();
        for (int x = 0; x < size.getX(); x++) {
            for (int z = 0; z < size.getZ(); z++) {
                CompoundTag block = new CompoundTag();
                block.put("pos", intList(x, 0, z));
                block.putInt("state", 0);
                blocks.add(block);
            }
        }
        structure.put("blocks", blocks);
        structure.put("entities", new ListTag());
        return structure;
    }

    private static byte[] compressed(CompoundTag structure) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            NbtIo.writeCompressed(structure, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    private static ListTag intList(int... values) {
        ListTag list = new ListTag();
        for (int value : values) {
            list.add(IntTag.valueOf(value));
        }
        return list;
    }

    @Override
    public String getName() {
        return "Chiikawa Game Test Structures";
    }

    /** @return the floors this writes, for a loader that wants to log or check them */
    public static Map<ResourceLocation, Vec3i> floors() {
        return GameTestStructureData.all();
    }
}
