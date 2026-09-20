package com.dwinovo.chiikawa.entity.impl;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.sound.PetSoundSet;
import com.dwinovo.chiikawa.sound.PetSoundSets;

import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Item;

public class UsagiPet extends AbstractPet{

    public UsagiPet(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return AbstractPet.petAttributes();
    }

    @Override
    protected PetSoundSet getSoundSet() {
        return PetSoundSets.USAGI;
    }

    @Override
    protected Item getReviveDollItem() {
        return InitItems.USAGI_DOLL.get();
    }
}
