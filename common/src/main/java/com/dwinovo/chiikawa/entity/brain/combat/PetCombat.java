package com.dwinovo.chiikawa.entity.brain.combat;

import java.util.List;
import java.util.OptionalInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;

/**
 * The rules of a fight, kept free of live entities so they can be read and tested on
 * their own. Borrowed in shape from Numen's combat core: <b>one decision at the top, two
 * legs underneath</b>.
 *
 * <h2>Where a pet stands is a band, not two states</h2>
 * A fight has an inner edge (any closer and it is standing somewhere it should not) and
 * an outer edge (any farther and it cannot land anything). Inside the band the pet holds
 * still and swings. Written as "too close → back off" and "too far → close in", the two
 * triggers end up on the same line and the pet shuffles back and forth on it; a band has
 * slack built in and needs no hysteresis.
 *
 * <h2>Moving and swinging are separate legs</h2>
 * The band decides where to stand; the swing decides whether this tick lands a hit. A pet
 * backing away from something still hits what is in front of it, which is the difference
 * between a pet that retreats and one that gets hit in the back all the way home.
 *
 * <h2>Distances come from the game where they can</h2>
 * Melee reach is vanilla's own hitbox test, and a lit creeper's danger is vanilla's blast
 * radius doubled — the distance at which an explosion stops doing damage.
 */
public final class PetCombat {
    /** Below this share of its health a pet breaks off. */
    public static final float BREAK_OFF_HEALTH = 0.35F;
    /**
     * And it will not go back in until this much has come back. Two lines rather than one
     * so a pet healing under fire does not step in and out of the fight every few ticks.
     */
    public static final float RALLY_HEALTH = 0.6F;
    /** How far a pet that has broken off tries to get from what it was fighting. */
    public static final double BREAK_OFF_DISTANCE = 16.0;
    /** A bow wants room: closer than this and the pet backs up while it draws. */
    public static final double BOW_NEAR = 5.0;
    /** Past this a shot is not worth taking, so the pet closes in instead. */
    public static final double BOW_FAR = 15.0;
    /**
     * How far a creeper's blast still hurts: vanilla's explosion radius of 3, and damage
     * carries to twice the radius. A lit creeper is the one thing a pet backs away from
     * whatever else it was doing.
     */
    public static final double BLAST_SPAN = 6.0;
    /**
     * How close a creeper lets something get before it starts swelling, plus a step.
     *
     * <p>A pet's reach is about a block and a half — far inside that — so unlike a player
     * there is no distance from which a pet can hit a creeper without lighting it. That is
     * the whole reason a creeper is not something a pet with a sword fights: it is
     * something it stays away from until somebody with a bow turns up.
     */
    public static final double FUSE_RADIUS = 4.0;
    /**
     * The quickest a pet swings, however light the weapon. Bare hands are quick and weak
     * in vanilla, and a pet is not meant to be a blender either way.
     */
    public static final int FASTEST_SWING = 10;
    /** And the slowest, which is what a pet with nothing to say about it swings at. */
    public static final int SLOWEST_SWING = 20;

    /**
     * How far a mob's swing carries past its own box, vanilla's {@code DEFAULT_ATTACK_REACH}.
     * Kept here rather than guessed at, so a wide target is walked up to from a distance
     * that actually reaches it.
     */
    private static final double MOB_REACH = Math.sqrt(2.04) - 0.6;

    /** Where a pet wants to be, measured from what it is fighting. */
    public record Band(double near, double far) {
        public boolean tooClose(double distance) {
            return distance < near;
        }

        public boolean tooFar(double distance) {
            return distance > far;
        }
    }

    /**
     * One thing on the field, as the rules need it.
     *
     * @param explosive it goes off when it gets close enough — a creeper, fuse or no fuse
     * @param armed the fuse is already going
     */
    public record Foe(int id, double distance, boolean explosive, boolean armed) {
    }

    private PetCombat() {
    }

    /**
     * Where to stand.
     *
     * @param brokenOff the pet has broken off and is getting away
     * @param shooting the pet is fighting with a bow
     * @param foe what the pet is fighting: a creeper with its fuse lit is about to go off
     * @param meleeReach how far this pet can reach this target, vanilla's own number
     */
    public static Band band(boolean brokenOff, boolean shooting, Foe foe, double meleeReach) {
        if (brokenOff) {
            return new Band(BREAK_OFF_DISTANCE, Double.POSITIVE_INFINITY);
        }
        double near = foe.armed() ? BLAST_SPAN : foe.explosive() ? FUSE_RADIUS : 0.0;
        if (shooting) {
            return new Band(Math.max(near, BOW_NEAR), BOW_FAR);
        }
        // With a sword there is nothing to be done about a creeper but keep out of its way,
        // so the band has no far edge to walk in to.
        return near > 0.0 ? new Band(near, Double.POSITIVE_INFINITY) : new Band(0.0, meleeReach);
    }

    /**
     * The far edge of a sword fight: centre to centre, the distance at which a swing
     * reaches. Both boxes count — a spider is more than twice as wide as a zombie, and a
     * pet that walks to a zombie's distance from a spider never lands anything.
     */
    public static double meleeFar(double petWidth, double foeWidth) {
        return petWidth / 2.0 + MOB_REACH + foeWidth / 2.0;
    }

    /**
     * Whether the pet is breaking off this tick.
     *
     * @param healthFraction how much of its health is left, 0 to 1
     * @param wasBrokenOff whether it was already backing away
     */
    public static boolean breakingOff(float healthFraction, boolean wasBrokenOff) {
        return wasBrokenOff ? healthFraction < RALLY_HEALTH : healthFraction <= BREAK_OFF_HEALTH;
    }

    /**
     * How long between swings. Vanilla gives a weapon an attack speed and the pet an
     * attack speed attribute to hang it on, so a sword is quicker in a pet's hands than a
     * bare one — clamped at both ends, because a pet is not a player with a cooldown bar.
     *
     * @param attackSpeed the pet's {@code ATTACK_SPEED} attribute, swings per second
     */
    public static int swingCooldown(double attackSpeed) {
        if (attackSpeed <= 0.0) {
            return SLOWEST_SWING;
        }
        return (int) Math.clamp(Math.round(20.0 / attackSpeed), FASTEST_SWING, SLOWEST_SWING);
    }

    /**
     * Whether this is worth walking towards. A creeper is not, whatever its fuse is doing,
     * unless the pet has a bow — which answers it from outside the blast.
     *
     * <p>It still becomes the target when it is the only thing about: keeping away from
     * something is also a thing the pet has to be told to do, and the target is what tells
     * it. What changes is only the band it keeps to.
     */
    public static boolean fightable(Foe foe, boolean hasRanged) {
        return !foe.explosive() || hasRanged;
    }

    /**
     * Which one to fight. The nearest, <b>but the one already being fought stays chosen</b>
     * while it is still worth fighting: picking the nearest afresh every scan means a pet
     * in a crowd turns towards a new one every couple of seconds and never finishes any of
     * them.
     *
     * @param kept the id of the target the pet already has, or {@link #NO_FOE}
     */
    public static OptionalInt pick(List<Foe> foes, int kept, boolean hasRanged) {
        Foe best = null;
        Foe fallback = null;
        for (Foe foe : foes) {
            if (!fightable(foe, hasRanged)) {
                fallback = nearer(fallback, foe);
                continue;
            }
            if (foe.id() == kept) {
                return OptionalInt.of(foe.id());
            }
            best = nearer(best, foe);
        }
        // Nothing worth fighting, but something worth backing away from: it is still the
        // target, because a pet with no target stands where it is and waits for the bang.
        Foe chosen = best != null ? best : fallback;
        return chosen == null ? OptionalInt.empty() : OptionalInt.of(chosen.id());
    }

    /** The nearer of the two, and the lower id when they are the same distance off. */
    private static Foe nearer(Foe best, Foe foe) {
        if (best == null || foe.distance() < best.distance()
                || (foe.distance() == best.distance() && foe.id() < best.id())) {
            return foe;
        }
        return best;
    }

    /**
     * Reads a live entity into the shape the rules take. The one place that knows what a
     * creeper looks like from the outside, so the sensor and the feet cannot disagree.
     */
    public static Foe of(Entity entity, double distance) {
        boolean armed = entity instanceof Creeper creeper
            && (creeper.getSwellDir() > 0 || creeper.isIgnited());
        return new Foe(entity.getId(), distance, explodes(entity), armed);
    }

    /**
     * Whether this thing goes off. A pet never swings at one: its reach is inside the
     * distance at which a creeper swells, so a hit is a lit fuse, and one hit is nowhere
     * near enough to put a creeper down. Keeping away is the whole answer.
     */
    public static boolean explodes(Entity entity) {
        return entity instanceof Creeper;
    }

    /** No target; the id no entity has. */
    public static final int NO_FOE = Integer.MIN_VALUE;
}
