package com.dwinovo.chiikawa.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.anim.state.PetAction;
import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.anim.state.PetReaction;
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
 * Animations ship as named candidates with fallbacks, so a pet whose model lacks the first does the next; what the
 * code names and the files do not have yet is the animators' backlog, and it is written down in
 * {@code pending_animations.txt}, one line for each character that lacks one. The list is held to the files both ways:
 * a name that is lacking and not on the list fails, so a new candidate cannot be added without the backlog saying so,
 * and a line for an animation that has been made, or that nothing names any more, fails too, so the list shrinks to
 * empty as the animations are made.
 *
 * <p>The names read are those of the enums, {@link PetActivity}, {@link PetAction} and {@link PetReaction}. The moves
 * the scenes between pets name are data that needs the entity registry to read, which a test outside a game has not.
 */
class AnimationBacklogTest {
    private static final Path ANIMATIONS = Path.of("src", "main", "resources", "assets", "chiikawa", "animations");
    private static final Path PENDING = Path.of("src", "test", "resources", "pending_animations.txt");

    /** @return the animations the enums name, by any pet that is in the activity, makes the move or has the reaction */
    private static Set<String> named() {
        Set<String> names = new TreeSet<>();
        Arrays.stream(PetActivity.values()).forEach(activity -> names.addAll(activity.animationCandidates()));
        Arrays.stream(PetAction.values()).forEach(action -> names.addAll(action.animationCandidates()));
        Arrays.stream(PetReaction.values()).forEach(reaction -> names.addAll(reaction.animationCandidates()));
        return names;
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
        made().forEach((character, animations) -> named().stream().filter(name -> !animations.contains(name))
            .forEach(name -> lacking.add(character + " " + name)));
        Set<String> pending = pending();

        Set<String> unlisted = new TreeSet<>(lacking);
        unlisted.removeAll(pending);
        Set<String> stale = new TreeSet<>(pending);
        stale.removeAll(lacking);

        assertEquals(Set.of(), unlisted, "named by the code, not in the character's file and not on the backlog: add the animation or the line");
        assertEquals(Set.of(), stale, "on the backlog, though the animation exists or nothing names it: take the line off");
    }

    @Test
    void theBacklogNamesCharactersWeHave() {
        Set<String> characters = made().keySet();

        pending().forEach(line -> assertTrue(characters.contains(line.split(" ")[0]), line + " is no character"));
    }
}
