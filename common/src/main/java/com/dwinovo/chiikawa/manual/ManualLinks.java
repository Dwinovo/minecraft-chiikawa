package com.dwinovo.chiikawa.manual;

import java.util.Locale;

/**
 * Where a handbook chapter goes on the website, in the player's language: the one place the
 * address is put together.
 */
public final class ManualLinks {
    public static final String SITE = "https://chiikawa.dwinovo.cn";

    private ManualLinks() {
    }

    /**
     * @param languageCode the client's language, such as {@code zh_cn} or {@code ja_jp}
     * @return the website's language folder for it: Chinese for any Chinese, Japanese, Portuguese for
     *         any Portuguese, else English
     */
    public static String folder(String languageCode) {
        String code = languageCode.toLowerCase(Locale.ROOT);
        if (code.startsWith("zh")) {
            return "zh";
        }
        if (code.startsWith("ja")) {
            return "ja";
        }
        return code.startsWith("pt") ? "pt" : "en";
    }

    /**
     * @param languageCode the client's language
     * @param path the chapter's page, such as {@code play/work}
     */
    public static String url(String languageCode, String path) {
        return SITE + "/" + folder(languageCode) + "/" + path + ".html";
    }
}
