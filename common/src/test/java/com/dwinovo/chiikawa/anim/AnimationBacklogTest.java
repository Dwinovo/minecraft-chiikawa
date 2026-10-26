package com.dwinovo.chiikawa.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.anim.state.PetAction;
import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.entity.job.impl.MusicianJob;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
 * read, which a test outside a game has not.
 */
class AnimationBacklogTest {
    private static final Path ANIMATIONS = Path.of("src", "main", "resources", "assets", "chiikawa", "animations");
    private static final Path PENDING = Path.of("src", "test", "resources", "pending_animations.txt");

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
        return names;
    }

    /** @return whether the character is the one the musician job is open to, by the name of its class */
    private static boolean plays(String character) {
        return MusicianJob.PLAYER.getSimpleName().equalsIgnoreCase(character + "Pet");
    }

    /** @return the animations each character has, by the name of its file */
    private static Map<String, Set<String>> made() {
        assertTrue(Files.isDirectory(ANIMATIONS), "the scan runs from the common module, not from " + Path.of("").toAbsolutePath());
        try (Stream<Path> files = Files.list(ANIMATIONS)) {
            Map<String, Set<String>> made = new TreeMap<>();
            for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                String character = file.getFileName().toString().replaceAll("\\.json$", "");
                made.put(character, JsonParser.parseString(Files.readString(file)).getAsJsonObject().getAsJsonObject("animations")
                    .keySet().stream().collect(Collectors.toCollection(TreeSet::new)));
            }
            return made;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** @return the backlog, as {@code character animation} for each line */
    private static Set<String> pending() {
        try {
            return Files.readAllLines(PENDING).stream().map(String::strip)
                .filter(line -> !line.isEmpty() && !line.startsWith("#")).collect(Collectors.toCollection(TreeSet::new));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void whatTheEnumsNameAndACharacterLacksIsOnTheBacklogAndNothingElseIs() {
        Set<String> lacking = new TreeSet<>();
        made().forEach((character, animations) -> asked(character).stream().filter(name -> !animations.contains(name))
            .forEach(name -> lacking.add(character + " " + name)));
        Set<String> pending = pending();

        Set<String> unlisted = new TreeSet<>(lacking);
        unlisted.removeAll(pending);
        Set<String> stale = new TreeSet<>(pending);
        stale.removeAll(lacking);

        assertEquals(Set.of(), unlisted, "meant by the code for this character, not in its file and not on the backlog: add the animation or the line");
        assertEquals(Set.of(), stale, "on the backlog, though the animation exists or the character cannot reach it: take the line off");
    }

    @Test
    void theBacklogNamesCharactersWeHave() {
        Set<String> characters = made().keySet();

        pending().forEach(line -> assertTrue(characters.contains(line.split(" ")[0]), line + " is no character"));
    }
}
