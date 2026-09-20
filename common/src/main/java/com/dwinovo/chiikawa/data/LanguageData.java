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
            addCommonTranslations(adder, "Chiikawa", "背包", "跟随", "坐下", "自由活动");
            addDollTooltipTranslations(adder, "试着把玩偶放在蛋糕上？");
            addJobTranslations(adder, "职业", "无", "农夫", "剑士", "弓箭手", "音乐家", "未知");
            addEntityTranslations(adder, "乌萨奇", "小八", "吉伊", "狮萨", "飞鼠", "栗子馒头", "獭师父", "古本屋");
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
                "古本屋刷怪蛋",
                "乌萨奇玩偶",
                "小八玩偶",
                "吉伊玩偶",
                "狮萨玩偶",
                "飞鼠玩偶",
                "栗子馒头玩偶",
                "獭师父玩偶",
                "古本屋玩偶",
                "乌萨奇的讨伐棒",
                "小八的讨伐棒",
                "吉伊的讨伐棒"
            );
            addMusicBoxTranslations(adder, "八音盒", "未选择歌曲", "歌曲：%s", "选择歌曲", "导入中", "导入失败", "没有可播放的歌曲",
                "把 MP3 / WAV 文件放入文件夹后点击刷新", "部分歌曲导入失败", "请使用 MP3 格式的音频", "打开文件夹", "刷新");
            addIntentFailureTranslations(adder, "无法跟随主人", "主人就在附近", "已经回到主人身边", "目标不在范围内",
                "目标离开了活动范围", "附近没有可拾取的物品", "没有可收割的作物", "没有可种植的耕地", "没有可存放的容器",
                "没有攻击目标", "攻击冷却中", "没有箭", "没有新的曲子可演奏", "曲子演奏完了", "先做更要紧的活",
                "附近没有野草", "附近没有蘑菇", "要领到对应的工作牌才会做", "只在夜里做",
                "已经带着一张工作牌", "刚才没领到牌，歇一会儿", "附近的公告板没有合适的工作牌");
            addBlockTranslations(adder, "劳动公告板");
            addIntentNameTranslations(adder, "跟着主人", "坐着", "闲逛", "捡东西", "去领工作牌",
                "收割", "种地", "送货", "拔草", "采蘑菇", "讨伐", "射箭", "演奏");
            addTaskTypeTranslations(adder, "除草", "夜间采蘑菇", "街头演奏");
            addBoardScreenTranslations(adder, "劳动公告板", "今天没有工作牌", "%s · %s 个 · %s",
                "还没人领", "%s 领走了");
            addPetStatusTranslations(adder, "正在：%s", "闲着", "工作牌：%s %s/%s", "没有工作牌");
        } else {
            addCommonTranslations(adder, "Chiikawa", "Pet Backpack", "Follow", "Sit", "Free Roam");
            addDollTooltipTranslations(adder, "Try placing the doll on a cake?");
            addJobTranslations(adder, "Job", "None", "Farmer", "Fencer", "Archer", "Musician", "Unknown");
            addEntityTranslations(adder, "Usagi", "Hachiware", "Chiikawa", "Shisa", "Momonga", "Kurimanju", "Rakko", "Furuhonya");
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
                "Furuhonya Spawn Egg",
                "Usagi Doll",
                "Hachiware Doll",
                "Chiikawa Doll",
                "Shisa Doll",
                "Momonga Doll",
                "Kurimanju Doll",
                "Rakko Doll",
                "Furuhonya Doll",
                "Usagi Weapon",
                "Hachiware Weapon",
                "Chiikawa Weapon"
            );
            addMusicBoxTranslations(adder, "Music Box", "No song selected", "Song: %s", "Choose Song", "Importing", "Failed", "No playable songs",
                "Put MP3 / WAV files in the folder, then reload", "Some songs failed to import", "Please use MP3 audio files", "Open Folder", "Reload");
            addIntentFailureTranslations(adder, "Can't follow the owner", "Owner is close by", "Back with the owner", "Target is out of range",
                "Target left the allowed area", "No item to pick up", "No crop to harvest", "No farmland to plant", "No container to deliver to",
                "No target to attack", "Attack is cooling down", "No arrows", "No new song to play", "The song is over",
                "Something more urgent comes first", "No weeds nearby", "No mushrooms nearby",
                "Only with a matching slip", "Only done at night",
                "Already carries a slip", "Resting after missing a slip", "No slip for it on a nearby board");
            addBlockTranslations(adder, "Labor Board");
            addIntentNameTranslations(adder, "Following its owner", "Sitting", "Wandering", "Picking up an item",
                "Fetching a slip", "Harvesting", "Planting", "Delivering", "Pulling weeds", "Picking mushrooms",
                "Fighting", "Shooting", "Performing");
            addTaskTypeTranslations(adder, "Weeding", "Mushroom Picking", "Street Performance");
            addBoardScreenTranslations(adder, "Labor Board", "No slips up today", "%s · %s · %s",
                "Still up", "Taken by %s");
            addPetStatusTranslations(adder, "Doing: %s", "Idle", "Slip: %s %s/%s", "No slip");
        }
    }

    private static void addCommonTranslations(Adder adder, String tabName, String backpackMenu, String follow, String stay, String free) {
        adder.add("itemGroup.chiikawa", tabName);
        adder.add("menu.chiikawa.pet_backpack", backpackMenu);
        adder.add("message.chiikawa.pet_follow", follow + ": %s");
        adder.add("message.chiikawa.pet_stay", stay + ": %s");
        adder.add("message.chiikawa.pet_free", free + ": %s");
    }

    private static void addIntentFailureTranslations(
        Adder adder,
        String ownerUnavailable,
        String ownerNearby,
        String ownerReached,
        String outOfReach,
        String outOfLeash,
        String noItem,
        String noCrop,
        String noFarmland,
        String noContainer,
        String noAttackTarget,
        String attackCoolingDown,
        String noArrows,
        String noNewSong,
        String songOver,
        String higherPriority,
        String noWeed,
        String noMushroom,
        String needsSlip,
        String notNight,
        String hasSlip,
        String boardResting,
        String noSlip
    ) {
        adder.add("intent.chiikawa.fail.owner_unavailable", ownerUnavailable);
        adder.add("intent.chiikawa.fail.owner_nearby", ownerNearby);
        adder.add("intent.chiikawa.fail.owner_reached", ownerReached);
        adder.add("intent.chiikawa.fail.out_of_reach", outOfReach);
        adder.add("intent.chiikawa.fail.out_of_leash", outOfLeash);
        adder.add("intent.chiikawa.fail.no_item", noItem);
        adder.add("intent.chiikawa.fail.no_crop", noCrop);
        adder.add("intent.chiikawa.fail.no_farmland", noFarmland);
        adder.add("intent.chiikawa.fail.no_container", noContainer);
        adder.add("intent.chiikawa.fail.no_attack_target", noAttackTarget);
        adder.add("intent.chiikawa.fail.attack_cooling_down", attackCoolingDown);
        adder.add("intent.chiikawa.fail.no_arrows", noArrows);
        adder.add("intent.chiikawa.fail.no_new_song", noNewSong);
        adder.add("intent.chiikawa.fail.song_over", songOver);
        adder.add("intent.chiikawa.fail.higher_priority", higherPriority);
        adder.add("intent.chiikawa.fail.no_weed", noWeed);
        adder.add("intent.chiikawa.fail.no_mushroom", noMushroom);
        adder.add("intent.chiikawa.fail.needs_slip", needsSlip);
        adder.add("intent.chiikawa.fail.not_night", notNight);
        adder.add("intent.chiikawa.fail.has_slip", hasSlip);
        adder.add("intent.chiikawa.fail.board_resting", boardResting);
        adder.add("intent.chiikawa.fail.no_slip", noSlip);
    }

    private static void addBlockTranslations(Adder adder, String laborBoard) {
        adder.add("block.chiikawa.labor_board", laborBoard);
    }

    /** Names of what a pet may be doing, shown in its backpack; one per intent. */
    private static void addIntentNameTranslations(
        Adder adder,
        String followOwner,
        String stay,
        String wander,
        String pickUpItem,
        String takeTask,
        String harvest,
        String plant,
        String deliver,
        String weed,
        String pickMushroom,
        String melee,
        String ranged,
        String playMusic
    ) {
        adder.add("intent.chiikawa.follow_owner", followOwner);
        adder.add("intent.chiikawa.stay", stay);
        adder.add("intent.chiikawa.wander", wander);
        adder.add("intent.chiikawa.pick_up_item", pickUpItem);
        adder.add("intent.chiikawa.take_task", takeTask);
        adder.add("intent.chiikawa.harvest", harvest);
        adder.add("intent.chiikawa.plant", plant);
        adder.add("intent.chiikawa.deliver", deliver);
        adder.add("intent.chiikawa.weed", weed);
        adder.add("intent.chiikawa.pick_mushroom", pickMushroom);
        adder.add("intent.chiikawa.melee", melee);
        adder.add("intent.chiikawa.ranged", ranged);
        adder.add("intent.chiikawa.play_music", playMusic);
    }

    /** Names of the slip types a labor board puts up. */
    private static void addTaskTypeTranslations(Adder adder, String weeding, String mushroomPicking, String streetPerformance) {
        adder.add("pet_task.chiikawa.weeding", weeding);
        adder.add("pet_task.chiikawa.mushroom_picking", mushroomPicking);
        adder.add("pet_task.chiikawa.street_performance", streetPerformance);
    }

    private static void addBoardScreenTranslations(Adder adder, String title, String empty, String slip,
            String open, String taken) {
        adder.add("screen.chiikawa.labor_board", title);
        adder.add("screen.chiikawa.labor_board.empty", empty);
        adder.add("screen.chiikawa.labor_board.slip", slip);
        adder.add("screen.chiikawa.labor_board.open", open);
        adder.add("screen.chiikawa.labor_board.taken", taken);
    }

    private static void addPetStatusTranslations(Adder adder, String doing, String doingNothing, String slip, String noSlip) {
        adder.add("screen.chiikawa.pet.doing", doing);
        adder.add("screen.chiikawa.pet.doing.nothing", doingNothing);
        adder.add("screen.chiikawa.pet.slip", slip);
        adder.add("screen.chiikawa.pet.slip.none", noSlip);
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
        String shisa, String momonga, String kurimanju, String rakko, String furuhonya) {
        adder.add("entity.chiikawa.usagi", usagi);
        adder.add("entity.chiikawa.hachiware", hachiware);
        adder.add("entity.chiikawa.chiikawa", chiikawa);
        adder.add("entity.chiikawa.shisa", shisa);
        adder.add("entity.chiikawa.momonga", momonga);
        adder.add("entity.chiikawa.kurimanju", kurimanju);
        adder.add("entity.chiikawa.rakko", rakko);
        adder.add("entity.chiikawa.furuhonya", furuhonya);
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
        String shisaEgg, String momongaEgg, String kurimanjuEgg, String rakkoEgg, String furuhonyaEgg,
        String usagiDoll, String hachiwareDoll, String chiikawaDoll,
        String shisaDoll, String momongaDoll, String kurimanjuDoll, String rakkoDoll, String furuhonyaDoll,
        String usagiWeapon, String hachiwareWeapon, String chiikawaWeapon) {
        adder.add("item.chiikawa.usagi_spawn_egg", usagiEgg);
        adder.add("item.chiikawa.hachiware_spawn_egg", hachiwareEgg);
        adder.add("item.chiikawa.chiikawa_spawn_egg", chiikawaEgg);
        adder.add("item.chiikawa.shisa_spawn_egg", shisaEgg);
        adder.add("item.chiikawa.momonga_spawn_egg", momongaEgg);
        adder.add("item.chiikawa.kurimanju_spawn_egg", kurimanjuEgg);
        adder.add("item.chiikawa.rakko_spawn_egg", rakkoEgg);
        adder.add("item.chiikawa.furuhonya_spawn_egg", furuhonyaEgg);
        adder.add("item.chiikawa.usagi_doll", usagiDoll);
        adder.add("item.chiikawa.hachiware_doll", hachiwareDoll);
        adder.add("item.chiikawa.chiikawa_doll", chiikawaDoll);
        adder.add("item.chiikawa.shisa_doll", shisaDoll);
        adder.add("item.chiikawa.momonga_doll", momongaDoll);
        adder.add("item.chiikawa.kurimanju_doll", kurimanjuDoll);
        adder.add("item.chiikawa.rakko_doll", rakkoDoll);
        adder.add("item.chiikawa.furuhonya_doll", furuhonyaDoll);
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
        String emptyHint,
        String importFailed,
        String formatHint,
        String openFolder,
        String reload
    ) {
        adder.add("item.chiikawa.music_box", itemName);
        adder.add("tooltip.chiikawa.music_box.empty", emptyTooltip);
        adder.add("tooltip.chiikawa.music_box.selected", selectedTooltip);
        adder.add("screen.chiikawa.music_box.title", title);
        adder.add("screen.chiikawa.music_box.importing", importing);
        adder.add("screen.chiikawa.music_box.failed", failed);
        adder.add("screen.chiikawa.music_box.empty_catalog", emptyCatalog);
        adder.add("screen.chiikawa.music_box.empty_hint", emptyHint);
        adder.add("screen.chiikawa.music_box.import_failed", importFailed);
        adder.add("screen.chiikawa.music_box.format_hint", formatHint);
        adder.add("screen.chiikawa.music_box.open_folder", openFolder);
        adder.add("screen.chiikawa.music_box.reload", reload);
    }
}
