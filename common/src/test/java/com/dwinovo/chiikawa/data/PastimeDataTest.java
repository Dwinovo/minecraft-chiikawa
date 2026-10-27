package com.dwinovo.chiikawa.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.PetCharacters;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.pastime.Pastime;
import com.dwinovo.chiikawa.entity.brain.errand.pastime.PastimeTuning;
import com.dwinovo.chiikawa.entity.brain.errand.pastime.Stage;
import com.dwinovo.chiikawa.entity.brain.errand.remark.PetRemark;
import com.dwinovo.chiikawa.social.Beat;
import com.dwinovo.chiikawa.testing.AnimationFiles;
import com.dwinovo.chiikawa.voice.PetVoice;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The pastimes the mod ships, as the series draws the characters (0.1.4 design, the first batch of section 11): that each is
 * done by the character the table gives it to, that every word a pet says in one is in its voice in every language, that
 * Usagi never cries, and that every animation a pastime asks for is one the character has or one on the animators' backlog,
 * with something it has to fall back on.
 */
class PastimeDataTest {
    private static final ResourceLocation USAGI = PetCharacters.USAGI;

    /** The first batch, by whose it is. */
    private static final Map<ResourceLocation, ResourceLocation> OF_ONE = Map.of(
        CharacterPastimeData.COWER, PetCharacters.CHIIKAWA,
        CharacterPastimeData.FRENZY, PetCharacters.USAGI,
        CharacterPastimeData.ASK_PRAISE, PetCharacters.MOMONGA,
        CharacterPastimeData.EVENING_DRINK, PetCharacters.KURIMANJU,
        CharacterPastimeData.COFFEE_FOR_A_WORKER, PetCharacters.KURIMANJU,
        CharacterPastimeData.SWEETS_STARE, PetCharacters.RAKKO,
        CharacterPastimeData.SWORD_PRACTICE, PetCharacters.RAKKO,
        CharacterPastimeData.GREET_OWNER, PetCharacters.SHISA);

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    /** @return every beat of a stage, as it plays them */
    private static List<Beat> beatsOf(Stage stage) {
        List<Beat> beats = new ArrayList<>();
        stage.begin().ifPresent(beats::add);
        stage.nowAndThen().ifPresent(recurring -> beats.add(recurring.beat()));
        stage.end().ifPresent(beats::add);
        return beats;
    }

    /** @return every animation a character is asked for in the pastimes and remarks, the candidates of a pose in order */
    private static Map<ResourceLocation, List<List<String>>> animationsAsked() {
        Map<ResourceLocation, List<List<String>>> asked = new HashMap<>();
        PastimeData.works().forEach((errand, work) -> work.pastime().ifPresent(pastime -> pastime.cast().forEach((who, stage) -> {
            List<List<String>> mine = asked.computeIfAbsent(who, key -> new ArrayList<>());
            stage.pose().ifPresent(pose -> mine.add(List.of(pose.split(","))));
            beatsOf(stage).forEach(beat -> beat.animation().ifPresent(name -> mine.add(List.of(name))));
            Object tuning = PastimeData.settings().get(errand).tuning();
            if (tuning instanceof ErrandTunings.AskPraise asking) {
                mine.add(List.of(asking.imitates().split(",")));
            }
        })));
        PetRemarkData.all().values().forEach(remark -> PetCharacters.all().forEach(who -> remark.partOf(who)
            .flatMap(PetRemark.Part::gesture).ifPresent(name -> asked.computeIfAbsent(who, key -> new ArrayList<>()).add(List.of(name)))));
        return asked;
    }

    /** @return every voice moment a character speaks at in the pastimes and remarks it has a part in */
    private static Map<ResourceLocation, Set<VoiceMoment>> momentsSpoken() {
        Map<ResourceLocation, Set<VoiceMoment>> spoken = new HashMap<>();
        PastimeData.works().values().forEach(work -> work.pastime().ifPresent(pastime -> pastime.cast().forEach((who, stage) ->
            beatsOf(stage).forEach(beat -> beat.voice().ifPresent(moment ->
                spoken.computeIfAbsent(who, key -> new TreeSet<>()).add(moment))))));
        PetRemarkData.all().values().forEach(remark -> PetCharacters.all().forEach(who -> remark.partOf(who)
            .ifPresent(part -> spoken.computeIfAbsent(who, key -> new TreeSet<>()).add(part.moment()))));
        return spoken;
    }

    @Test
    void theFirstBatchIsTheEightTheTableNamesAndEachIsOnlyForTheCharacterItIsFor() {
        assertEquals(new TreeSet<>(OF_ONE.keySet().stream().map(ResourceLocation::toString).toList()),
            new TreeSet<>(CharacterPastimeData.ids().stream().map(ResourceLocation::toString).toList()));
        OF_ONE.forEach((errand, character) -> assertEquals(Set.of(character),
            PastimeData.works().get(errand).pastime().orElseThrow().cast().keySet(), errand::toString));
    }

    @Test
    void everyoneWhoTakesPartInAPastimeIsACharacter() {
        PastimeData.works().forEach((errand, work) -> work.pastime().ifPresent(pastime ->
            pastime.cast().keySet().forEach(who -> assertTrue(PetCharacters.is(who), errand + " casts " + who))));
    }

    @Test
    void everyMomentAPetSpeaksAtInAPastimeHasLinesInItsVoiceAndTheWordsInEveryLanguage() {
        Map<ResourceLocation, PetVoice> voices = PetVoiceData.all();
        Map<String, Map<String, String>> words = new HashMap<>();
        for (String locale : LanguageData.LOCALES) {
            Map<String, String> translations = new HashMap<>();
            LanguageData.addTranslations(locale, translations::put);
            words.put(locale, translations);
        }

        momentsSpoken().forEach((who, moments) -> moments.forEach(moment -> {
            List<PetVoice.Line> lines = voices.get(who).lines().getOrDefault(moment, List.of());
            assertFalse(lines.isEmpty(), who + " has nothing to say at " + moment);
            words.forEach((locale, translations) -> lines.forEach(line -> assertTrue(
                translations.getOrDefault(line.text(), "").length() > 0, locale + " has no words for " + line.text())));
        }));
    }

    @Test
    void usagiNeverCriesInAnythingItDoesOrSays() {
        List<String> cries = new ArrayList<>();
        animationsAsked().getOrDefault(USAGI, List.of()).forEach(candidates -> candidates.stream()
            .filter(name -> name.startsWith("cry")).forEach(cries::add));
        PetVoiceData.all().get(USAGI).lines().keySet().stream().filter(moment -> moment.getSerializedName().startsWith("cry"))
            .forEach(moment -> cries.add(moment.getSerializedName()));
        ErrandSettingsData.all().values().forEach(settings -> {
            if (settings.tuning() instanceof ErrandTunings.Flee flee) {
                flee.cowers().of(USAGI).stream().filter(name -> name.startsWith("cry")).forEach(cries::add);
            }
            if (settings.tuning() instanceof ErrandTunings.Sleep sleep) {
                sleep.sleeps().of(USAGI).stream().filter(name -> name.startsWith("cry")).forEach(cries::add);
            }
        });

        assertEquals(List.of(), cries);
    }

    @Test
    void everyAnimationAPastimeAsksForIsMadeOrOnTheBacklogAndHasSomethingToFallBackOn() {
        Map<String, Set<String>> made = AnimationFiles.made();
        Set<String> pending = AnimationFiles.pending();

        animationsAsked().forEach((who, poses) -> {
            String character = who.getPath();
            for (List<String> candidates : poses) {
                String meant = candidates.get(0);
                assertTrue(made.get(character).contains(meant) || pending.contains(character + " " + meant),
                    character + " is asked for " + meant + ", which it has not and the backlog does not have");
                // The rest is what a pet without it does meanwhile, and a character that has it is not left standing.
                assertTrue(candidates.size() == 1 || candidates.stream().skip(1).anyMatch(made.get(character)::contains),
                    character + " has no animation to fall back on for " + candidates);
            }
        });
    }

    @Test
    void theRarePastimesRestForALongTimeAndTheOnesWithAReasonForAWhile() {
        long rare = 12000;
        for (ResourceLocation errand : List.of(CharacterPastimeData.FRENZY, CharacterPastimeData.SWORD_PRACTICE)) {
            Pastime pastime = PastimeData.works().get(errand).pastime().orElseThrow();
            assertTrue(pastime.when().isEmpty(), errand + " has a reason");
            assertTrue(((PastimeTuning) PastimeData.settings().get(errand).tuning()).cooldownTicks() >= rare, errand.toString());
        }
        assertTrue(((PastimeTuning) PastimeData.settings().get(CharacterPastimeData.EVENING_DRINK).tuning()).cooldownTicks() >= 24000,
            "an evening is a day apart from the next");
    }

    @Test
    void theFrightIsEndedByTheOwnerComingNearAfterItHasShown() {
        Stage cowering = PastimeData.works().get(CharacterPastimeData.COWER).pastime().orElseThrow().cast()
            .get(PetCharacters.CHIIKAWA);
        Optional<Stage.Until> until = cowering.until();

        assertTrue(until.isPresent());
        assertTrue(until.get().afterTicks() > 0, "it cries before it is comforted");
        assertTrue(until.get().afterTicks() < cowering.ticks());
    }

    @Test
    void thePastimesAreAllAtThePriorityOfIdleTime() {
        CharacterPastimeData.ids().forEach(errand -> assertEquals(10, ErrandSettingsData.all().get(errand).priority(), errand.toString()));
        assertTrue(ErrandSettingsData.all().containsKey(ErrandIds.WANDER));
    }
}
