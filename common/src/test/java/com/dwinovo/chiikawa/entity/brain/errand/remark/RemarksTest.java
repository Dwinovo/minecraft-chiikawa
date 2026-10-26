package com.dwinovo.chiikawa.entity.brain.errand.remark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.data.PetRemarkData;
import com.dwinovo.chiikawa.data.PetVoiceData;
import com.dwinovo.chiikawa.entity.brain.errand.LackKey;
import com.dwinovo.chiikawa.entity.brain.errand.decide.WhyNot;
import com.dwinovo.chiikawa.entity.brain.errand.run.Aside;
import com.dwinovo.chiikawa.voice.PetVoice;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import org.junit.jupiter.api.Test;

/** The remarks a pet makes in passing: when they are due, who makes them, and that they are made once. */
class RemarksTest {
    private static final ResourceLocation HARVEST = ResourceLocation.fromNamespaceAndPath("chiikawa", "harvest_block");
    private static final ResourceLocation CHIIKAWA = ResourceLocation.fromNamespaceAndPath("chiikawa", "chiikawa");
    private static final ResourceLocation HACHIWARE = ResourceLocation.fromNamespaceAndPath("chiikawa", "hachiware");
    private static final String ARROWS = "tag.item.minecraft.arrows";
    private static final Map<ResourceLocation, PetRemark> SHIPPED = PetRemarkData.all();
    private static final long SINCE = 1000L;

    @Test
    void aPetShortOfSomethingForHalfAMinuteWithItsOwnerByAsksForItInItsOwnWords() {
        Said said = new Said(true);

        Optional<RemarkRests> rests = Remarks.remark(SHIPPED, CHIIKAWA, lacking(SINCE + 600, 5.0), RemarkRests.NONE, said);

        assertEquals(List.of(VoiceMoment.MISSING), said.moments);
        assertEquals(List.of(Optional.of(ARROWS)), said.subjects);
        assertTrue(rests.orElseThrow().resting(PetRemarkData.MISSING, SINCE + 600));
    }

    @Test
    void notBeforeTheLackHasLastedThatLong() {
        Said said = new Said(true);

        assertEquals(Optional.empty(), Remarks.remark(SHIPPED, CHIIKAWA, lacking(SINCE + 599, 5.0), RemarkRests.NONE, said));
        assertEquals(List.of(), said.moments);
    }

    @Test
    void notWithNobodyNearToHearIt() {
        Said said = new Said(true);

        assertEquals(Optional.empty(), Remarks.remark(SHIPPED, CHIIKAWA, lacking(SINCE + 600, 8.5), RemarkRests.NONE, said));
        assertEquals(Optional.empty(), Remarks.remark(SHIPPED, CHIIKAWA, situation(Optional.of(lack()), SINCE + 600,
            OptionalDouble.empty()), RemarkRests.NONE, said));
        assertEquals(List.of(), said.moments);
        assertTrue(Remarks.remark(SHIPPED, CHIIKAWA, lacking(SINCE + 600, 8.0), RemarkRests.NONE, said).isPresent());
    }

    @Test
    void notForAPetThatHasNoWorkToDoButIsShortOfNothing() {
        Said said = new Said(true);
        WhyNot seeking = new WhyNot(HARVEST, "intent.chiikawa.fail.no_crop", Optional.empty(), SINCE);

        assertEquals(Optional.empty(), Remarks.remark(SHIPPED, CHIIKAWA, situation(Optional.of(seeking), SINCE + 6000, OptionalDouble.of(2.0)),
            RemarkRests.NONE, said));
        assertEquals(Optional.empty(), Remarks.remark(SHIPPED, CHIIKAWA, situation(Optional.empty(), SINCE + 6000, OptionalDouble.of(2.0)),
            RemarkRests.NONE, said));
        assertEquals(List.of(), said.moments);
    }

    @Test
    void onceMadeItIsLeftAloneUntilItsRestIsOver() {
        Said said = new Said(true);
        int cooldown = SHIPPED.get(PetRemarkData.MISSING).cooldownTicks();
        RemarkRests rests = Remarks.remark(SHIPPED, CHIIKAWA, lacking(SINCE + 600, 5.0), RemarkRests.NONE, said).orElseThrow();

        assertEquals(Optional.empty(), Remarks.remark(SHIPPED, CHIIKAWA, lacking(SINCE + 600 + cooldown - 1, 5.0), rests, said));
        assertTrue(Remarks.remark(SHIPPED, CHIIKAWA, lacking(SINCE + 600 + cooldown, 5.0), rests, said).isPresent());
        assertEquals(2, said.moments.size());
    }

    @Test
    void hachiwareDoesNotSayWhatItNeedsButScratchesItsHeadAndTrustsItWillTurnUp() {
        Said said = new Said(true);

        Remarks.remark(SHIPPED, HACHIWARE, lacking(SINCE + 600, 5.0), RemarkRests.NONE, said);

        assertEquals(List.of(VoiceMoment.TROUBLE), said.moments);
        assertEquals(List.of("scratch_head"), said.gestures);
    }

    @Test
    void whoeverHasNoPartAndNoEveryonesToFallBackOnSaysNothing() {
        PetRemark onlyHachiware = new PetRemark(SHIPPED.get(PetRemarkData.MISSING).when(),
            Map.of(HACHIWARE.toString(), new PetRemark.Part(VoiceMoment.TROUBLE, Optional.empty())), 100);
        Said said = new Said(true);

        assertEquals(Optional.empty(), Remarks.remark(Map.of(PetRemarkData.MISSING, onlyHachiware), CHIIKAWA,
            lacking(SINCE + 600, 5.0), RemarkRests.NONE, said));
        assertEquals(List.of(), said.moments);
    }

    @Test
    void aRemarkThatWasNotSaidIsNotRestedAndIsTriedAgain() {
        Said hushed = new Said(false);
        Said said = new Said(true);

        assertEquals(Optional.empty(), Remarks.remark(SHIPPED, CHIIKAWA, lacking(SINCE + 600, 5.0), RemarkRests.NONE, hushed));
        assertEquals(List.of(), hushed.gestures);
        assertTrue(Remarks.remark(SHIPPED, CHIIKAWA, lacking(SINCE + 601, 5.0), RemarkRests.NONE, said).isPresent());
    }

    @Test
    void whenTheWorldTurnsChatterOffThePetStaysSilentAndTheRemarkIsNotUsedUp() {
        PetVoice voice = PetVoiceData.all().get(CHIIKAWA);
        RandomSource random = RandomSource.create(7L);

        assertEquals(Optional.empty(), Remarks.remark(SHIPPED, CHIIKAWA, lacking(SINCE + 600, 5.0), RemarkRests.NONE,
            new VoicedAside(voice, 0.0F, random)));
        assertTrue(Remarks.remark(SHIPPED, CHIIKAWA, lacking(SINCE + 600, 5.0), RemarkRests.NONE,
            new VoicedAside(voice, 1.0F, random)).isPresent());
    }

    @Test
    void aCharacterWithNoWordsForTheMomentSaysNothingEvenWhenTheRemarkIsDue() {
        // A remark only names the moment: the words are the voice's, and a voice with none for it lets it pass in silence.
        PetVoice mute = new PetVoice(Map.of(), Map.of(), 0, 1, 1, 0.0);

        assertEquals(Optional.empty(), Remarks.remark(SHIPPED, CHIIKAWA, lacking(SINCE + 600, 5.0), RemarkRests.NONE,
            new VoicedAside(mute, 1.0F, RandomSource.create(7L))));
    }

    private static WhyNot lack() {
        return new WhyNot(HARVEST, "intent.chiikawa.fail.no_arrows", Optional.of(LackKey.tag(ItemTags.ARROWS)), SINCE);
    }

    private static RemarkCue.Situation lacking(long now, double ownerDistance) {
        return situation(Optional.of(lack()), now, OptionalDouble.of(ownerDistance));
    }

    private static RemarkCue.Situation situation(Optional<WhyNot> whyNot, long now, OptionalDouble ownerDistance) {
        return new RemarkCue.Situation(whyNot, now, ownerDistance);
    }

    /** An aside that says whatever it is asked, or nothing, and notes it. */
    private static final class Said implements Aside {
        private final boolean speaks;
        final List<VoiceMoment> moments = new ArrayList<>();
        final List<Optional<String>> subjects = new ArrayList<>();
        final List<String> gestures = new ArrayList<>();

        Said(boolean speaks) {
            this.speaks = speaks;
        }

        @Override
        public boolean say(VoiceMoment moment, Optional<String> subject) {
            if (speaks) {
                moments.add(moment);
                subjects.add(subject);
            }
            return speaks;
        }

        @Override
        public void gesture(String name) {
            gestures.add(name);
        }

        @Override
        public void glance(Entity target, int ticks) {
        }
    }

    /** An aside that says what a pet's voice draws under the world's chatter setting, as a pet does. */
    private static final class VoicedAside implements Aside {
        private final PetVoice voice;
        private final float chatter;
        private final RandomSource random;

        VoicedAside(PetVoice voice, float chatter, RandomSource random) {
            this.voice = voice;
            this.chatter = chatter;
            this.random = random;
        }

        @Override
        public boolean say(VoiceMoment moment, Optional<String> subject) {
            return voice.draw(moment, random, chatter).isPresent();
        }

        @Override
        public void gesture(String name) {
        }

        @Override
        public void glance(Entity target, int ticks) {
        }
    }
}
