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
            addDollTooltipTranslations(adder, "试着把玩偶放在蛋糕上？");
            addJobTranslations(adder, "职业", "无", "农夫", "剑士", "弓箭手", "音乐家", "未知");
            addEntityTranslations(adder, "乌萨奇", "小八", "吉伊", "狮萨", "飞鼠", "栗子馒头", "獭师父");
            addItemTagTranslations(adder, "农夫工具", "剑士工具", "弓箭手工具", "音乐家工具", "驯服食物", "种植作物", "运送物品", "可拾取物品");
            addItemTranslations(
                adder,
                "乌萨奇刷怪蛋",
                "小八刷怪蛋",
                "吉伊刷怪蛋",
                "狮萨刷怪蛋",
                "飞鼠刷怪蛋",
                "栗子馒头刷怪蛋",
                "獭师父刷怪蛋",
                "乌萨奇玩偶",
                "小八玩偶",
                "吉伊玩偶",
                "狮萨玩偶",
                "飞鼠玩偶",
                "栗子馒头玩偶",
                "獭师父玩偶",
                "乌萨奇的讨伐棒",
                "小八的讨伐棒",
                "吉伊的讨伐棒"
            );
            addMusicBoxTranslations(adder, "八音盒", "未选择歌曲", "歌曲：%s", "选择歌曲", "导入中", "导入失败", "没有可播放的歌曲",
                "缺少 FFmpeg，歌曲无法导入", "请手动安装后点击刷新", "刷新");
        } else {
            addCommonTranslations(adder, "Chiikawa", "Pet Backpack", "Follow", "Sit", "Work");
            addDollTooltipTranslations(adder, "Try placing the doll on a cake?");
            addJobTranslations(adder, "Job", "None", "Farmer", "Fencer", "Archer", "Musician", "Unknown");
            addEntityTranslations(adder, "Usagi", "Hachiware", "Chiikawa", "Shisa", "Momonga", "Kurimanju", "Rakko");
            addItemTagTranslations(adder, "Farmer Tools", "Fencer Tools", "Archer Tools", "Musician Tools", "Tame Foods", "Plant Crops", "Deliver Items", "Pickable Items");
            addItemTranslations(
                adder,
                "Usagi Spawn Egg",
                "Hachiware Spawn Egg",
                "Chiikawa Spawn Egg",
                "Shisa Spawn Egg",
                "Momonga Spawn Egg",
                "Kurimanju Spawn Egg",
                "Rakko Spawn Egg",
                "Usagi Doll",
                "Hachiware Doll",
                "Chiikawa Doll",
                "Shisa Doll",
                "Momonga Doll",
                "Kurimanju Doll",
                "Rakko Doll",
                "Usagi Weapon",
                "Hachiware Weapon",
                "Chiikawa Weapon"
            );
            addMusicBoxTranslations(adder, "Music Box", "No song selected", "Song: %s", "Choose Song", "Importing", "Failed", "No playable songs",
                "FFmpeg is missing, songs cannot be imported", "Install it manually, then reload", "Reload");
        }
    }

    private static void addCommonTranslations(Adder adder, String tabName, String backpackMenu, String follow, String sit, String work) {
        adder.add("itemGroup.chiikawa", tabName);
        adder.add("menu.chiikawa.pet_backpack", backpackMenu);
        adder.add("message.chiikawa.pet_follow", follow + ": %s");
        adder.add("message.chiikawa.pet_sit", sit + ": %s");
        adder.add("message.chiikawa.pet_work", work + ": %s");
    }

    private static void addDollTooltipTranslations(Adder adder, String placeOnCakeHint) {
        adder.add("tooltip.chiikawa.doll.place_on_cake", placeOnCakeHint);
    }

    private static void addJobTranslations(
        Adder adder,
        String jobLabel,
        String none,
        String farmer,
        String fencer,
        String archer,
        String musician,
        String unknown
    ) {
        adder.add("tooltip.chiikawa.pet_job", jobLabel + ": %s");
        adder.add("tooltip.chiikawa.pet_job.none", none);
        adder.add("tooltip.chiikawa.pet_job.farmer", farmer);
        adder.add("tooltip.chiikawa.pet_job.fencer", fencer);
        adder.add("tooltip.chiikawa.pet_job.archer", archer);
        adder.add("tooltip.chiikawa.pet_job.musician", musician);
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

    private static void addItemTagTranslations(
        Adder adder,
        String farmerTools,
        String fencerTools,
        String archerTools,
        String musicianTools,
        String tameFoods,
        String plantCrops,
        String deliverItems,
        String pickableItems
    ) {
        adder.add("tag.item.chiikawa.entity_farmer_tools", farmerTools);
        adder.add("tag.item.chiikawa.entity_fencer_tools", fencerTools);
        adder.add("tag.item.chiikawa.entity_archer_tools", archerTools);
        adder.add("tag.item.chiikawa.entity_musician_tools", musicianTools);
        adder.add("tag.item.chiikawa.entity_tame_foods", tameFoods);
        adder.add("tag.item.chiikawa.entity_plant_crops", plantCrops);
        adder.add("tag.item.chiikawa.entity_deliver_items", deliverItems);
        adder.add("tag.item.chiikawa.entity_pickable_items", pickableItems);
    }

    private static void addItemTranslations(Adder adder, String usagiEgg, String hachiwareEgg, String chiikawaEgg,
        String shisaEgg, String momongaEgg, String kurimanjuEgg, String rakkoEgg,
        String usagiDoll, String hachiwareDoll, String chiikawaDoll,
        String shisaDoll, String momongaDoll, String kurimanjuDoll, String rakkoDoll,
        String usagiWeapon, String hachiwareWeapon, String chiikawaWeapon) {
        adder.add("item.chiikawa.usagi_spawn_egg", usagiEgg);
        adder.add("item.chiikawa.hachiware_spawn_egg", hachiwareEgg);
        adder.add("item.chiikawa.chiikawa_spawn_egg", chiikawaEgg);
        adder.add("item.chiikawa.shisa_spawn_egg", shisaEgg);
        adder.add("item.chiikawa.momonga_spawn_egg", momongaEgg);
        adder.add("item.chiikawa.kurimanju_spawn_egg", kurimanjuEgg);
        adder.add("item.chiikawa.rakko_spawn_egg", rakkoEgg);
        adder.add("item.chiikawa.usagi_doll", usagiDoll);
        adder.add("item.chiikawa.hachiware_doll", hachiwareDoll);
        adder.add("item.chiikawa.chiikawa_doll", chiikawaDoll);
        adder.add("item.chiikawa.shisa_doll", shisaDoll);
        adder.add("item.chiikawa.momonga_doll", momongaDoll);
        adder.add("item.chiikawa.kurimanju_doll", kurimanjuDoll);
        adder.add("item.chiikawa.rakko_doll", rakkoDoll);
        adder.add("item.chiikawa.usagi_weapon", usagiWeapon);
        adder.add("item.chiikawa.hachiware_weapon", hachiwareWeapon);
        adder.add("item.chiikawa.chiikawa_weapon", chiikawaWeapon);
    }

    private static void addMusicBoxTranslations(
        Adder adder,
        String itemName,
        String emptyTooltip,
        String selectedTooltip,
        String title,
        String importing,
        String failed,
        String emptyCatalog,
        String ffmpegMissing,
        String ffmpegHint,
        String reload
    ) {
        adder.add("item.chiikawa.music_box", itemName);
        adder.add("tooltip.chiikawa.music_box.empty", emptyTooltip);
        adder.add("tooltip.chiikawa.music_box.selected", selectedTooltip);
        adder.add("screen.chiikawa.music_box.title", title);
        adder.add("screen.chiikawa.music_box.importing", importing);
        adder.add("screen.chiikawa.music_box.failed", failed);
        adder.add("screen.chiikawa.music_box.empty_catalog", emptyCatalog);
        adder.add("screen.chiikawa.music_box.ffmpeg_missing", ffmpegMissing);
        adder.add("screen.chiikawa.music_box.ffmpeg_hint", ffmpegHint);
        adder.add("screen.chiikawa.music_box.reload", reload);
    }
}
