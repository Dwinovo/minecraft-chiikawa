package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.voice.PetVoice;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * The generated pet voices (design 0.1.1, section 3): which moments each character speaks
 * at, how many lines it has for each and how often it picks each one, how likely it is to
 * speak up and how long it keeps quiet after. The words themselves are in
 * {@link LanguageData}, under {@link #lineKey}.
 *
 * <p>Every character talks as the series draws it (research notes, section 2), and a moment
 * it would let pass in silence has no lines here: Kurimanju only sighs over something good
 * to eat or drink, Rakko does not cry out when it is hurt, and Momonga has never been one
 * for a fight.
 *
 * <p>Pets are named by their entity id's path rather than by their registered type, so the
 * table can be read without the game's registries — and checked the same way.
 */
public final class PetVoiceData {
    /** The quiet a pet keeps after saying something, unless it is quieter than most. */
    private static final int COOLDOWN = 200;
    /** How many pets in earshot may already be talking for one more to join in. */
    private static final int CROWD = 3;
    /** How long a line stays up, long enough to read a few words. */
    private static final int TALK = 60;
    /** How far a pet is heard: as far as the game carries a sound at full volume. */
    private static final double HEARING = 16.0;

    private PetVoiceData() {
    }

    /** @return voices by pet entity type id */
    public static Map<ResourceLocation, PetVoice> all() {
        Map<ResourceLocation, PetVoice> all = new HashMap<>();
        // Hardly a word, and tears whenever something goes wrong.
        add(all, voice("chiikawa")
            .says(VoiceMoment.TAME, 1)
            .says(VoiceMoment.HURT, 1, 1)
            .says(VoiceMoment.HUNT, 1)
            .says(VoiceMoment.PAID, 1)
            .says(VoiceMoment.SHOP, 1)
            .says(VoiceMoment.GIFT, 1)
            .says(VoiceMoment.REVIVE, 1)
            .says(VoiceMoment.IDLE, 1, 1)
            .says(VoiceMoment.CLUNG_TO, 1)
            .says(VoiceMoment.TREATED, 1)
            .says(VoiceMoment.GIVEN_COFFEE, 1)
            .says(VoiceMoment.LISTEN, 1)
            .chance(VoiceMoment.IDLE, 0.001F));
        // Cheerful, and one of the few who speak in words.
        add(all, voice("hachiware")
            .says(VoiceMoment.TAME, 1)
            .says(VoiceMoment.HURT, 1)
            .says(VoiceMoment.HUNT, 1)
            .says(VoiceMoment.PAID, 1)
            .says(VoiceMoment.SHOP, 1)
            .says(VoiceMoment.GIFT, 1)
            .says(VoiceMoment.REVIVE, 1)
            .says(VoiceMoment.IDLE, 1)
            .says(VoiceMoment.CLUNG_TO, 1)
            .says(VoiceMoment.CRAB_GREETING, 1)
            .says(VoiceMoment.TREATED, 1)
            .says(VoiceMoment.GIVEN_COFFEE, 1)
            .says(VoiceMoment.LISTEN, 1)
            .chance(VoiceMoment.IDLE, 0.002F));
        // Nothing but its own noises, and never a tear.
        add(all, voice("usagi")
            .says(VoiceMoment.TAME, 1)
            .says(VoiceMoment.HURT, 1)
            .says(VoiceMoment.HUNT, 1)
            .says(VoiceMoment.PAID, 1)
            .says(VoiceMoment.SHOP, 1)
            .says(VoiceMoment.GIFT, 1)
            .says(VoiceMoment.REVIVE, 1)
            .says(VoiceMoment.IDLE, 1, 1, 1)
            .says(VoiceMoment.CLUNG_TO, 1)
            .says(VoiceMoment.GIVEN_COFFEE, 1)
            .says(VoiceMoment.LISTEN, 1)
            .chance(VoiceMoment.IDLE, 0.002F));
        // Wants praise for everything, and comfort for the rest; no fighter. It never lets
        // up: quiet only while its last demand is still over its head, so a demand for
        // praise can be followed by one for comfort within the same little scene. Someone
        // else's music gets a word only now and then.
        add(all, voice("momonga")
            .says(VoiceMoment.TAME, 1)
            .says(VoiceMoment.HURT, 1)
            .says(VoiceMoment.PAID, 1)
            .says(VoiceMoment.SHOP, 1)
            .says(VoiceMoment.GIFT, 1)
            .says(VoiceMoment.REVIVE, 1)
            .says(VoiceMoment.IDLE, 2, 1)
            .says(VoiceMoment.CLING, 1)
            .says(VoiceMoment.TURNED_DOWN, 1)
            .says(VoiceMoment.CRAB_GREETING, 1)
            .says(VoiceMoment.GIVEN_COFFEE, 1)
            .says(VoiceMoment.LISTEN, 1)
            .chance(VoiceMoment.IDLE, 0.002F)
            .chance(VoiceMoment.LISTEN, 0.5F)
            .cooldown(TALK));
        // A grown-up of few words: a sigh over something good, and that is all.
        add(all, voice("kurimanju")
            .says(VoiceMoment.SHOP, 1)
            .cooldown(2 * COOLDOWN));
        // Hardboiled, a word or two at most, softer only over something sweet.
        add(all, voice("rakko")
            .says(VoiceMoment.TAME, 1)
            .says(VoiceMoment.HUNT, 1)
            .says(VoiceMoment.PAID, 1)
            .says(VoiceMoment.SHOP, 1)
            .says(VoiceMoment.GIFT, 1)
            .says(VoiceMoment.REVIVE, 1)
            .says(VoiceMoment.TREAT, 1)
            .says(VoiceMoment.GIVEN_COFFEE, 1)
            .cooldown(2 * COOLDOWN));
        // Polite and earnest, with a touch of Okinawa.
        add(all, voice("shisa")
            .says(VoiceMoment.TAME, 2, 1)
            .says(VoiceMoment.HURT, 1)
            .says(VoiceMoment.HUNT, 1)
            .says(VoiceMoment.PAID, 1)
            .says(VoiceMoment.SHOP, 1)
            .says(VoiceMoment.GIFT, 1)
            .says(VoiceMoment.REVIVE, 1)
            .says(VoiceMoment.IDLE, 1, 1)
            .says(VoiceMoment.CLUNG_TO, 1)
            .says(VoiceMoment.GIVEN_COFFEE, 1)
            .says(VoiceMoment.LISTEN, 1)
            .chance(VoiceMoment.IDLE, 0.0015F));
        // Quiet and kind: the crab greeting, and a word when it gives something.
        add(all, voice("furuhonya")
            .says(VoiceMoment.TAME, 1)
            .says(VoiceMoment.GIFT, 1)
            .says(VoiceMoment.IDLE, 1)
            .says(VoiceMoment.CRAB_GREETING, 1)
            .says(VoiceMoment.GIVEN_COFFEE, 1)
            .chance(VoiceMoment.IDLE, 0.0005F)
            .cooldown(2 * COOLDOWN));
        return all;
    }

    /**
     * The translation key of one of a pet's lines, shared with the wording in
     * {@link LanguageData}.
     *
     * @param pet the pet's entity id path, such as {@code chiikawa}
     * @param line counted from 1
     */
    public static String lineKey(String pet, VoiceMoment moment, int line) {
        return "voice." + Constants.MOD_ID + "." + pet + "." + moment.getSerializedName() + "." + line;
    }

    private static void add(Map<ResourceLocation, PetVoice> all, Builder builder) {
        all.put(new ResourceLocation(Constants.MOD_ID, builder.pet), builder.build());
    }

    private static Builder voice(String pet) {
        return new Builder(pet);
    }

    private static final class Builder {
        private final String pet;
        private final Map<VoiceMoment, List<PetVoice.Line>> lines = new EnumMap<>(VoiceMoment.class);
        private final Map<VoiceMoment, Float> chance = new EnumMap<>(VoiceMoment.class);
        private int cooldown = COOLDOWN;

        Builder(String pet) {
            this.pet = pet;
        }

        /** Lines for a moment, one per weight, numbered from 1 in that order. */
        Builder says(VoiceMoment moment, int... weights) {
            List<PetVoice.Line> said = new ArrayList<>();
            for (int i = 0; i < weights.length; i++) {
                said.add(new PetVoice.Line(lineKey(pet, moment, i + 1), weights[i]));
            }
            lines.put(moment, said);
            return this;
        }

        Builder chance(VoiceMoment moment, float likely) {
            chance.put(moment, likely);
            return this;
        }

        Builder cooldown(int ticks) {
            this.cooldown = ticks;
            return this;
        }

        PetVoice build() {
            return new PetVoice(lines, chance, cooldown, CROWD, TALK, HEARING);
        }
    }
}
