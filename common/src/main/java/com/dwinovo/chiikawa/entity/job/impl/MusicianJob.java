package com.dwinovo.chiikawa.entity.job.impl;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.impl.HachiwarePet;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Musician job: only Hachiware can play the music box. */
public final class MusicianJob extends BasicJob {
    /** The one character that plays; what the animation backlog asks the others of is read from here too. */
    public static final Class<? extends AbstractPet> PLAYER = HachiwarePet.class;

    public MusicianJob(int id, int priority, TagKey<Item> toolTag) {
        super(id, priority, toolTag);
    }

    @Override
    public boolean canAssume(AbstractPet pet) {
        return PLAYER.isInstance(pet) && super.canAssume(pet);
    }
}
