package com.dwinovo.chiikawa.entity.brain.errand.step;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * What a step is about, as a place, a creature or a view: where a walk goes, what a gaze rests on. A creature is
 * kept by its id and looked for each time, since it moves, may be gone, and a plan must not hold on to an
 * entity that is no longer in the world.
 */
public sealed interface Goal {
    /** @return where it is now; nothing for a creature that is gone */
    Optional<Vec3> where(LiveHandle live);

    /**
     * Sets off for it, keeping up with it as it moves.
     *
     * @return whether it was there to set off for
     */
    boolean walkTo(LiveHandle live, float speed, int closeEnough);

    /** @return whether it was there to look at */
    boolean lookAt(LiveHandle live);

    /**
     * @param pos the block
     */
    record Place(BlockPos pos) implements Goal {
        @Override
        public Optional<Vec3> where(LiveHandle live) {
            return Optional.of(Vec3.atCenterOf(pos));
        }

        @Override
        public boolean walkTo(LiveHandle live, float speed, int closeEnough) {
            live.walkTo(pos, speed, closeEnough);
            return true;
        }

        @Override
        public boolean lookAt(LiveHandle live) {
            live.lookAt(pos);
            return true;
        }
    }

    /**
     * A view: nothing to walk to, a heading to look along from where the pet stands.
     *
     * @param facing which way, level
     * @param distance how far along it the pet looks
     */
    record Horizon(Vec3 facing, double distance) implements Goal {
        @Override
        public Optional<Vec3> where(LiveHandle live) {
            return Optional.of(live.pet().getEyePosition().add(facing.scale(distance)));
        }

        /** Already as near as it gets: there is nowhere to walk to. */
        @Override
        public boolean walkTo(LiveHandle live, float speed, int closeEnough) {
            return true;
        }

        @Override
        public boolean lookAt(LiveHandle live) {
            live.lookAt(live.pet().getEyePosition().add(facing.scale(distance)));
            return true;
        }
    }

    /**
     * The pet's owner, wherever they are: looked for each time like a creature, and gone while they are not in the
     * pet's world.
     */
    record Owner() implements Goal {
        @Override
        public Optional<Vec3> where(LiveHandle live) {
            return live.owner().map(Entity::position);
        }

        @Override
        public boolean walkTo(LiveHandle live, float speed, int closeEnough) {
            return live.owner().map(owner -> {
                live.walkTo(owner, speed, closeEnough);
                return true;
            }).orElse(false);
        }

        @Override
        public boolean lookAt(LiveHandle live) {
            return live.owner().map(owner -> {
                live.lookAt(owner);
                return true;
            }).orElse(false);
        }
    }

    /**
     * @param id the creature's, or item's, entity id
     */
    record Creature(UUID id) implements Goal {
        @Override
        public Optional<Vec3> where(LiveHandle live) {
            return live.creature(id).map(Entity::position);
        }

        @Override
        public boolean walkTo(LiveHandle live, float speed, int closeEnough) {
            return live.creature(id).map(creature -> {
                live.walkTo(creature, speed, closeEnough);
                return true;
            }).orElse(false);
        }

        @Override
        public boolean lookAt(LiveHandle live) {
            return live.creature(id).map(creature -> {
                live.lookAt(creature);
                return true;
            }).orElse(false);
        }
    }
}
