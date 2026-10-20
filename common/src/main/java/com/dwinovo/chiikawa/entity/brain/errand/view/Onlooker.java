package com.dwinovo.chiikawa.entity.brain.errand.view;

import com.dwinovo.chiikawa.entity.brain.personality.IdleHabits;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;

/** Whom a pet with nothing to do may look at, and the idle habit that says how near and for how long. */
public enum Onlooker {
    /** A player. */
    PLAYER {
        @Override
        public boolean matches(LivingEntity entity) {
            return entity.getType() == EntityType.PLAYER;
        }

        @Override
        public IdleHabits.Glance habit(IdleHabits habits) {
            return habits.lookAtPlayer();
        }
    },
    /** An animal, or another pet. */
    CREATURE {
        @Override
        public boolean matches(LivingEntity entity) {
            return entity.getType().getCategory() == MobCategory.CREATURE;
        }

        @Override
        public IdleHabits.Glance habit(IdleHabits habits) {
            return habits.lookAtCreature();
        }
    };

    /** @return whether this entity is one of them */
    public abstract boolean matches(LivingEntity entity);

    /** @return the habit of looking at them */
    public abstract IdleHabits.Glance habit(IdleHabits habits);
}
