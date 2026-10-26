package com.dwinovo.chiikawa.network;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.PetRules;
import com.dwinovo.chiikawa.entity.brain.PetMind;
import com.dwinovo.chiikawa.entity.brain.errand.LackKey;
import com.dwinovo.chiikawa.entity.brain.errand.decide.DecisionTrace;
import com.dwinovo.chiikawa.entity.brain.errand.decide.WhyNot;
import com.dwinovo.chiikawa.entity.brain.errand.run.Running;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** What an owner asks of a pet from its screen, and what the players who can see a pet are told it does. */
public final class PetPayloads {
    private PetPayloads() {
    }

    /**
     * An order picked on the pet screen: follow, sit, or get on with things.
     *
     * @param pet the pet's entity id; the server checks it is the sender's and within reach
     * @param directive the order
     */
    public record PetDirectivePayload(int pet, PetDirective directive) implements CustomPacketPayload {
        public static final Type<PetDirectivePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "pet_directive"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PetDirectivePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeVarInt(value.pet);
                buffer.writeByte(value.directive.ordinal());
            },
            buffer -> new PetDirectivePayload(buffer.readVarInt(), PetDirective.fromId(buffer.readByte()))
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * What an owner has picked on the pet screen that the pet may and may not do.
     *
     * @param pet the pet's entity id; the server checks it is the sender's and within reach
     * @param rules all of them, as the screen now shows them
     */
    public record PetRulesPayload(int pet, PetRules rules) implements CustomPacketPayload {
        public static final Type<PetRulesPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "pet_rules"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PetRulesPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PetRulesPayload::pet,
            PetRules.STREAM_CODEC, PetRulesPayload::rules,
            PetRulesPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * A pet made a move once, one named by data rather than by code — a clap as it listens
     * to music, say. A player's game plays it if the pet has an animation by that name, and
     * otherwise shows nothing.
     *
     * @param pet the pet's entity id
     * @param animation the move's animation name
     */
    public record PetGesturePayload(int pet, String animation) implements CustomPacketPayload {
        public static final Type<PetGesturePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "pet_gesture"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PetGesturePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PetGesturePayload::pet,
            ByteBufCodecs.STRING_UTF8, PetGesturePayload::animation,
            PetGesturePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * What a pet would do next and why it is not at work, for the screen of the owner who has its backpack open: sent now and
     * then while it is open, from what the pet's mind would decide and what it found when it last did. Each player's game
     * puts the keys into their language.
     *
     * @param pet the pet's entity id
     * @param next the errand it would start if it were on none, other than the one it is on
     * @param whyNotKey the translation key of why it is not at work, when it is not
     * @param lackKey the translation key of what it is short of, when that is why
     */
    public record PetOutlookPayload(int pet, Optional<ResourceLocation> next, Optional<String> whyNotKey,
                                    Optional<String> lackKey) implements CustomPacketPayload {
        public static final Type<PetOutlookPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "pet_outlook"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PetOutlookPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PetOutlookPayload::pet,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), PetOutlookPayload::next,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), PetOutlookPayload::whyNotKey,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), PetOutlookPayload::lackKey,
            PetOutlookPayload::new);

        /**
         * @param pet the pet's entity id
         * @param look how each errand would fare for the pet now, see {@code PetMind#look}, and the errand it is on
         * @param whyNot why the pet is not at work, if it is not
         * @return what the pet's screen is told
         */
        public static PetOutlookPayload of(int pet, PetMind.Look look, Optional<WhyNot> whyNot) {
            Optional<ResourceLocation> running = look.running().map(Running::errand);
            Optional<ResourceLocation> next = look.trace().lines().stream()
                .filter(line -> line.verdict().equals(DecisionTrace.STARTABLE) && !Optional.of(line.errand()).equals(running))
                .max(Comparator.comparingInt(DecisionTrace.Line::priority)
                    .thenComparing(line -> line.score().orElse(0.0F)))
                .map(DecisionTrace.Line::errand);
            return new PetOutlookPayload(pet, next, whyNot.map(WhyNot::reasonKey),
                whyNot.flatMap(WhyNot::lack).map(LackKey::translationKey));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
