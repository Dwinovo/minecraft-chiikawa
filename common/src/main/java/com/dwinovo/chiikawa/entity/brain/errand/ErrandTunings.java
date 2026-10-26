package com.dwinovo.chiikawa.entity.brain.errand;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

/**
 * The shapes of an errand's own numbers, the {@code tuning} block of its file in {@code pet_errand}:
 * how fast it walks, how near it gets, how long it waits at each step. Errands that go about it
 * alike share a shape. Distances are in blocks and times in ticks; a distance the game compares
 * squared is named {@code _sqr} and given squared.
 */
public final class ErrandTunings {
    private ErrandTunings() {
    }

    /** The kinds of number the shapes below are made of, apart from them so a shape can be made first. */
    private static final class Fields {
        private static final Codec<Float> SPEED = Codec.floatRange(0.0F, 4.0F);
        private static final Codec<Double> DISTANCE = Codec.doubleRange(0.0, 256.0);
        private static final Codec<Integer> BLOCKS = ExtraCodecs.NON_NEGATIVE_INT;
        private static final Codec<Integer> TICKS = ExtraCodecs.POSITIVE_INT;
        /** A time that may be none at all: a pet that is not to wait. */
        private static final Codec<Integer> PATIENCE = ExtraCodecs.NON_NEGATIVE_INT;
    }

    /** An errand with no numbers of its own: what it does is the same for every pet, or all in other files. */
    public record Untuned() {
        public static final Untuned NONE = new Untuned();
        public static final Codec<Untuned> CODEC = RecordCodecBuilder.create(instance -> instance.point(NONE));
    }

    /**
     * @param speed how fast it walks to the owner
     * @param maxScore what it is worth at the owner's far end of the leash: the errand's own score is
     *                 what it is worth at the near end
     */
    public record FollowOwner(float speed, float maxScore) {
        public static final Codec<FollowOwner> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Fields.SPEED.fieldOf("speed").forGetter(FollowOwner::speed),
            Codec.floatRange(0.0F, 1.0F).fieldOf("max_score").forGetter(FollowOwner::maxScore)
        ).apply(instance, FollowOwner::new));
    }

    /**
     * @param strollSpeed how fast it strolls
     * @param strollRadius how far from where it stands it strolls to, each way
     * @param strollAttempts how many spots it tries for one it can stand on
     * @param strollTicks the longest one stroll lasts
     * @param returnSpeed how fast it walks back to the area it keeps to
     */
    public record Wander(float strollSpeed, int strollRadius, int strollAttempts, int strollTicks, float returnSpeed) {
        public static final Codec<Wander> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Fields.SPEED.fieldOf("stroll_speed").forGetter(Wander::strollSpeed),
            Fields.BLOCKS.fieldOf("stroll_radius").forGetter(Wander::strollRadius),
            Fields.TICKS.fieldOf("stroll_attempts").forGetter(Wander::strollAttempts),
            Fields.TICKS.fieldOf("stroll_ticks").forGetter(Wander::strollTicks),
            Fields.SPEED.fieldOf("return_speed").forGetter(Wander::returnSpeed)
        ).apply(instance, Wander::new));
    }

    /**
     * @param speed how fast it walks to the item
     * @param pickupDistance how near it has to be to pick it up
     * @param maxWalkTicks the longest it walks to an item before it decides the item is not worth the walk
     * @param noWayTicks how long the mover may have no way to the item before the pet gives up on one: none gives
     *                   up the first time it says so
     */
    public record PickUp(float speed, double pickupDistance, int maxWalkTicks, int noWayTicks) {
        public static final Codec<PickUp> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Fields.SPEED.fieldOf("speed").forGetter(PickUp::speed),
            Fields.DISTANCE.fieldOf("pickup_distance").forGetter(PickUp::pickupDistance),
            Fields.TICKS.fieldOf("max_walk_ticks").forGetter(PickUp::maxWalkTicks),
            Fields.PATIENCE.fieldOf("no_way_ticks").forGetter(PickUp::noWayTicks)
        ).apply(instance, PickUp::new));
    }

    /**
     * @param speed how fast it walks to the block
     * @param reachSqr how near to the block's middle it has to be to break it, squared
     * @param noWayTicks how long the mover may have no way to the block before the pet gives up on one: none gives
     *                   up the first time it says so
     * @param toolWear how much the tool in the pet's hand wears each time it breaks the block: none for a plant pulled
     *                 by hand
     */
    public record HarvestBlock(float speed, double reachSqr, int noWayTicks, int toolWear) {
        public static final Codec<HarvestBlock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Fields.SPEED.fieldOf("speed").forGetter(HarvestBlock::speed),
            Codec.doubleRange(0.0, 65536.0).fieldOf("reach_sqr").forGetter(HarvestBlock::reachSqr),
            Fields.PATIENCE.fieldOf("no_way_ticks").forGetter(HarvestBlock::noWayTicks),
            Fields.BLOCKS.fieldOf("tool_wear").forGetter(HarvestBlock::toolWear)
        ).apply(instance, HarvestBlock::new));
    }

    /**
     * @param speed how fast it walks to the soil
     * @param reachSqr how near to the soil's middle it has to be to plant in it, squared
     * @param noWayTicks how long the mover may have no way to the soil before the pet gives up on one: none gives
     *                   up the first time it says so
     */
    public record PlantBlock(float speed, double reachSqr, int noWayTicks) {
        public static final Codec<PlantBlock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Fields.SPEED.fieldOf("speed").forGetter(PlantBlock::speed),
            Codec.doubleRange(0.0, 65536.0).fieldOf("reach_sqr").forGetter(PlantBlock::reachSqr),
            Fields.PATIENCE.fieldOf("no_way_ticks").forGetter(PlantBlock::noWayTicks)
        ).apply(instance, PlantBlock::new));
    }

    /**
     * @param speed how fast it walks to the container
     * @param arriveDistance how near the walk gets, counted in blocks along each axis
     * @param reach how near it has to be to the container to put things in it
     * @param noWayTicks how long the mover may have no way to the container before the pet gives up on one: none gives
     *                   up the first time it says so
     */
    public record Deliver(float speed, int arriveDistance, double reach, int noWayTicks) {
        public static final Codec<Deliver> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Fields.SPEED.fieldOf("speed").forGetter(Deliver::speed),
            Fields.BLOCKS.fieldOf("arrive_distance").forGetter(Deliver::arriveDistance),
            Fields.DISTANCE.fieldOf("reach").forGetter(Deliver::reach),
            Fields.PATIENCE.fieldOf("no_way_ticks").forGetter(Deliver::noWayTicks)
        ).apply(instance, Deliver::new));
    }

    /**
     * Where a fighter stands, melee or ranged.
     *
     * @param closeInSpeed how fast it walks in on a foe that is too far
     * @param backOffSpeed how fast it walks out from one that is too near, in more of a hurry than in
     * @param backOffStep how far away it aims when backing off
     * @param saferBy how much farther from the foe than the pet the owner has to be before running to
     *                them counts as getting away
     * @param ownerArriveDistance how near to the owner it runs
     */
    public record Footwork(float closeInSpeed, float backOffSpeed, double backOffStep, double saferBy,
                           int ownerArriveDistance) {
        public static final Codec<Footwork> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Fields.SPEED.fieldOf("close_in_speed").forGetter(Footwork::closeInSpeed),
            Fields.SPEED.fieldOf("back_off_speed").forGetter(Footwork::backOffSpeed),
            Fields.DISTANCE.fieldOf("back_off_step").forGetter(Footwork::backOffStep),
            Fields.DISTANCE.fieldOf("safer_by").forGetter(Footwork::saferBy),
            Fields.BLOCKS.fieldOf("owner_arrive_distance").forGetter(Footwork::ownerArriveDistance)
        ).apply(instance, Footwork::new));
    }

    /**
     * @param footwork where it stands
     * @param range how far away it still shoots
     * @param drawTicks how long it draws the bow before the tick it looses in: the bow is loosed on the tick after the
     *                  draw, so a draw of 15 is an arrow 16 ticks after the bow is raised
     * @param cooldownTicks how long it waits after a shot
     */
    public record Ranged(Footwork footwork, double range, int drawTicks, int cooldownTicks) {
        public static final Codec<Ranged> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Footwork.CODEC.fieldOf("footwork").forGetter(Ranged::footwork),
            Fields.DISTANCE.fieldOf("range").forGetter(Ranged::range),
            Fields.TICKS.fieldOf("draw_ticks").forGetter(Ranged::drawTicks),
            Fields.TICKS.fieldOf("cooldown_ticks").forGetter(Ranged::cooldownTicks)
        ).apply(instance, Ranged::new));
    }

    /**
     * How a pet fights when it takes up a weapon from its bag to answer an attack, with either.
     *
     * @param sword where it stands with a sword
     * @param bow how it shoots with a bow
     */
    public record Defend(Footwork sword, Ranged bow) {
        public static final Codec<Defend> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Footwork.CODEC.fieldOf("sword").forGetter(Defend::sword),
            Ranged.CODEC.fieldOf("bow").forGetter(Defend::bow)
        ).apply(instance, Defend::new));
    }

    /**
     * What a pet does with its fear: the animations it tries, in order of preference, the first it has being shown.
     * Each character has its own, as the series draws it, and the rest the default.
     *
     * @param byCharacter the animations of the characters that have their own, by the id of their entity
     * @param otherwise the animations of the rest
     */
    public record Cowers(Map<ResourceLocation, List<String>> byCharacter, List<String> otherwise) {
        public static final Codec<Cowers> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(ResourceLocation.CODEC, ExtraCodecs.nonEmptyList(Codec.STRING.listOf()))
                .fieldOf("by_character").forGetter(Cowers::byCharacter),
            ExtraCodecs.nonEmptyList(Codec.STRING.listOf()).fieldOf("otherwise").forGetter(Cowers::otherwise)
        ).apply(instance, Cowers::new));

        /** @return the animations the character cowers with, the one it prefers first */
        public List<String> of(ResourceLocation character) {
            return byCharacter.getOrDefault(character, otherwise);
        }
    }

    /**
     * @param speed how fast it runs
     * @param ownerRange how far off its owner may be for it to run behind them
     * @param behindOwner how far past its owner, seen from what it runs from, it runs to
     * @param awayStep how far from what it runs from it runs when it has no owner nor home to run to
     * @param arriveDistance how near the place it runs to, or its seat, it has to be for that walk to be done
     * @param safeTicks how long nothing may have hurt it before it comes out of hiding
     * @param safeDistance how far off what it ran from has to be before it does
     * @param cowers how it cowers until then
     */
    public record Flee(float speed, double ownerRange, double behindOwner, double awayStep, double arriveDistance,
                       int safeTicks, double safeDistance, Cowers cowers) {
        public static final Codec<Flee> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Fields.SPEED.fieldOf("speed").forGetter(Flee::speed),
            Fields.DISTANCE.fieldOf("owner_range").forGetter(Flee::ownerRange),
            Fields.DISTANCE.fieldOf("behind_owner").forGetter(Flee::behindOwner),
            Fields.DISTANCE.fieldOf("away_step").forGetter(Flee::awayStep),
            Fields.DISTANCE.fieldOf("arrive_distance").forGetter(Flee::arriveDistance),
            Fields.TICKS.fieldOf("safe_ticks").forGetter(Flee::safeTicks),
            Fields.DISTANCE.fieldOf("safe_distance").forGetter(Flee::safeDistance),
            Cowers.CODEC.fieldOf("cowers").forGetter(Flee::cowers)
        ).apply(instance, Flee::new));
    }

    /** @param noteParticleIntervalTicks how often a note rises from the pet while it plays */
    public record PlayMusic(int noteParticleIntervalTicks) {
        public static final Codec<PlayMusic> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Fields.TICKS.fieldOf("note_particle_interval_ticks").forGetter(PlayMusic::noteParticleIntervalTicks)
        ).apply(instance, PlayMusic::new));
    }

    /**
     * @param speed how fast it walks to the board
     * @param arriveDistance how near the walk gets, counted in blocks along each axis; the board is solid
     * @param takeDistance how near it has to be to take a slip
     */
    public record TakeTask(float speed, int arriveDistance, double takeDistance) {
        public static final Codec<TakeTask> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Fields.SPEED.fieldOf("speed").forGetter(TakeTask::speed),
            Fields.BLOCKS.fieldOf("arrive_distance").forGetter(TakeTask::arriveDistance),
            Fields.DISTANCE.fieldOf("take_distance").forGetter(TakeTask::takeDistance)
        ).apply(instance, TakeTask::new));
    }

    /**
     * @param speed how fast it walks to the shop
     * @param arriveDistance how near the walk gets; nearer than the buying distance, so arriving is enough
     * @param buyDistance how near it has to be to the counter to buy
     */
    public record Shop(float speed, int arriveDistance, double buyDistance) {
        public static final Codec<Shop> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Fields.SPEED.fieldOf("speed").forGetter(Shop::speed),
            Fields.BLOCKS.fieldOf("arrive_distance").forGetter(Shop::arriveDistance),
            Fields.DISTANCE.fieldOf("buy_distance").forGetter(Shop::buyDistance)
        ).apply(instance, Shop::new));
    }

    /**
     * @param speed how fast it walks to the owner
     * @param arriveDistance how near the walk aims: inside reach, not at the edge of it
     * @param reach how near it has to be to hold the present out
     * @param morningFrom the time of day after which an owner has woken in the morning, as the level's clock reads it: a
     *                    cat leaves its morning gift only then
     * @param morningTo the time of day before which they have
     * @param sleptTicks how long the owner has to have slept to have woken, as the game counts it
     */
    public record GiftOwner(float speed, int arriveDistance, double reach, float morningFrom, float morningTo, int sleptTicks) {
        public static final Codec<GiftOwner> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Fields.SPEED.fieldOf("speed").forGetter(GiftOwner::speed),
            Fields.BLOCKS.fieldOf("arrive_distance").forGetter(GiftOwner::arriveDistance),
            Fields.DISTANCE.fieldOf("reach").forGetter(GiftOwner::reach),
            Codec.floatRange(0.0F, 1.0F).fieldOf("morning_from").forGetter(GiftOwner::morningFrom),
            Codec.floatRange(0.0F, 1.0F).fieldOf("morning_to").forGetter(GiftOwner::morningTo),
            Fields.BLOCKS.fieldOf("slept_ticks").forGetter(GiftOwner::sleptTicks)
        ).apply(instance, GiftOwner::new));
    }

    /**
     * @param speed how fast it walks to the partner
     * @param arriveMargin how much further than the scene's approach distance the two may stand and
     *                     still begin: a partner shifting its feet should not undo the arrival
     * @param replyTicks how long it waits for the partner to say yes or no to being asked, before it gives up on a
     *                   partner that is not listening
     */
    public record Socialize(float speed, int arriveMargin, int replyTicks) {
        public static final Codec<Socialize> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Fields.SPEED.fieldOf("speed").forGetter(Socialize::speed),
            Fields.BLOCKS.fieldOf("arrive_margin").forGetter(Socialize::arriveMargin),
            Fields.TICKS.fieldOf("reply_ticks").forGetter(Socialize::replyTicks)
        ).apply(instance, Socialize::new));
    }

    /**
     * @param speed how fast it walks to the chair
     * @param arriveDistance how near the chair it has to be to sit down on it
     * @param handInTicks how long it stays in its chair handing the paper in: the {@code hand_in} animation's second
     */
    public record TakeExam(float speed, double arriveDistance, int handInTicks) {
        public static final Codec<TakeExam> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Fields.SPEED.fieldOf("speed").forGetter(TakeExam::speed),
            Fields.DISTANCE.fieldOf("arrive_distance").forGetter(TakeExam::arriveDistance),
            Fields.TICKS.fieldOf("hand_in_ticks").forGetter(TakeExam::handInTicks)
        ).apply(instance, TakeExam::new));
    }

    /**
     * @param speed how fast it walks to the chair
     * @param readingDistance how near the chair it has to be to read what is on the desk
     * @param readingTicks how long it stands reading the results before it takes them in
     */
    public record CheckResults(float speed, double readingDistance, int readingTicks) {
        public static final Codec<CheckResults> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Fields.SPEED.fieldOf("speed").forGetter(CheckResults::speed),
            Fields.DISTANCE.fieldOf("reading_distance").forGetter(CheckResults::readingDistance),
            Fields.TICKS.fieldOf("reading_ticks").forGetter(CheckResults::readingTicks)
        ).apply(instance, CheckResults::new));
    }

    /**
     * @param speed how fast it walks to the spot
     * @param closeEnough how near the spot it comes, by blocks: near enough to look, not so near as to crowd; the brain's
     *                    mover stops the pet when it is this many blocks away from the spot's block
     * @param arriveDistance how near the spot, as the crow flies, the pet counts as there: a little over the blocks it
     *                       comes to, since the middle of the block it stops in can be up to half a block further
     * @param lookRange how far from the spot the blower may have wandered and still be the one looked at
     */
    public record AnswerWhistle(float speed, int closeEnough, double arriveDistance, double lookRange) {
        public static final Codec<AnswerWhistle> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Fields.SPEED.fieldOf("speed").forGetter(AnswerWhistle::speed),
            Fields.BLOCKS.fieldOf("close_enough").forGetter(AnswerWhistle::closeEnough),
            Fields.DISTANCE.fieldOf("arrive_distance").forGetter(AnswerWhistle::arriveDistance),
            Fields.DISTANCE.fieldOf("look_range").forGetter(AnswerWhistle::lookRange)
        ).apply(instance, AnswerWhistle::new));
    }

    /**
     * @param speed how fast it walks to who is being photographed
     * @param standBack how near the walk gets: a few blocks, as a photographer stands
     * @param holdTicks how long the pose is held after the shutter, so the camera is not put away before the picture is drawn
     * @param lookBackTicks how long whoever is photographed keeps looking at the camera
     * @param viewDistance how far a view is looked at, for the pet to turn its head towards
     * @param maxWalkTicks the longest it walks to somebody before it decides they are not worth the walk
     * @param noWayTicks how long the mover may have no way to who it photographs before the pet gives up on one: none gives
     *                   up the first time it says so
     */
    public record TakePhoto(float speed, int standBack, int holdTicks, int lookBackTicks, double viewDistance,
                            int maxWalkTicks, int noWayTicks) {
        public static final Codec<TakePhoto> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Fields.SPEED.fieldOf("speed").forGetter(TakePhoto::speed),
            Fields.BLOCKS.fieldOf("stand_back").forGetter(TakePhoto::standBack),
            Fields.TICKS.fieldOf("hold_ticks").forGetter(TakePhoto::holdTicks),
            Fields.TICKS.fieldOf("look_back_ticks").forGetter(TakePhoto::lookBackTicks),
            Fields.DISTANCE.fieldOf("view_distance").forGetter(TakePhoto::viewDistance),
            Fields.TICKS.fieldOf("max_walk_ticks").forGetter(TakePhoto::maxWalkTicks),
            Fields.PATIENCE.fieldOf("no_way_ticks").forGetter(TakePhoto::noWayTicks)
        ).apply(instance, TakePhoto::new));
    }
}
