package com.dwinovo.chiikawa.entity.brain.errand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Who may write what a pet is doing: one writer for each piece of state, so there are not two sources
 * of what a pet is up to. The source of the mod is scanned for the calls that write each, and the files
 * that do must be the owner and the legacy writers listed here, exactly: a file that starts writing is
 * caught, and so is a legacy entry that no longer writes, which has to be taken off the list. The legacy
 * writers are the behaviors the errands that are not yet native still use; each errand that goes native
 * takes its writers off, and the lists are empty when the last has.
 */
class StateOwnershipTest {
    private static final Path SOURCE = Path.of("src", "main", "java");
    private static final String ROOT = "com/dwinovo/chiikawa/";

    private static final String PET = "entity/AbstractPet.java";
    private static final String EXECUTOR = "entity/brain/errand/run/Executor.java";
    private static final String STEPS = "entity/brain/errand/step/";
    private static final String HANDLE = STEPS + "LiveHandle.java";
    private static final String RESTS = "entity/brain/errand/rest/RestStore.java";
    private static final String BEHAVIORS = "entity/brain/task/";
    private static final String SENSORS = "entity/brain/sensor/";

    /**
     * One kind of write, and who does it.
     *
     * @param what what is written
     * @param call the calls that write it
     * @param owners the files that are meant to
     * @param legacy the other files that still do, until the errands they belong to are native
     */
    private record Rule(String what, Pattern call, Set<String> owners, Set<String> legacy) {
    }

    private static final String WRITE = "(?:setMemory|setMemoryWithExpiry|eraseMemory)\\(\\s*";

    private static final List<Rule> RULES = List.of(
        new Rule("the pose", Pattern.compile("\\bsetActivity\\("),
            Set.of(PET),
            Set.of(BEHAVIORS + "musician/PlayMusicBehavior.java", "music/ServerMusicStreamManager.java")),
        new Rule("the scene", Pattern.compile("\\bsetPerformance\\("),
            Set.of(PET),
            Set.of("social/PartPlayer.java")),
        new Rule("the errand shown", Pattern.compile("\\bsetIntent\\(|\\.intent\\(|entityData\\.set\\(INTENT\\b"),
            Set.of(PET, EXECUTOR),
            Set.of()),
        new Rule("the walk, the gaze and the path",
            Pattern.compile(WRITE + "MemoryModuleType\\.(?:WALK_TARGET|LOOK_TARGET|PATH)\\b"
                + "|\\bsetWalkAndLookTargetMemories\\(|\\blookAt(?:Entity|EachOther|Entities)\\("),
            Set.of(HANDLE),
            Set.of(PET, BEHAVIORS + "combat/CombatMoveBehavior.java",
                BEHAVIORS + "idle/GlanceBehavior.java", BEHAVIORS + "musician/PlayMusicBehavior.java",
                BEHAVIORS + "social/CooperateBehavior.java", BEHAVIORS + "social/SocializeBehavior.java",
                BEHAVIORS + "tameable/AnchorLeashBehavior.java", BEHAVIORS + "tameable/AnswerWhistleBehavior.java",
                BEHAVIORS + "tameable/FollowOwnerBehavior.java", BEHAVIORS + "tameable/RandomWalkTask.java",
                BEHAVIORS + "tameable/SitBehavior.java")),
        new Rule("a rest",
            Pattern.compile(WRITE + "InitMemory\\.(?:PHOTO_COOLDOWN|GIFT_COOLDOWN|SHOP_COOLDOWN|TAKE_TASK_COOLDOWN"
                + "|MUSICIAN_RESTING|SOCIAL_COOLDOWNS|ERRAND_RESTS|SUBJECT_RESTS)\\b"),
            Set.of(RESTS),
            Set.of(BEHAVIORS + "musician/PlayMusicBehavior.java", "social/SocialRules.java")),
        new Rule("the blocks of work the farmer sees",
            Pattern.compile(WRITE + "(?:InitMemory\\.(?:HARVEST_POS|PLANT_POS|CONTAINER_POS|WEED_POS|MUSHROOM_POS)\\b|\\S*?\\.memory\\(\\))"),
            Set.of(SENSORS + "PetFarmerWorkSensor.java"),
            Set.of()),
        new Rule("the present in the paws", Pattern.compile("\\bsetPendingGift\\("),
            Set.of(PET, STEPS + "TakePresent.java", STEPS + "HandOver.java"),
            Set.of()));

    /** What an errand that is native, and so composes steps, may not reach for: the pet's brain, its random, its memories. */
    private static final Pattern NOT_FOR_ERRANDS = Pattern.compile("\\bgetBrain\\(\\)|\\bgetRandom\\(\\)|\\bInitMemory\\b|\\bsetMemory\\b");
    private static final String ERRANDS = "entity/brain/errand/impl/";
    /** The errands still carried out by an old activity, which reach into the pet as the behaviors did. */
    private static final Set<String> LEGACY_ERRANDS = Set.of(ERRANDS + "AnswerWhistleErrand.java",
        ERRANDS + "PlayMusicErrand.java");

    @Test
    void eachPieceOfStateIsWrittenByItsOwnerAndTheLegacyWritersOnly() {
        Map<String, String> code = sources();
        for (Rule rule : RULES) {
            Set<String> writers = filesMatching(code, rule.call());
            Set<String> others = new TreeSet<>(writers);
            others.removeAll(rule.owners());
            assertEquals(new TreeSet<>(rule.legacy()), others,
                "who writes " + rule.what() + " besides " + rule.owners() + ": a new writer is a second source of it, and a legacy"
                    + " entry that no longer writes has to come off the list");
            assertTrue(writers.containsAll(rule.owners()),
                "the owner of " + rule.what() + " no longer writes it: " + rule.owners());
        }
    }

    @Test
    void anErrandThatIsNativeComposesStepsAndReachesIntoNothing() {
        Set<String> reaching = filesMatching(sources(), NOT_FOR_ERRANDS).stream()
            .filter(file -> file.startsWith(ERRANDS)).collect(Collectors.toCollection(TreeSet::new));

        assertEquals(new TreeSet<>(LEGACY_ERRANDS), reaching,
            "the errands that reach for the brain, the random or the memories are the legacy ones, and only those");
    }

    @Test
    void theLegacyListsAreOfFilesThatExist() {
        Map<String, String> code = sources();
        Stream.concat(RULES.stream().flatMap(rule -> Stream.concat(rule.owners().stream(), rule.legacy().stream())),
            LEGACY_ERRANDS.stream()).forEach(file -> assertTrue(code.containsKey(file), file + " is not a source file"));
    }

    private static Set<String> filesMatching(Map<String, String> code, Pattern pattern) {
        return code.entrySet().stream().filter(file -> pattern.matcher(file.getValue()).find())
            .map(Map.Entry::getKey).collect(Collectors.toCollection(TreeSet::new));
    }

    /** @return the code of each source file, without its comments, by the path under the mod's package */
    private static Map<String, String> sources() {
        Path root = SOURCE.resolve(ROOT);
        assertTrue(Files.isDirectory(root), "the scan runs from the common module, not from " + Path.of("").toAbsolutePath());
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(file -> file.toString().endsWith(".java")).collect(Collectors.toMap(
                file -> root.relativize(file).toString().replace('\\', '/'), StateOwnershipTest::codeOf));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String codeOf(Path file) {
        try {
            return Files.readString(file).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
