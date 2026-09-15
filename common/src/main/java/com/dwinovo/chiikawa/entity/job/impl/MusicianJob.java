package com.dwinovo.chiikawa.entity.job.impl;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.impl.HachiwarePet;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Musician job: only Hachiware can play the music box. */
public final class MusicianJob extends BasicJob {
    public MusicianJob(int id, int priority, TagKey<Item> toolTag, List<ResourceLocation> intents) {
        super(id, priority, toolTag, intents);
    }

    @Override
    public boolean canAssume(AbstractPet pet) {
        return pet instanceof HachiwarePet && super.canAssume(pet);
    }
}
