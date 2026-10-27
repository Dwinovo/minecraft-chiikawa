package com.dwinovo.chiikawa.testing;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The animations the characters have, by the name of their files, and the backlog of the ones they have not yet, as
 * {@code pending_animations.txt} writes it: what the tests that hold the code's animation names to the files read.
 */
public final class AnimationFiles {
    private static final Path ANIMATIONS = Path.of("src", "main", "resources", "assets", "chiikawa", "animations");
    private static final Path PENDING = Path.of("src", "test", "resources", "pending_animations.txt");

    private AnimationFiles() {
    }

    /** @return the animations each character has, by the name of its file */
    public static Map<String, Set<String>> made() {
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
    public static Set<String> pending() {
        try {
            return Files.readAllLines(PENDING).stream().map(String::strip)
                .filter(line -> !line.isEmpty() && !line.startsWith("#")).collect(Collectors.toCollection(TreeSet::new));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
