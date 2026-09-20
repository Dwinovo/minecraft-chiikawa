package com.dwinovo.chiikawa.entity.impl;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.sound.PetSoundSet;
import com.dwinovo.chiikawa.sound.PetSoundSets;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class HachiwarePet extends AbstractPet {
    public HachiwarePet(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractPet.petAttributes();
    }

    @Override
    protected PetSoundSet getSoundSet() {
        return PetSoundSets.HACHIWARE;
    }

    @Override
    protected Item getReviveDollItem() {
        return InitItems.HACHIWARE_DOLL.get();
    }
}
