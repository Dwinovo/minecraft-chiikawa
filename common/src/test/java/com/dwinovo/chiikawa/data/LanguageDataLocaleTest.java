package com.dwinovo.chiikawa.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Every language says everything English says, and fills in the same blanks: a line may
 * put the pet, the price and the thing in its own order, but may not drop one.
 */
class LanguageDataLocaleTest {
    private static final Pattern ARGUMENT = Pattern.compile("%(?:(\\d+)\\$)?s");

    @Test
    void everyLanguageHasEnglishsKeys() {
        Set<String> english = collect("en_us").keySet();
        for (String locale : LanguageData.LOCALES) {
            assertEquals(new TreeSet<>(english), new TreeSet<>(collect(locale).keySet()), locale);
        }
    }

    @Test
    void everyLineFillsInEnglishsBlanks() {
        Map<String, String> english = collect("en_us");
        for (String locale : LanguageData.LOCALES) {
            Map<String, String> translations = collect(locale);
            english.forEach((key, line) -> assertEquals(arguments(line), arguments(translations.get(key)),
                () -> locale + " " + key));
        }
    }

    /** Which of the arguments a line uses, counting a bare {@code %s} as the next one. */
    private static Set<Integer> arguments(String line) {
        Set<Integer> used = new TreeSet<>();
        if (line == null) {
            return used;
        }
        int next = 1;
        Matcher matcher = ARGUMENT.matcher(line);
        while (matcher.find()) {
            used.add(matcher.group(1) != null ? Integer.parseInt(matcher.group(1)) : next++);
        }
        return used;
    }

    private static Map<String, String> collect(String locale) {
        Map<String, String> map = new HashMap<>();
        LanguageData.addTranslations(locale, map::put);
        return map;
    }
}
