package com.dwinovo.chiikawa.entity.job.impl;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.job.api.IJobTickHandler;
import com.dwinovo.chiikawa.entity.job.api.IPetJob;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Standard pet job: holding an item with {@code toolTag} causes the pet to
 * adopt this job (via {@link #canAssume}); the {@link #tickBrain} delegate
 * selects which of the job's pre-registered brain activities to run each
 * server tick.
 *
 * <p>Brain initialisation has moved out of jobs — see
 * {@link com.dwinovo.chiikawa.entity.AbstractPet#makeBrain} which registers
 * every job's activities once at construction. This class only carries the
 * tick-time logic.
 */
public class BasicJob implements IPetJob {
    private final int id;
    private final int priority;
    private final TagKey<Item> toolTag;
    private final IJobTickHandler tickHandler;

    public BasicJob(int id, int priority, TagKey<Item> toolTag, IJobTickHandler tickHandler) {
        this.id = id;
        this.priority = priority;
        this.toolTag = toolTag;
        this.tickHandler = tickHandler;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public boolean canAssume(AbstractPet pet) {
        ItemStack stack = pet.getBackpack().getItem(0);
        return stack.is(toolTag);
    }

    @Override
    public void tickBrain(AbstractPet pet, Brain<AbstractPet> brain) {
        tickHandler.tick(pet, brain);
    }
}
