package com.dwinovo.chiikawa.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.dwinovo.chiikawa.data.LanguageData;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.brain.errand.LackKey;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkActions;
import com.dwinovo.chiikawa.testing.MinecraftFont;
import com.dwinovo.chiikawa.ui.widget.Segments;
import com.dwinovo.chiikawa.ui.widget.TabStrip;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Every word drawn on the pet screen fits the room the screen draws it in, in every language, measured with the game's own
 * font: the banner's name, job and licence, the speech bubble, the tabs, the order and stance controls, and the two rows of
 * the settings page. A word that does not fit is shortened in its language and not cut with an ellipsis, which a player
 * reads as a word nobody finished. The rooms are {@link PetScreenLayout}'s. What waits under the cursor is not here: that has
 * the room of the screen.
 */
class PetScreenLabelsTest {
    private static final String PET = "screen.chiikawa.pet.";

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void theTabsAreNamedAndStillStandClearOfThePet() {
        List<String> tooWide = new ArrayList<>();
        for (String locale : LanguageData.LOCALES) {
            Map<String, String> words = collect(locale);
            int strip = TabStrip.stripWidth(MinecraftFont::width, List.of(get(words, PET + "tab.backpack"), get(words, PET + "tab.settings")));
            if (strip > PetScreenLayout.TABS_ROOM) {
                tooWide.add(locale + " tabs are " + strip + " wide, the room is " + PetScreenLayout.TABS_ROOM);
            }
        }
        assertEquals(List.of(), tooWide);
    }

    @Test
    void theThreeOrdersFitTheirControl() {
        List<String> tooWide = new ArrayList<>();
        for (String locale : LanguageData.LOCALES) {
            Map<String, String> words = collect(locale);
            List<String> names = new ArrayList<>();
            for (PetDirective order : PetDirective.values()) {
                names.add(get(words, PET + "order." + order.name().toLowerCase(Locale.ROOT)));
            }
            int width = Segments.minWidth(MinecraftFont::width, names);
            if (width > PetScreenLayout.ORDERS_ROOM) {
                tooWide.add(locale + " orders " + names + " need " + width + ", the control is " + PetScreenLayout.ORDERS_ROOM);
            }
        }
        assertEquals(List.of(), tooWide);
    }

    @Test
    void pickingUpHasATitleAndALineThatFitItsRow() {
        List<String> tooWide = new ArrayList<>();
        for (String locale : LanguageData.LOCALES) {
            Map<String, String> words = collect(locale);
            fits(tooWide, locale, PET + "pick_up", get(words, PET + "pick_up"), PetScreenLayout.SWITCH_ROW_ROOM);
            fits(tooWide, locale, PET + "pick_up.hint", get(words, PET + "pick_up.hint"), PetScreenLayout.SWITCH_ROW_ROOM);
        }
        assertEquals(List.of(), tooWide);
    }

    @Test
    void everyPetsNameEveryJobAndEveryGradeFitTheirChips() {
        List<String> tooWide = new ArrayList<>();
        for (String locale : LanguageData.LOCALES) {
            Map<String, String> words = collect(locale);
            Map<String, String> names = withPrefix(words, "entity.chiikawa.");
            assertFalse(names.isEmpty(), "no pet has a name in " + locale);
            names.forEach((key, text) -> fits(tooWide, locale, key, text, PetScreenLayout.NAME_ROOM));
            Map<String, String> jobs = withPrefix(words, "tooltip.chiikawa.pet_job.");
            assertFalse(jobs.isEmpty(), "no job has a name in " + locale);
            jobs.forEach((key, text) -> fits(tooWide, locale, key, text, PetScreenLayout.JOB_ROOM));
            // Grades go up to ten, which is as wide as a grade gets.
            fits(tooWide, locale, PET + "licence.rank", String.format(Locale.ROOT, get(words, PET + "licence.rank"), 10),
                PetScreenLayout.RANK_ROOM);
            fits(tooWide, locale, PET + "licence.none", get(words, PET + "licence.none"), PetScreenLayout.RANK_ROOM);
        }
        assertEquals(List.of(), tooWide);
    }

    @Test
    void theBadgeForAPresentFitsItsRoom() {
        List<String> tooWide = new ArrayList<>();
        for (String locale : LanguageData.LOCALES) {
            fits(tooWide, locale, PET + "gift_badge", get(collect(locale), PET + "gift_badge"), PetScreenLayout.GIFT_ROOM);
        }
        assertEquals(List.of(), tooWide);
    }

    @Test
    void whatAPetIsAtFitsTheBubbleBesideTheSlipsBarAndCount() {
        List<String> tooWide = new ArrayList<>();
        for (String locale : LanguageData.LOCALES) {
            Map<String, String> words = collect(locale);
            Map<String, String> doing = new TreeMap<>(withPrefix(words, "intent.chiikawa.", "intent.chiikawa.fail."));
            doing.putAll(withPrefix(words, "pet_task.chiikawa.", ".amount"));
            doing.put(PET + "doing.nothing", get(words, PET + "doing.nothing"));
            assertFalse(doing.isEmpty(), "nothing a pet is at has a name in " + locale);
            doing.forEach((key, text) -> fits(tooWide, locale, key, text, PetScreenLayout.ACTIVITY_ROOM));
        }
        assertEquals(List.of(), tooWide);
    }

    @Test
    void whatAPetIsShortOfIsSaidBesideItsPictureEvenWithAPresentBesideTheBubble() {
        List<LackKey> lacks = WorkActions.all().stream().flatMap(action -> action.lacks().stream()).toList();
        assertFalse(lacks.isEmpty(), "no action names what a pet can be short of");
        List<String> tooWide = new ArrayList<>();
        for (String locale : LanguageData.LOCALES) {
            Map<String, String> words = collect(locale);
            String needs = get(words, PET + "outlook.lacks");
            for (LackKey lack : lacks) {
                String name = words.get(lack.translationKey());
                if (name == null) {
                    tooWide.add(locale + " has no words for " + lack.translationKey());
                } else {
                    fits(tooWide, locale, lack.translationKey(), String.format(Locale.ROOT, needs, name), PetScreenLayout.REASON_ROOM);
                }
            }
        }
        assertEquals(List.of(), tooWide);
    }

    /** Notes what is too wide for its room, so that one run shows all of it. */
    private static void fits(List<String> tooWide, String locale, String key, String text, int room) {
        int width = MinecraftFont.width(text);
        if (width > room) {
            tooWide.add(locale + " " + key + " \"" + text + "\" is " + width + " wide, the room is " + room);
        }
    }

    private static String get(Map<String, String> words, String key) {
        String text = words.get(key);
        if (text == null) {
            throw new AssertionError("no " + key);
        }
        return text;
    }

    /** @return the translations whose key starts with the prefix, and does not start with or end in any of the others */
    private static Map<String, String> withPrefix(Map<String, String> words, String prefix, String... except) {
        Map<String, String> found = new TreeMap<>();
        words.forEach((key, text) -> {
            if (key.startsWith(prefix) && java.util.Arrays.stream(except).noneMatch(other -> key.startsWith(other) || key.endsWith(other))) {
                found.put(key, text);
            }
        });
        return found;
    }

    private static Map<String, String> collect(String locale) {
        Map<String, String> map = new HashMap<>();
        LanguageData.addTranslations(locale, map::put);
        return map;
    }
}
