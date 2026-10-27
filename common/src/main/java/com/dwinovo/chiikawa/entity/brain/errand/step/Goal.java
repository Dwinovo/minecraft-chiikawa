package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.utils.Deliveries;
import com.dwinovo.chiikawa.whistle.WhistleHeard;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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
     * @param where where it is now
     * @param arriveDistanceSqr how near, squared, a walk to it counts as being there
     * @return whether the pet has got there: near enough, unless the goal says it is somewhere to be in and not near
     */
    default boolean reached(LiveHandle live, Vec3 where, double arriveDistanceSqr) {
        return live.pet().distanceToSqr(where) <= arriveDistanceSqr;
    }

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
     * A block the pet is to stand in, such as the place out of the rain that a roof makes: reached once the pet is in the
     * block and not before, however near it is. Near is not enough there: a pet that stops half a block short of the roof
     * stands in the rain.
     *
     * @param pos the block
     */
    record Spot(BlockPos pos) implements Goal {
        @Override
        public Optional<Vec3> where(LiveHandle live) {
            return Optional.of(Vec3.atCenterOf(pos));
        }

        @Override
        public boolean reached(LiveHandle live, Vec3 where, double arriveDistanceSqr) {
            return live.pet().blockPosition().equals(pos);
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
     * The container a pet delivers to, to the middle of the block, for as long as it is worth the walk: one the pets deliver
     * to that would still take something from the pet's bag. Looked at each time, since a container can be broken or filled
     * while its pet is on the way, or the bag emptied, and then there is nowhere to go.
     *
     * @param pos the container's block
     * @param deliveries which containers and which things the delivery is about
     */
    record Container(BlockPos pos, Deliveries deliveries) implements Goal {
        private boolean worthTheWalk(LiveHandle live) {
            return live.pet().level() instanceof ServerLevel level
                && deliveries.worthVisiting(level, pos, live.pet().getBackpack());
        }

        @Override
        public Optional<Vec3> where(LiveHandle live) {
            return worthTheWalk(live) ? Optional.of(Vec3.atCenterOf(pos)) : Optional.empty();
        }

        @Override
        public boolean walkTo(LiveHandle live, float speed, int closeEnough) {
            if (!worthTheWalk(live)) {
                return false;
            }
            live.walkTo(pos, speed, closeEnough);
            return true;
        }

        @Override
        public boolean lookAt(LiveHandle live) {
            if (!worthTheWalk(live)) {
                return false;
            }
            live.lookAt(pos);
            return true;
        }
    }

    /**
     * The chair of an exam desk, to the middle of the floor it stands on, which is where a pet sits down from. The desk
     * is looked for each time and the chair is gone with it, since a desk can be broken while its pet is on the way.
     *
     * @param desk the desk
     */
    record Chair(GlobalPos desk) implements Goal {
        @Override
        public Optional<Vec3> where(LiveHandle live) {
            return live.examDesk(desk).map(found -> Vec3.atBottomCenterOf(found.chair()));
        }

        @Override
        public boolean walkTo(LiveHandle live, float speed, int closeEnough) {
            return live.examDesk(desk).map(found -> {
                live.walkTo(found.chair(), speed, closeEnough);
                return true;
            }).orElse(false);
        }

        @Override
        public boolean lookAt(LiveHandle live) {
            return live.examDesk(desk).map(found -> {
                live.lookAt(found.chair());
                return true;
            }).orElse(false);
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
     * Where a whistle was blown that the pet has set off for, and whoever is standing there: the walk is to the spot, and
     * the gaze is on the player nearest to it if one is near enough and on the spot if none is. Looked for in the pet's
     * memory each time, since it lasts only as long as the pet's curiosity does, and is gone with it.
     *
     * @param lookRange how far from the spot a player may have wandered and still be the one looked at
     */
    record Whistler(double lookRange) implements Goal {
        @Override
        public Optional<Vec3> where(LiveHandle live) {
            return WhistleHeard.by(live.pet()).map(spot -> Vec3.atCenterOf(spot.pos()));
        }

        @Override
        public boolean walkTo(LiveHandle live, float speed, int closeEnough) {
            return WhistleHeard.by(live.pet()).map(spot -> {
                live.walkTo(spot.pos(), speed, closeEnough);
                return true;
            }).orElse(false);
        }

        @Override
        public boolean lookAt(LiveHandle live) {
            return where(live).map(centre -> {
                Player blower = live.pet().level().getNearestPlayer(centre.x, centre.y, centre.z, lookRange, false);
                if (blower != null) {
                    live.lookAt(blower);
                } else {
                    live.lookAt(centre.add(0.0, 1.0, 0.0));
                }
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
