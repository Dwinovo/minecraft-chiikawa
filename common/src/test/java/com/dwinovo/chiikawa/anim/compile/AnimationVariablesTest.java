package com.dwinovo.chiikawa.anim.compile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dwinovo.chiikawa.anim.molang.MolangContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The pets' animations ask Molang only for what the renderer feeds it. A variable it does
 * not know is read as 0 with a warning at every load: one left over from the tool a file was
 * made in, such as Yes Steve Model's {@code ysm.head_yaw}, belongs written as what it is.
 */
class AnimationVariablesTest {
    private static final Path ANIMATIONS = Path.of("src/main/resources/assets/chiikawa/animations");
    /** A name with a dot in it that is not a number: a variable, or a function under {@code math.}. */
    private static final Pattern NAME = Pattern.compile("[A-Za-z_][A-Za-z_0-9]*[.][A-Za-z_][A-Za-z_0-9.]*");

    @Test
    void everyVariableAnAnimationReadsIsOneTheRendererFeeds() throws IOException {
        Set<String> unknown = new TreeSet<>();
        for (Path file : files()) {
            try (Reader reader = Files.newBufferedReader(file)) {
                collect(JsonParser.parseReader(reader), file.getFileName().toString(), unknown);
            }
        }
        assertEquals(Set.of(), unknown, "animations read variables nothing sets");
    }

    private static void collect(JsonElement element, String file, Set<String> unknown) {
        if (element.isJsonObject()) {
            element.getAsJsonObject().entrySet().forEach(entry -> collect(entry.getValue(), file, unknown));
        } else if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collect(child, file, unknown));
        } else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            Matcher name = NAME.matcher(element.getAsString());
            while (name.find()) {
                String found = name.group();
                if (!found.toLowerCase().startsWith("math.") && MolangContext.resolveSlot(found) < 0) {
                    unknown.add(file + ": " + found);
                }
            }
        }
    }

    private static List<Path> files() throws IOException {
        try (Stream<Path> files = Files.list(ANIMATIONS)) {
            return files.filter(file -> file.toString().endsWith(".json")).sorted().toList();
        }
    }
}
