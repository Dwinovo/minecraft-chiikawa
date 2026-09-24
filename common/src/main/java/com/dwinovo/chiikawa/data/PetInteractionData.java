package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.init.InitTag;
import com.dwinovo.chiikawa.social.PetInteraction;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
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
    public static final ResourceLocation CLING = id("cling");
    /** Furuhonya and a friend greet each other the crab way, face to face. */
    public static final ResourceLocation CRAB_GREETING = id("crab_greeting");
    /** Rakko treats Chiikawa or Hachiware to something to eat out of its own backpack. */
    public static final ResourceLocation TREAT = id("treat");
    /** Kurimanju brings a coffee to someone who has just finished weeding. */
    public static final ResourceLocation COFFEE = id("coffee");
    /** Anyone sits down by Hachiware busking and listens for a while. */
    public static final ResourceLocation LISTEN_TO_MUSIC = id("listen_to_music");

    private PetInteractionData() {
    }

    /** @return scenes by id */
    public static Map<ResourceLocation, PetInteraction> all() {
        return Map.of(
            // Momonga clings on and wants to be praised; the one clung to minds. Chiikawa,
            // whom it has bitten more than once, cries; the rest are put out each in its own
            // way, and Rakko and Kurimanju hardly let it show.
            CLING, new PetInteraction(
                List.of(side("cling", PetReaction.HAPPY, VoiceMoment.CLING, pets(InitEntity.MOMONGA_PET.get()))),
                List.of(
                    side("clung_to", PetReaction.HURT, VoiceMoment.CLUNG_TO, pets(InitEntity.CHIIKAWA_PET.get())),
                    side("clung_to", PetReaction.CONFUSED, VoiceMoment.CLUNG_TO, pets(InitEntity.HACHIWARE_PET.get(),
                        InitEntity.USAGI_PET.get(), InitEntity.SHISA_PET.get(), InitEntity.FURUHONYA_PET.get())),
                    side("clung_to", null, VoiceMoment.CLUNG_TO, pets(InitEntity.RAKKO_PET.get(), InitEntity.KURIMANJU_PET.get()))),
                PetInteraction.PartnerState.IDLE, Optional.empty(), Optional.empty(), 0,
                8.0, 1, 300, 80, 12000, 0.05F),
            // Furuhonya greets the friends who greet it back the crab way, Momonga first: the
            // crab headband was Momonga's present.
            CRAB_GREETING, new PetInteraction(
                List.of(side("crab_greeting", PetReaction.HAPPY, VoiceMoment.CRAB_GREETING, pets(InitEntity.FURUHONYA_PET.get()))),
                List.of(side("crab_greeting", PetReaction.HAPPY, VoiceMoment.CRAB_GREETING, pets(InitEntity.MOMONGA_PET.get(),
                    InitEntity.CHIIKAWA_PET.get(), InitEntity.HACHIWARE_PET.get()))),
                PetInteraction.PartnerState.IDLE, Optional.empty(), Optional.empty(), 0,
                10.0, 2, 300, 60, 12000, 0.05F),
            // The big brother of the two with the least money: it hands something over
            // without a word or a smile, and they eat it on the spot, delighted.
            TREAT, new PetInteraction(
                List.of(side("hand_over", null, VoiceMoment.TREAT, pets(InitEntity.RAKKO_PET.get()))),
                List.of(side("eat", PetReaction.HAPPY, VoiceMoment.TREATED, pets(InitEntity.CHIIKAWA_PET.get(),
                    InitEntity.HACHIWARE_PET.get()))),
                PetInteraction.PartnerState.IDLE, Optional.empty(),
                Optional.of(new ExtraCodecs.TagOrElementLocation(InitTag.PET_TREATS.location(), true)), 0,
                12.0, 1, 400, 80, 24000, 0.03F),
            // As it did for Chiikawa after the weeding: a quiet coffee, and the one who gets
            // it is keener for a while. Chiikawa first, anyone else who weeds after.
            COFFEE, new PetInteraction(
                List.of(side("hand_over", null, null, pets(InitEntity.KURIMANJU_PET.get()))),
                List.of(
                    side("drink", PetReaction.HAPPY, VoiceMoment.GIVEN_COFFEE, pets(InitEntity.CHIIKAWA_PET.get())),
                    side("drink", PetReaction.HAPPY, VoiceMoment.GIVEN_COFFEE, pets(InitEntity.HACHIWARE_PET.get(),
                        InitEntity.USAGI_PET.get(), InitEntity.SHISA_PET.get(), InitEntity.MOMONGA_PET.get(),
                        InitEntity.FURUHONYA_PET.get())),
                    side("drink", null, VoiceMoment.GIVEN_COFFEE, pets(InitEntity.RAKKO_PET.get()))),
                PetInteraction.PartnerState.IDLE,
                Optional.of(new PetInteraction.SlipCondition(PetTaskTypeData.WEEDING, 2400)), Optional.empty(), 1200,
                12.0, 1, 400, 80, 24000, 0.25F),
            // Hachiware plays on; whoever comes by sits down to listen. Rakko and Kurimanju
            // listen without a fuss.
            LISTEN_TO_MUSIC, new PetInteraction(
                List.of(
                    side("sit", PetReaction.HAPPY, VoiceMoment.LISTEN, pets(InitEntity.CHIIKAWA_PET.get(),
                        InitEntity.HACHIWARE_PET.get(), InitEntity.USAGI_PET.get(), InitEntity.SHISA_PET.get(),
                        InitEntity.MOMONGA_PET.get(), InitEntity.FURUHONYA_PET.get())),
                    side("sit", null, VoiceMoment.LISTEN, pets(InitEntity.RAKKO_PET.get(), InitEntity.KURIMANJU_PET.get()))),
                List.of(side(null, null, null, pets(InitEntity.HACHIWARE_PET.get()))),
                PetInteraction.PartnerState.PLAYING_MUSIC, Optional.empty(), Optional.empty(), 0,
                12.0, 3, 400, 400, 6000, 0.1F)
        );
    }

    private static PetInteraction.Side side(String animation, PetReaction reaction, VoiceMoment voice,
            List<ExtraCodecs.TagOrElementLocation> pets) {
        return new PetInteraction.Side(pets, Optional.ofNullable(animation), Optional.ofNullable(reaction),
            Optional.ofNullable(voice));
    }

    private static List<ExtraCodecs.TagOrElementLocation> pets(EntityType<?>... types) {
        return Arrays.stream(types)
            .map(type -> new ExtraCodecs.TagOrElementLocation(BuiltInRegistries.ENTITY_TYPE.getKey(type), false))
            .toList();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
    }
}
