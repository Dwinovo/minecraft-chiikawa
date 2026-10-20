package com.dwinovo.chiikawa.entity.job.impl;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.job.api.PetCapability;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Standard pet job: holding an item with {@code toolTag} lets the pet assume it,
 * which offers the job's errands.
 */
public class BasicJob implements PetCapability {
    private final int id;
    private final int priority;
    private final TagKey<Item> toolTag;
    private final List<ResourceLocation> errands;

    public BasicJob(int id, int priority, TagKey<Item> toolTag, List<ResourceLocation> errands) {
        this.id = id;
        this.priority = priority;
        this.toolTag = toolTag;
        this.errands = List.copyOf(errands);
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public boolean canAssume(AbstractPet pet) {
        ItemStack stack = pet.getBackpack().getItem(0);
        return stack.is(toolTag);
    }

    @Override
    public List<ResourceLocation> errands() {
        return errands;
    }
}
