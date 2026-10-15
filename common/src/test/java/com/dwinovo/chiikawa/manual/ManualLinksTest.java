package com.dwinovo.chiikawa.manual;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ManualLinksTest {
    @Test
    void anyChineseGetsTheChinesePages() {
        assertEquals("zh", ManualLinks.folder("zh_cn"));
        assertEquals("zh", ManualLinks.folder("zh_tw"));
        assertEquals("zh", ManualLinks.folder("zh_hk"));
        assertEquals("zh", ManualLinks.folder("ZH_CN"));
    }

    @Test
    void japaneseGetsJapanesePortugueseGetsPortugueseAndAnythingElseEnglish() {
        assertEquals("ja", ManualLinks.folder("ja_jp"));
        assertEquals("en", ManualLinks.folder("en_us"));
        assertEquals("en", ManualLinks.folder("fr_fr"));
        assertEquals("pt", ManualLinks.folder("pt_br"));
        assertEquals("pt", ManualLinks.folder("pt_pt"));
        assertEquals("en", ManualLinks.folder("ko_kr"), "the site has no Korean");
        assertEquals("en", ManualLinks.folder(""));
    }

    @Test
    void theAddressIsTheSiteTheLanguageAndTheChaptersPage() {
        assertEquals("https://chiikawa.dwinovo.cn/zh/play/work.html", ManualLinks.url("zh_cn", "play/work"));
        assertEquals("https://chiikawa.dwinovo.cn/ja/play/camera.html", ManualLinks.url("ja_jp", "play/camera"));
        assertEquals("https://chiikawa.dwinovo.cn/en/play/taming.html", ManualLinks.url("en_us", "play/taming"));
        assertEquals("https://chiikawa.dwinovo.cn/pt/play/shop.html", ManualLinks.url("pt_br", "play/shop"));
        assertEquals("https://chiikawa.dwinovo.cn/en/play/shop.html", ManualLinks.url("ko_kr", "play/shop"));
    }
}
