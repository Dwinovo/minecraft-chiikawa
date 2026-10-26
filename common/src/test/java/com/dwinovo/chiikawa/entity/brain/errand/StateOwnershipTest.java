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
 * that do must be the owners listed here, exactly: a file that starts writing is caught, and so is an
 * owner that no longer writes, which has to be taken off the list. A write outside the owners fails the
 * test, whatever it is for.
 */
class StateOwnershipTest {
    private static final Path SOURCE = Path.of("src", "main", "java");
    private static final String ROOT = "com/dwinovo/chiikawa/";

    private static final String PET = "entity/AbstractPet.java";
    private static final String MIND = "entity/brain/PetMind.java";
    private static final String EXECUTOR = "entity/brain/errand/run/Executor.java";
    private static final String STEPS = "entity/brain/errand/step/";
    private static final String HANDLE = STEPS + "LiveHandle.java";
    private static final String RESTS = "entity/brain/errand/rest/RestStore.java";
    private static final String SENSORS = "entity/brain/sensor/";

    /**
     * One kind of write, and who does it.
     *
     * @param what what is written
     * @param call the calls that write it
     * @param owners the files that are meant to
     */
    private record Rule(String what, Pattern call, Set<String> owners) {
    }

    private static final String WRITE = "(?:setMemory|setMemoryWithExpiry|eraseMemory)\\(\\s*";

    private static final List<Rule> RULES = List.of(
        new Rule("the pose", Pattern.compile("\\bsetActivity\\("),
            Set.of(PET)),
        new Rule("the scene", Pattern.compile("\\bsetPerformance\\("),
            Set.of(PET)),
        new Rule("the errand shown", Pattern.compile("\\bsetIntent\\(|\\.intent\\(|entityData\\.set\\(INTENT\\b"),
            Set.of(PET, EXECUTOR)),
        new Rule("the walk, the gaze and the path",
            Pattern.compile(WRITE + "MemoryModuleType\\.(?:WALK_TARGET|LOOK_TARGET|PATH)\\b"
                + "|\\bsetWalkAndLookTargetMemories\\(|\\blookAt(?:Entity|EachOther|Entities)\\("),
            Set.of(HANDLE)),
        new Rule("a rest",
            Pattern.compile(WRITE + "InitMemory\\.(?:PHOTO_COOLDOWN|GIFT_COOLDOWN|SHOP_COOLDOWN|TAKE_TASK_COOLDOWN"
                + "|MUSICIAN_RESTING|SOCIAL_COOLDOWNS|ERRAND_RESTS|SUBJECT_RESTS)\\b"),
            Set.of(RESTS)),
        new Rule("the scene a pet has thought of",
            Pattern.compile(WRITE + "InitMemory\\.INTERACTION_PLAN\\b"),
            Set.of(SENSORS + "PetSocialSensor.java", STEPS + "Meeting.java")),
        new Rule("another pet's brain, health, mood or bag: a pet tells another pet and never does it to it",
            Pattern.compile("\\b(?:partner|other|friend|initiator|visitor|asker|guest|invitee)\\.(?:getBrain\\(\\)\\s*\\.\\s*"
                + "(?:setMemory|setMemoryWithExpiry|eraseMemory)|heal|setHealth|hurt|feedDish|setPetDirective|setItemSlot"
                + "|getBackpack\\(\\)\\s*\\.\\s*(?:setItem|addItem|removeItem|removeItemType|clearContent))\\("),
            Set.of()),
        new Rule("what the musician has played",
            Pattern.compile(WRITE + "InitMemory\\.(?:MUSICIAN_LAST_MUSIC_SIGNATURE|MUSICIAN_NOW_PLAYING)\\b"),
            Set.of(STEPS + "Busk.java")),
        new Rule("the bow in use", Pattern.compile("\\b(?:pet|entity)\\.(?:start|stop)UsingItem\\("),
            Set.of(HANDLE)),
        new Rule("the attack cooldown", Pattern.compile(WRITE + "MemoryModuleType\\.ATTACK_COOLING_DOWN\\b"),
            Set.of(HANDLE)),
        new Rule("the foe the sensor names", Pattern.compile(WRITE + "MemoryModuleType\\.ATTACK_TARGET\\b"),
            Set.of(SENSORS + "PetAttackbleEntitySensor.java")),
        new Rule("the whistle a wild pet has heard",
            Pattern.compile(WRITE + "(?:InitMemory\\.WHISTLE_HEARD|WhistleHeard\\.type\\(\\))"),
            Set.of(MIND)),
        new Rule("what the pet is told to do in general: follow, stay or be free",
            Pattern.compile("\\bsetPetDirective\\(|entityData\\.set\\(PET_MODE\\b"),
            // The owner's own hand (the screen, the crouch, the payload, the handbook's demonstration) and the order they
            // whistled, which the pet's mind takes once the pet's wait is over.
            Set.of(PET, MIND, "entity/interact/PetInteractHandler.java", "network/PetServerPacketHandler.java",
                "client/manual/ManualScene.java")),
        new Rule("the blocks of work the sensor sees",
            Pattern.compile(WRITE + "InitMemory\\.WORK_SIGHTS\\b"),
            Set.of(SENSORS + "PetWorkSensor.java")),
        // Retired in 0.1.4 and kept registered only so that the saves that hold them still load: nobody writes them.
        new Rule("the blocks of work the farmer saw before the work sensor",
            Pattern.compile(WRITE + "InitMemory\\.(?:HARVEST_POS|PLANT_POS|CONTAINER_POS|WEED_POS|MUSHROOM_POS)\\b"),
            Set.of()),
        new Rule("the present in the paws", Pattern.compile("\\bsetPendingGift\\("),
            Set.of(PET, STEPS + "TakePresent.java", STEPS + "HandOver.java")));

    /** What an errand, which composes steps, may not reach for: the pet's brain, its random, its memories. */
    private static final Pattern NOT_FOR_ERRANDS = Pattern.compile("\\bgetBrain\\(\\)|\\bgetRandom\\(\\)|\\bInitMemory\\b|\\bsetMemory\\b");
    private static final List<String> ERRANDS = List.of("entity/brain/errand/impl/", "entity/brain/errand/work/");

    @Test
    void eachPieceOfStateIsWrittenByItsOwnerOnly() {
        Map<String, String> code = sources();
        for (Rule rule : RULES) {
            Set<String> writers = filesMatching(code, rule.call());
            Set<String> others = new TreeSet<>(writers);
            others.removeAll(rule.owners());
            assertEquals(Set.of(), others,
                "who writes " + rule.what() + " besides " + rule.owners() + ": a new writer is a second source of it");
            assertTrue(writers.containsAll(rule.owners()),
                "the owner of " + rule.what() + " no longer writes it: " + rule.owners());
        }
    }

    @Test
    void anErrandComposesStepsAndReachesIntoNothing() {
        Set<String> reaching = filesMatching(sources(), NOT_FOR_ERRANDS).stream()
            .filter(file -> ERRANDS.stream().anyMatch(file::startsWith)).collect(Collectors.toCollection(TreeSet::new));

        assertEquals(Set.of(), reaching, "an errand reaches for the brain, the random or the memories: only steps do");
    }

    @Test
    void theOwnersAreFilesThatExist() {
        Map<String, String> code = sources();
        RULES.stream().flatMap(rule -> rule.owners().stream())
            .forEach(file -> assertTrue(code.containsKey(file), file + " is not a source file"));
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
