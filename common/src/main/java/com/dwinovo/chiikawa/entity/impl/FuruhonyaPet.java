package com.dwinovo.chiikawa.entity.impl;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Furuhonya (古本屋). Shares Hachiware's skeleton and reuses its animation
 * set, but the model carries no {@code guitar}/{@code tail} props — so this
 * pet has no bespoke sound set yet (falls back to {@link AbstractPet}'s
 * silent {@code EMPTY} set) and the renderer only hides the {@code Mouth3}
 * expression bone by default (see {@code FuruhonyaRenderer}).
 */
public class FuruhonyaPet extends AbstractPet {

    public FuruhonyaPet(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
    }

    @Override
    protected Item getReviveDollItem() {
        return InitItems.FURUHONYA_DOLL.get();
    }
}
