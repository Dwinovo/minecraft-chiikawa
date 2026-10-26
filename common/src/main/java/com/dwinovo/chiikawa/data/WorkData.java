package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.work.Work;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkActions;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * The generated works, by the id of the errand each is: what the pet does, what kind of errand it is, what it
 * counts for a slip and what it needs. Written beside the errand's settings, see {@link ErrandFiles}. The ids of
 * the works are for this generator and for tests: nothing in the game names a work, it has what its file says.
 */
public final class WorkData {
    /** Playing the song on the held music box. */
    public static final ResourceLocation PLAY_MUSIC = id("play_music");
    /** One second of a performance, which a street performance slip counts. */
    public static final ResourceLocation PLAY_MUSIC_SECOND = id("play_music_second");

    private WorkData() {
    }

    /** @return works by errand id */
    public static Map<ResourceLocation, Work<?, ?>> all() {
        return Map.of(
            PLAY_MUSIC, new Work<>(WorkActions.BUSK, ErrandCategory.WORK, Optional.of(PLAY_MUSIC_SECOND), List.of(),
                Optional.empty()));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
    }
}
