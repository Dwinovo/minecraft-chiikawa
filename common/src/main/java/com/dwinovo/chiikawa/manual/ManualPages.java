package com.dwinovo.chiikawa.manual;

import java.util.List;

/**
 * The handbook's chapters (pages of the book) as the resource packs have them now, in reading order. Client side;
 * replaced whole on every resource reload.
 */
public final class ManualPages {
    private static volatile List<ManualPage> pages = List.of();

    private ManualPages() {
    }

    /** @return every chapter, in reading order */
    public static List<ManualPage> all() {
        return pages;
    }

    static void replaceAll(List<ManualPage> loaded) {
        pages = List.copyOf(loaded);
    }
}
