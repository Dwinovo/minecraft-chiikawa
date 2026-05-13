package com.dwinovo.chiikawa.entity.job.impl;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.handler.MusicianJobHandler;
import com.dwinovo.chiikawa.entity.impl.HachiwarePet;
import com.dwinovo.chiikawa.entity.job.api.IJobTickHandler;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.item.Item;

public final class MusicianJob extends BasicJob {
    public MusicianJob(int id, int priority, TagKey<Item> toolTag, IJobTickHandler tickHandler) {
        super(id, priority, toolTag, tickHandler);
    }

    @Override
    public boolean canAssume(AbstractPet pet) {
        return pet instanceof HachiwarePet && super.canAssume(pet);
    }

    @Override
    public void onDeactivated(AbstractPet pet, Brain<AbstractPet> brain) {
        MusicianJobHandler.clearMusicState(pet, brain);
    }
}
