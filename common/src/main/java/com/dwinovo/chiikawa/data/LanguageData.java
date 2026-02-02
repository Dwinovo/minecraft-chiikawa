package com.dwinovo.chiikawa.data;

public final class LanguageData {
    private LanguageData() {
    }

    @FunctionalInterface
    public interface Adder {
        void add(String key, String value);
    }

    public static void addTranslations(String locale, Adder adder) {
        if ("zh_cn".equals(locale)) {
            addCommonTranslations(adder, "Chiikawa", "背包", "跟随", "坐下", "工作");
            addJobTranslations(adder, "职业", "无", "农夫", "剑士", "弓箭手", "未知");
            addEntityTranslations(adder, "乌萨奇", "小八", "吉伊", "狮萨", "飞鼠", "栗子馒头", "獭师父");
            addItemTranslations(
                adder,
                "乌萨奇刷怪蛋",
                "小八刷怪蛋",
                "吉伊刷怪蛋",
                "狮萨刷怪蛋",
                "飞鼠刷怪蛋",
                "栗子馒头刷怪蛋",
                "獭师父刷怪蛋",
                "乌萨奇的讨伐棒",
                "小八的讨伐棒",
                "吉伊的讨伐棒"
            );
        } else {
            addCommonTranslations(adder, "Chiikawa", "Pet Backpack", "Follow", "Sit", "Work");
            addJobTranslations(adder, "Job", "None", "Farmer", "Fencer", "Archer", "Unknown");
            addEntityTranslations(adder, "Usagi", "Hachiware", "Chiikawa", "Shisa", "Momonga", "Kurimanju", "Rakko");
            addItemTranslations(
                adder,
                "Usagi Spawn Egg",
                "Hachiware Spawn Egg",
                "Chiikawa Spawn Egg",
                "Shisa Spawn Egg",
                "Momonga Spawn Egg",
                "Kurimanju Spawn Egg",
                "Rakko Spawn Egg",
                "Usagi Weapon",
                "Hachiware Weapon",
                "Chiikawa Weapon"
            );
        }
    }

    private static void addCommonTranslations(Adder adder, String tabName, String backpackMenu, String follow, String sit, String work) {
        adder.add("itemGroup.chiikawa", tabName);
        adder.add("menu.chiikawa.pet_backpack", backpackMenu);
        adder.add("message.chiikawa.pet_follow", follow + ": %s");
        adder.add("message.chiikawa.pet_sit", sit + ": %s");
        adder.add("message.chiikawa.pet_work", work + ": %s");
    }

    private static void addJobTranslations(Adder adder, String jobLabel, String none, String farmer, String fencer, String archer, String unknown) {
        adder.add("tooltip.chiikawa.pet_job", jobLabel + ": %s");
        adder.add("tooltip.chiikawa.pet_job.none", none);
        adder.add("tooltip.chiikawa.pet_job.farmer", farmer);
        adder.add("tooltip.chiikawa.pet_job.fencer", fencer);
        adder.add("tooltip.chiikawa.pet_job.archer", archer);
        adder.add("tooltip.chiikawa.pet_job.unknown", unknown);
    }

    private static void addEntityTranslations(Adder adder, String usagi, String hachiware, String chiikawa,
        String shisa, String momonga, String kurimanju, String rakko) {
        adder.add("entity.chiikawa.usagi", usagi);
        adder.add("entity.chiikawa.hachiware", hachiware);
        adder.add("entity.chiikawa.chiikawa", chiikawa);
        adder.add("entity.chiikawa.shisa", shisa);
        adder.add("entity.chiikawa.momonga", momonga);
        adder.add("entity.chiikawa.kurimanju", kurimanju);
        adder.add("entity.chiikawa.rakko", rakko);
    }

    private static void addItemTranslations(Adder adder, String usagiEgg, String hachiwareEgg, String chiikawaEgg,
        String shisaEgg, String momongaEgg, String kurimanjuEgg, String rakkoEgg,
        String usagiWeapon, String hachiwareWeapon, String chiikawaWeapon) {
        adder.add("item.chiikawa.usagi_spawn_egg", usagiEgg);
        adder.add("item.chiikawa.hachiware_spawn_egg", hachiwareEgg);
        adder.add("item.chiikawa.chiikawa_spawn_egg", chiikawaEgg);
        adder.add("item.chiikawa.shisa_spawn_egg", shisaEgg);
        adder.add("item.chiikawa.momonga_spawn_egg", momongaEgg);
        adder.add("item.chiikawa.kurimanju_spawn_egg", kurimanjuEgg);
        adder.add("item.chiikawa.rakko_spawn_egg", rakkoEgg);
        adder.add("item.chiikawa.usagi_weapon", usagiWeapon);
        adder.add("item.chiikawa.hachiware_weapon", hachiwareWeapon);
        adder.add("item.chiikawa.chiikawa_weapon", chiikawaWeapon);
    }
}
