package com.dwinovo.chiikawa.social;

import com.dwinovo.chiikawa.task.FinishedSlip;
import com.dwinovo.chiikawa.utils.ModCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

/**
 * A little scene two pets play together, the way the characters get on in the series,
 * loaded from {@code data/<namespace>/pet_interaction/<id>.json} by
 * {@link PetInteractionLoader}. One pet walks up to another, the two face each other and
 * each plays its part — a pose held throughout, and beats as they meet, now and then while
 * they are at it and as it is over (see {@link Beat}) — then they go their own ways.
 *
 * <p>Pets are named by entity type id or {@code #tag}. A pet plays the part of the first
 * entry that names it, so one file can have each character answer in its own way — the
 * same pestering makes one pet cry and leaves another unmoved.
 *
 * @param initiators who starts it, and how each plays its part
 * @param partners who it is played with, in order of preference: a pet goes for a partner
 *                 of the earliest entry it can find, the nearest one of those
 * @param partnerState what the partner has to be doing
 * @param partnerFinished a slip the partner must have just finished, if any
 * @param handsOver what the initiator takes out of its backpack and hands over, for the
 *                  partner to eat there and then; without one of these on it, it does not
 *                  start
 * @param partnerEagerTicks how long the partner is keener on work afterwards, as after a
 *                          proper meal; 0 for not at all
 * @param noticeDistance how near, in blocks, a partner has to be for a pet to think of it;
 *                       a pet only sees other pets within 16
 * @param approachDistance how close, in blocks, the initiator walks up before they begin
 * @param reservationTicks how long the initiator has to get there; the partner waits for it
 *                         that long and no longer
 * @param durationTicks how long the two play it
 * @param cooldownTicks how long the same two leave this scene alone afterwards
 * @param chance the chance, each time a pet looks around, that it thinks of doing this
 */
public record PetInteraction(
    List<Side> initiators,
    List<Side> partners,
    PartnerState partnerState,
    Optional<SlipCondition> partnerFinished,
    Optional<ExtraCodecs.TagOrElementLocation> handsOver,
    int partnerEagerTicks,
    double noticeDistance,
    int approachDistance,
    int reservationTicks,
    int durationTicks,
    int cooldownTicks,
    float chance
) {
    public static final Codec<PetInteraction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ExtraCodecs.nonEmptyList(Side.CODEC.listOf()).fieldOf("initiators").forGetter(PetInteraction::initiators),
        ExtraCodecs.nonEmptyList(Side.CODEC.listOf()).fieldOf("partners").forGetter(PetInteraction::partners),
        ModCodecs.strictOptionalField(PartnerState.CODEC, "partner_state", PartnerState.IDLE).forGetter(PetInteraction::partnerState),
        ModCodecs.strictOptionalField(SlipCondition.CODEC, "partner_finished").forGetter(PetInteraction::partnerFinished),
        ModCodecs.strictOptionalField(ExtraCodecs.TAG_OR_ELEMENT_ID, "hands_over").forGetter(PetInteraction::handsOver),
        ModCodecs.strictOptionalField(ExtraCodecs.NON_NEGATIVE_INT, "partner_eager_ticks", 0).forGetter(PetInteraction::partnerEagerTicks),
        Codec.doubleRange(0.0, Double.MAX_VALUE).fieldOf("notice_distance").forGetter(PetInteraction::noticeDistance),
        ExtraCodecs.NON_NEGATIVE_INT.fieldOf("approach_distance").forGetter(PetInteraction::approachDistance),
        ExtraCodecs.POSITIVE_INT.fieldOf("reservation_ticks").forGetter(PetInteraction::reservationTicks),
        ExtraCodecs.POSITIVE_INT.fieldOf("duration_ticks").forGetter(PetInteraction::durationTicks),
        ExtraCodecs.NON_NEGATIVE_INT.fieldOf("cooldown_ticks").forGetter(PetInteraction::cooldownTicks),
        Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(PetInteraction::chance)
    ).apply(instance, PetInteraction::new));

    public PetInteraction {
        initiators = List.copyOf(initiators);
        partners = List.copyOf(partners);
    }

    /**
     * @param type a pet's entity type
     * @return the part that kind of pet plays when it starts this, if it ever does
     */
    public Optional<Side> initiatorSide(Holder<EntityType<?>> type) {
        return initiators.stream().filter(side -> side.names(type)).findFirst();
    }

    /**
     * @param type a pet's entity type
     * @return where that kind of pet stands among the partners, lower being preferred;
     *         empty if this is never played with it
     */
    public OptionalInt partnerRank(Holder<EntityType<?>> type) {
        for (int i = 0; i < partners.size(); i++) {
            if (partners.get(i).names(type)) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }

    /** Whether this is what the initiator hands over, when it hands anything over. */
    public boolean handsOver(ItemStack stack) {
        return handsOver.filter(item -> names(item, stack.getItemHolder(), Registries.ITEM)).isPresent();
    }

    private static <T> boolean names(ExtraCodecs.TagOrElementLocation entry, Holder<T> holder,
            ResourceKey<? extends Registry<T>> registry) {
        return entry.tag() ? holder.is(TagKey.create(registry, entry.id())) : holder.is(entry.id());
    }

    /**
     * One pet's part: the pose it holds while the scene lasts, and its beats — one as the
     * two meet, one now and then while they are at it, and one as it is over.
     *
     * @param pets the pets that play it, by entity type id or {@code #tag}
     * @param pose what it holds for as long as the scene lasts; a pet without that
     *             animation simply stands there
     * @param begin what it does as the two meet
     * @param nowAndThen what it does over and over while the scene lasts
     * @param end what it does as the scene is over
     */
    public record Side(
        List<ExtraCodecs.TagOrElementLocation> pets,
        Optional<String> pose,
        Optional<Beat> begin,
        Optional<Beat.Recurring> nowAndThen,
        Optional<Beat> end
    ) {
        public static final Codec<Side> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.nonEmptyList(ExtraCodecs.TAG_OR_ELEMENT_ID.listOf()).fieldOf("pets").forGetter(Side::pets),
            ModCodecs.strictOptionalField(Codec.STRING, "pose").forGetter(Side::pose),
            ModCodecs.strictOptionalField(Beat.CODEC, "begin").forGetter(Side::begin),
            ModCodecs.strictOptionalField(Beat.Recurring.CODEC, "now_and_then").forGetter(Side::nowAndThen),
            ModCodecs.strictOptionalField(Beat.CODEC, "end").forGetter(Side::end)
        ).apply(instance, Side::new));

        public Side {
            pets = List.copyOf(pets);
        }

        /** Whether this part is for that kind of pet. */
        public boolean names(Holder<EntityType<?>> type) {
            return pets.stream().anyMatch(entry -> PetInteraction.names(entry, type, Registries.ENTITY_TYPE));
        }
    }

    /** What the partner has to be doing for a pet to go up to it. */
    public enum PartnerState implements StringRepresentable {
        /**
         * Pottering about. The partner is asked first: it stops where it is, turns to the
         * pet coming over and plays its part, and nobody else can have it in the meantime.
         */
        IDLE("idle"),
        /**
         * Playing music. The partner carries on and is not asked; the pet goes and makes up
         * its audience, and so can anyone else.
         */
        PLAYING_MUSIC("playing_music");

        public static final Codec<PartnerState> CODEC = StringRepresentable.fromEnum(PartnerState::values);

        private final String name;

        PartnerState(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    /**
     * @param slip the slip type the partner finished
     * @param withinTicks how long ago it may have finished it
     */
    public record SlipCondition(ResourceLocation slip, int withinTicks) {
        public static final Codec<SlipCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("slip").forGetter(SlipCondition::slip),
            ExtraCodecs.POSITIVE_INT.fieldOf("within_ticks").forGetter(SlipCondition::withinTicks)
        ).apply(instance, SlipCondition::new));

        /**
         * @param finished the last slip the partner finished, if it remembers one
         * @param gameTime the current game time
         */
        public boolean metBy(Optional<FinishedSlip> finished, long gameTime) {
            return finished
                .filter(last -> last.type().equals(slip) && gameTime - last.gameTime() <= withinTicks)
                .isPresent();
        }
    }
}
