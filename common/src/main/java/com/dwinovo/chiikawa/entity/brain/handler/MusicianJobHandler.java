package com.dwinovo.chiikawa.entity.brain.handler;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.PetActivities;
import com.dwinovo.chiikawa.entity.brain.task.musician.PlayMusicBehavior;
import com.dwinovo.chiikawa.init.InitActivity;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import java.util.Set;
import net.minecraft.world.entity.ai.Brain;

/**
 * Brain wiring for the {@code MUSICIAN} pet job. The {@code play_music} intent
 * decides when the performance activity runs.
 */
public final class MusicianJobHandler {
    private MusicianJobHandler() {
    }

    public static void registerActivities(Brain<AbstractPet> brain) {
        PetActivities.register(brain, InitActivity.MUSICIAN_PLAY.get(), ImmutableList.of(
            Pair.of(0, new PlayMusicBehavior())
        ), Set.of());
    }
}
