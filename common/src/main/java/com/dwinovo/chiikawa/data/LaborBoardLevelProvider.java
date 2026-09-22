package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.task.BoardLevels;
import com.dwinovo.chiikawa.task.BoardLevelsLoader;
import com.mojang.serialization.JsonOps;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * Writes {@link LaborBoardLevelData} to {@code data/chiikawa/labor_board/levels.json}.
 * Shared by both loaders' data generators.
 */
public final class LaborBoardLevelProvider implements DataProvider {
    private final PackOutput.PathProvider pathProvider;

    public LaborBoardLevelProvider(PackOutput output) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, BoardLevelsLoader.DIRECTORY);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return DataProvider.saveStable(cache,
            BoardLevels.CODEC.encodeStart(JsonOps.INSTANCE, LaborBoardLevelData.LEVELS).getOrThrow(),
            pathProvider.json(BoardLevelsLoader.FILE));
    }

    @Override
    public String getName() {
        return "Chiikawa Labor Board Levels";
    }
}
