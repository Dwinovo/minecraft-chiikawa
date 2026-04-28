package com.dwinovo.chiikawa.entity.impl;

import com.dwinovo.chiikawa.anim.api.ChiikawaAnimated;
import com.dwinovo.chiikawa.anim.runtime.PetAnimator;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.sound.PetSoundSet;
import com.dwinovo.chiikawa.sound.PetSoundSets;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class ChiikawaPet extends AbstractPet implements ChiikawaAnimated {

    // Lazily allocated on first read. The animator only matters on the client,
    // so server-side instances never pay the allocation. Phase 4 will move the
    // field up to AbstractPet alongside the GeckoLib removal.
    private PetAnimator petAnimator;

    public ChiikawaPet(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
    }

    @Override
    public PetAnimator getPetAnimator() {
        if (petAnimator == null) {
            petAnimator = new PetAnimator();
        }
        return petAnimator;
    }

    @Override
    protected PetSoundSet getSoundSet() {
        return PetSoundSets.CHIIKAWA;
    }

    @Override
    protected Item getReviveDollItem() {
        return InitItems.CHIIKAWA_DOLL.get();
    }
}
