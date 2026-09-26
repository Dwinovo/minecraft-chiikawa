package com.dwinovo.chiikawa.qualification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.util.Map;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class QualificationTest {
    private static final ResourceLocation WEEDING = ResourceLocation.fromNamespaceAndPath("chiikawa", "weeding");

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void aLicenceFileSaysEverythingAboutItsExams() {
        Qualification weeding = parse(file("[0.4, 0.3, 0.22, 0.15, 0.08]", "[70, 18, 8, 3, 1, 0]")).getOrThrow();

        assertEquals(5, weeding.grades());
        assertEquals(7, weeding.everyDays());
        assertEquals(WEEDING, weeding.practiceTask());
        assertEquals(0.08F, weeding.basePass().get(4), 1.0E-6F);
        assertEquals(Items.BOOK, weeding.book());
        assertEquals(480, (int) weeding.writeTicks().minInclusive());
    }

    @Test
    void theOddsAndTheWildWeightsMatchTheGrades() {
        assertTrue(parse(file("[0.4, 0.3]", "[70, 18, 8, 3, 1, 0]")).isError(), "two chances for five grades");
        assertTrue(parse(file("[0.4, 0.3, 0.22, 0.15, 0.08]", "[70, 18]")).isError(), "two weights for six ranks");
        assertTrue(parse(file("[0.4, 0.3, 0.22, 0.15, 0.08]", "[0, 0, 0, 0, 0, 0]")).isError(), "every rank weighs nothing");
        assertTrue(parse(file("[0.4, 0.3, 0.22, 0.15, 1.5]", "[70, 18, 8, 3, 1, 0]")).isError(), "a chance over 1");
    }

    /** The series counts the grades down: the first one passed is grade 5, the best grade 1. */
    @Test
    void theFirstGradePassedIsGradeFiveAndTheLastGradeOne() {
        Qualification weeding = parse(file("[0.4, 0.3, 0.22, 0.15, 0.08]", "[70, 18, 8, 3, 1, 0]")).getOrThrow();

        assertEquals(5, weeding.rank(1));
        assertEquals(1, weeding.rank(5));
    }

    @Test
    void theLoaderSkipsWhatDoesNotParseAndWarnsAboutPractiseNoPackHas() {
        Map<ResourceLocation, JsonElement> files = Map.of(
            WEEDING, file("[0.4, 0.3, 0.22, 0.15, 0.08]", "[70, 18, 8, 3, 1, 0]"),
            ResourceLocation.fromNamespaceAndPath("chiikawa", "broken"), JsonParser.parseString("{}"));

        QualificationLoader.Loaded known = QualificationLoader.load(files, Set.of(WEEDING)::contains);
        assertEquals(Set.of(WEEDING), known.qualifications().keySet());
        assertEquals(1, known.errors().size());
        assertTrue(known.warnings().isEmpty());

        QualificationLoader.Loaded unknown = QualificationLoader.load(files, task -> false);
        assertEquals(1, unknown.warnings().size());
    }

    private static DataResult<Qualification> parse(JsonElement json) {
        return Qualification.CODEC.parse(JsonOps.INSTANCE, json);
    }

    private static JsonElement file(String basePass, String wildGrades) {
        return JsonParser.parseString("""
            {
              "grades": 5,
              "every_days": 7,
              "practice_task": "chiikawa:weeding",
              "required_practice": 1,
              "base_pass": %s,
              "practice_per_slip": 0.03,
              "practice_cap": 0.3,
              "fail_bonus": 0.05,
              "fail_cap": 0.2,
              "book": "minecraft:book",
              "book_bonus": 0.25,
              "max_pass": 0.95,
              "write_ticks": [480, 560],
              "wild_grades": %s
            }
            """.formatted(basePass, wildGrades));
    }
}
