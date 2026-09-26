package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitTag;
import com.dwinovo.chiikawa.social.Beat;
import com.dwinovo.chiikawa.social.PetInteraction;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;

/**
 * The generated scenes pets play together (0.1.1 design, section 5), each as the
 * characters get on in the series.
 *
 * <p>Every character answers in its own way, which is why a scene lists its partners one
 * by one rather than as "anyone": Chiikawa cries when something is unpleasant, Usagi never
 * does, Rakko keeps a straight face, Kurimanju says nothing. What each says is left to its
 * own {@code pet_voice} lines; a scene only names the moment.
 *
 * <p>Chances are per look around, every two seconds, so they are small: a scene is
 * something that happens now and then between two pets that happen to be idle together,
 * and a pair does not play the same one again for a good while after.
 */
public final class PetInteractionData {
    /** Momonga pesters someone, Chiikawa above all, clinging on and demanding praise. */
    public static final Identifier CLING = id("cling");
    /** Furuhonya and a friend greet each other the crab way, face to face. */
    public static final Identifier CRAB_GREETING = id("crab_greeting");
    /** Rakko treats Chiikawa or Hachiware to something to eat out of its own backpack. */
    public static final Identifier TREAT = id("treat");
    /** Kurimanju brings a coffee to someone who has just finished weeding. */
    public static final Identifier COFFEE = id("coffee");
    /** Anyone sits down by Hachiware busking, listens for a while and claps along. */
    public static final Identifier LISTEN_TO_MUSIC = id("listen_to_music");

    /** A listener claps every three to eight seconds: now and then, not in time. */
    private static final UniformInt CLAPS = UniformInt.of(60, 160);

    private PetInteractionData() {
    }

    /** @return scenes by id */
    public static Map<Identifier, PetInteraction> all() {
        return Map.of(
            // Momonga clings on and wants to be praised; the one clung to minds. Chiikawa,
            // whom it has bitten more than once, cries; the rest are put out each in its own
            // way, and Rakko and Kurimanju hardly let it show. Nobody praises it, so as it
            // lets go it bursts into its fake tears and wants comforting instead.
            CLING, new PetInteraction(
                List.of(part(InitEntity.MOMONGA_PET.get()).pose("cling")
                    .begin(beat(PetReaction.HAPPY, VoiceMoment.CLING))
                    .end(beat(PetReaction.HURT, VoiceMoment.TURNED_DOWN))
                    .build()),
                List.of(
                    part(InitEntity.CHIIKAWA_PET.get()).pose("clung_to")
                        .begin(beat(PetReaction.HURT, VoiceMoment.CLUNG_TO)).build(),
                    part(InitEntity.HACHIWARE_PET.get(), InitEntity.USAGI_PET.get(), InitEntity.SHISA_PET.get(),
                        InitEntity.FURUHONYA_PET.get()).pose("clung_to")
                        .begin(beat(PetReaction.CONFUSED, VoiceMoment.CLUNG_TO)).build(),
                    part(InitEntity.RAKKO_PET.get(), InitEntity.KURIMANJU_PET.get()).pose("clung_to")
                        .begin(beat(null, VoiceMoment.CLUNG_TO)).build()),
                PetInteraction.PartnerState.IDLE, Optional.empty(), Optional.empty(), 0,
                8.0, 1, 300, 80, 12000, 0.05F),
            // Furuhonya greets the friends who greet it back the crab way, Momonga first: the
            // crab headband was Momonga's present.
            CRAB_GREETING, new PetInteraction(
                List.of(part(InitEntity.FURUHONYA_PET.get()).pose("crab_greeting")
                    .begin(beat(PetReaction.HAPPY, VoiceMoment.CRAB_GREETING)).build()),
                List.of(part(InitEntity.MOMONGA_PET.get(), InitEntity.CHIIKAWA_PET.get(), InitEntity.HACHIWARE_PET.get())
                    .pose("crab_greeting").begin(beat(PetReaction.HAPPY, VoiceMoment.CRAB_GREETING)).build()),
                PetInteraction.PartnerState.IDLE, Optional.empty(), Optional.empty(), 0,
                10.0, 2, 300, 60, 12000, 0.05F),
            // The big brother of the two with the least money: it hands something over
            // without a word or a smile, and they eat it on the spot, delighted.
            TREAT, new PetInteraction(
                List.of(part(InitEntity.RAKKO_PET.get()).pose("hand_over").begin(beat(null, VoiceMoment.TREAT)).build()),
                List.of(part(InitEntity.CHIIKAWA_PET.get(), InitEntity.HACHIWARE_PET.get()).pose("eat")
                    .begin(beat(PetReaction.HAPPY, VoiceMoment.TREATED)).build()),
                PetInteraction.PartnerState.IDLE, Optional.empty(),
                Optional.of(new ExtraCodecs.TagOrElementLocation(InitTag.PET_TREATS.location(), true)), 0,
                12.0, 1, 400, 80, 24000, 0.03F),
            // As it did for Chiikawa after the weeding: a quiet coffee, and the one who gets
            // it is keener for a while. Chiikawa first, anyone else who weeds after.
            COFFEE, new PetInteraction(
                List.of(part(InitEntity.KURIMANJU_PET.get()).pose("hand_over").build()),
                List.of(
                    part(InitEntity.CHIIKAWA_PET.get()).pose("drink")
                        .begin(beat(PetReaction.HAPPY, VoiceMoment.GIVEN_COFFEE)).build(),
                    part(InitEntity.HACHIWARE_PET.get(), InitEntity.USAGI_PET.get(), InitEntity.SHISA_PET.get(),
                        InitEntity.MOMONGA_PET.get(), InitEntity.FURUHONYA_PET.get()).pose("drink")
                        .begin(beat(PetReaction.HAPPY, VoiceMoment.GIVEN_COFFEE)).build(),
                    part(InitEntity.RAKKO_PET.get()).pose("drink").begin(beat(null, VoiceMoment.GIVEN_COFFEE)).build()),
                PetInteraction.PartnerState.IDLE,
                Optional.of(new PetInteraction.SlipCondition(PetTaskTypeData.WEEDING, 2400)), Optional.empty(), 1200,
                12.0, 1, 400, 80, 24000, 0.25F),
            // Hachiware plays on; whoever comes by sits down to listen and now and then
            // claps along, with a word when it has one. Rakko and Kurimanju listen without a
            // fuss, and never clap.
            // Music draws a crowd: an idle pet in earshot soon thinks of going over.
            LISTEN_TO_MUSIC, new PetInteraction(
                List.of(
                    part(InitEntity.CHIIKAWA_PET.get(), InitEntity.HACHIWARE_PET.get(), InitEntity.USAGI_PET.get(),
                        InitEntity.SHISA_PET.get(), InitEntity.MOMONGA_PET.get(), InitEntity.FURUHONYA_PET.get())
                        .pose("sit")
                        .begin(beat(PetReaction.HAPPY, VoiceMoment.LISTEN))
                        .nowAndThen(new Beat.Recurring(CLAPS,
                            new Beat(Optional.of("clap"), Optional.empty(), Optional.of(VoiceMoment.LISTEN))))
                        .build(),
                    part(InitEntity.RAKKO_PET.get(), InitEntity.KURIMANJU_PET.get()).pose("sit")
                        .begin(beat(null, VoiceMoment.LISTEN)).build()),
                List.of(part(InitEntity.HACHIWARE_PET.get()).build()),
                PetInteraction.PartnerState.PLAYING_MUSIC, Optional.empty(), Optional.empty(), 0,
                12.0, 3, 400, 400, 6000, 0.6F)
        );
    }

    /** A face and a line, without a move of its own. */
    private static Beat beat(PetReaction reaction, VoiceMoment voice) {
        return new Beat(Optional.empty(), Optional.ofNullable(reaction), Optional.ofNullable(voice));
    }

    private static Part part(EntityType<?>... types) {
        return new Part(Arrays.stream(types)
            .map(type -> new ExtraCodecs.TagOrElementLocation(BuiltInRegistries.ENTITY_TYPE.getKey(type), false))
            .toList());
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Constants.MOD_ID, path);
    }

    /** One part, said the way a stage direction is. */
    private static final class Part {
        private final List<ExtraCodecs.TagOrElementLocation> pets;
        private Optional<String> pose = Optional.empty();
        private Optional<Beat> begin = Optional.empty();
        private Optional<Beat.Recurring> nowAndThen = Optional.empty();
        private Optional<Beat> end = Optional.empty();

        Part(List<ExtraCodecs.TagOrElementLocation> pets) {
            this.pets = pets;
        }

        Part pose(String animation) {
            pose = Optional.of(animation);
            return this;
        }

        Part begin(Beat beat) {
            begin = Optional.of(beat);
            return this;
        }

        Part nowAndThen(Beat.Recurring recurring) {
            nowAndThen = Optional.of(recurring);
            return this;
        }

        Part end(Beat beat) {
            end = Optional.of(beat);
            return this;
        }

        PetInteraction.Side build() {
            return new PetInteraction.Side(pets, pose, begin, nowAndThen, end);
        }
    }
}
