package com.dwinovo.chiikawa.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.anim.state.PetAction;
import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.data.ErrandSettingsData;
import com.dwinovo.chiikawa.data.PastimeData;
import com.dwinovo.chiikawa.data.PetRemarkData;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.remark.PetRemark;
import com.dwinovo.chiikawa.entity.job.impl.MusicianJob;
import com.dwinovo.chiikawa.testing.AnimationFiles;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Animations ship as named candidates with fallbacks, so a pet whose model lacks the first does the next; the first
 * candidate is the animation meant, the rest are what a pet without it does meanwhile. What the code means and a
 * character that can reach it does not have yet is the animators' backlog, and it is written down in
 * {@code pending_animations.txt}, one line for each character that lacks one. The list is held to the files both ways:
 * a name that is lacking and not on the list fails, so a new candidate cannot be added without the backlog saying so,
 * and a line for an animation that has been made, or that nothing asks of the character, fails too, so the list shrinks
 * to empty as the animations are made.
 *
 * <p>A character is asked only for what it can reach. The first candidates of {@link PetActivity}, {@link PetAction} and
 * {@link PetReaction} are asked of every character, except those of an activity that belongs to a job only one
 * character can assume ({@link MusicianJob#PLAYER}, who plays the guitar), and the two generic actions, which only
 * the deprecated {@link com.dwinovo.chiikawa.entity.AbstractPet#triggerAnim} names and nothing calls: they are what
 * the other actions fall back to. The moves the scenes between pets name are data that needs the entity registry to
 * read, which a test outside a game has not. How a character cowers and sleeps, and what it does at the pastimes it has a part in
 * and when it makes a remark, are data that needs no registry, and are asked.
 */
class AnimationBacklogTest {
    /** @return the animation meant by each value of the enums, the first of its candidates, that the character can reach */
    private static Set<String> asked(String character) {
        Set<String> names = new TreeSet<>();
        Arrays.stream(PetActivity.values()).filter(activity -> activity != PetActivity.NONE)
            .filter(activity -> activity != PetActivity.PLAY_GUITAR || plays(character))
            .forEach(activity -> names.add(activity.animationCandidates().get(0)));
        Arrays.stream(PetAction.values())
            .filter(action -> action != PetAction.NONE && action != PetAction.GENERIC_USE_MAINHAND && action != PetAction.GENERIC_SWORD_ATTACK)
            .forEach(action -> names.add(action.animationCandidates().get(0)));
        Arrays.stream(PetReaction.values()).filter(reaction -> reaction != PetReaction.NONE)
            .forEach(reaction -> names.add(reaction.animationCandidates().get(0)));
        names.add(cowering(character));
        names.add(sleeping(character));
        names.addAll(passingTheTime(character));
        return names;
    }

    /** @return the animation the character sleeps with, which is the first its data names for it */
    private static String sleeping(String character) {
        ErrandTunings.Sleep sleep = (ErrandTunings.Sleep) ErrandSettingsData.all().get(ErrandIds.SLEEP).tuning();
        return sleep.sleeps().of(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, character)).get(0);
    }

    /**
     * @return the animations the character passes the time with and makes a remark with: the first of the pose of its part in
     *         each pastime, the moves of its beats, and the gesture of the remarks it has a part in
     */
    private static Set<String> passingTheTime(String character) {
        ResourceLocation who = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, character);
        Set<String> names = new TreeSet<>();
        PastimeData.works().values().forEach(work -> work.pastime().map(pastime -> pastime.cast().get(who)).ifPresent(stage -> {
            stage.pose().ifPresent(pose -> names.add(pose.split(",")[0]));
            stage.begin().ifPresent(beat -> beat.animation().ifPresent(names::add));
            stage.nowAndThen().ifPresent(recurring -> recurring.beat().animation().ifPresent(names::add));
            stage.end().ifPresent(beat -> beat.animation().ifPresent(names::add));
        }));
        PetRemarkData.all().values().forEach(remark -> remark.partOf(who).flatMap(PetRemark.Part::gesture).ifPresent(names::add));
        // Momonga copies a pet, and does what it has for want of one that it can.
        PastimeData.works().forEach((errand, work) -> work.pastime().filter(pastime -> pastime.cast().containsKey(who))
            .map(pastime -> PastimeData.settings().get(errand).tuning()).filter(ErrandTunings.AskPraise.class::isInstance)
            .ifPresent(tuning -> names.add(((ErrandTunings.AskPraise) tuning).imitates().split(",")[0])));
        return names;
    }

    /** @return the animation the character cowers with when it is frightened, which is the first its data names for it */
    private static String cowering(String character) {
        ErrandTunings.Flee flee = (ErrandTunings.Flee) ErrandSettingsData.all().get(ErrandIds.FLEE).tuning();
        return flee.cowers().of(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, character)).get(0);
    }

    /** @return whether the character is the one the musician job is open to, by the name of its class */
    private static boolean plays(String character) {
        return MusicianJob.PLAYER.getSimpleName().equalsIgnoreCase(character + "Pet");
    }

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void whatTheEnumsNameAndACharacterLacksIsOnTheBacklogAndNothingElseIs() {
        Set<String> lacking = new TreeSet<>();
        AnimationFiles.made().forEach((character, animations) -> asked(character).stream().filter(name -> !animations.contains(name))
            .forEach(name -> lacking.add(character + " " + name)));
        Set<String> pending = AnimationFiles.pending();

        Set<String> unlisted = new TreeSet<>(lacking);
        unlisted.removeAll(pending);
        Set<String> stale = new TreeSet<>(pending);
        stale.removeAll(lacking);

        assertEquals(Set.of(), unlisted, "meant by the code for this character, not in its file and not on the backlog: add the animation or the line");
        assertEquals(Set.of(), stale, "on the backlog, though the animation exists or the character cannot reach it: take the line off");
    }

    @Test
    void theBacklogNamesCharactersWeHave() {
        Set<String> characters = AnimationFiles.made().keySet();

        AnimationFiles.pending().forEach(line -> assertTrue(characters.contains(line.split(" ")[0]), line + " is no character"));
    }
}
