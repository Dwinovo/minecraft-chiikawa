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
            addHandbookTranslations(adder, "吉伊的打工手册", "右键翻开，看大家怎么过日子", "这本手册还是空白的",
                "上一页", "下一页", "哇！", "耶！", "好吃！", "给你！", "呜……");
            addHandbookPage(adder, "meet", "遇见吉伊",
                "平原、草原、沙漠和雪地里，会遇到拿着工具闲逛的吉伊们。",
                "拿吃的喂它就能驯服，它会跟着你走。",
                "蹲下右键换指令：跟着你、坐下等你，或者自由活动。",
                "右键打开它的界面，背包、状态和指令都在里面。");
            addHandbookPage(adder, "work", "去打工",
                "放一块劳动公告板，每天早上它会贴出几张工作牌。",
                "拿着工具的吉伊会自己去领牌：锄头去除草，剑去讨伐。",
                "领了牌就去干活，牌上写着要干多少。",
                "干完了！工钱直接进它的背包，主人随时可以拿。");
            addHandbookPage(adder, "shop", "去商店",
                "放一个商店。宠物攒够了钱，会自己去买喜欢的东西。",
                "它按自己的口味挑：吉伊最爱零食。",
                "你也能在柜台卖作物、买东西，价钱写在按钮上。",
                "拿着钱右键它，就是给它零花钱。");
            addHandbookPage(adder, "presents", "送礼物",
                "宠物有时会在商店给你挑一份礼物……",
                "……揣着它来找你……",
                "……亲手交给你！",
                "它买了什么、送了什么，聊天框里都会说一声。");
            addHandbookPage(adder, "supplies", "背包和用品",
                "背上双肩包，背包多出 10 格。",
                "小熊、鲸鱼、星星挎包，谁都能背。",
                "喂一份简单料理，一阵子干活更快、更有干劲。",
                "用名字牌给它起个名字。");
            addHandbookPage(adder, "upgrade", "升级公告板",
                "花钱给公告板升级，每天多贴几张牌。",
                "升到二级后会贴出讨伐牌。",
                "拿剑的上前砍，拿弓的在远处射。",
                "讨伐报酬最高，但倒下就算失败。");
            addHandbookPage(adder, "safety", "倒下与召回",
                "宠物倒下会变成玩偶，身上的东西都在里面。",
                "把玩偶放到蛋糕上，它就带着东西回来了。",
                "打不过时它会先退到你身边，回了血再上。",
                "摇一摇宠物铃铛，你的宠物都会回来，别的维度也听得见。");
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
                "把 MP3 / WAV 文件放入文件夹后点击刷新", "部分歌曲导入失败", "请使用 MP3 格式的音频", "打开文件夹", "刷新", "上一页", "下一页");
            addIntentFailureTranslations(adder, "无法跟随主人", "主人就在附近", "已经回到主人身边", "目标不在范围内",
                "目标离开了活动范围", "附近没有可拾取的物品", "没有可收割的作物", "没有可种植的耕地", "没有可存放的容器",
                "没有攻击目标", "攻击冷却中", "没有箭", "没有新的曲子可演奏", "曲子演奏完了", "先做更要紧的活",
                "附近没有野草", "附近没有蘑菇", "要领到对应的工作牌才会做", "只在夜里做",
                "已经带着一张工作牌", "刚才没领到牌，歇一会儿", "附近的公告板没有合适的工作牌",
                "刚买过东西", "商店里没有它想要又买得起的", "手上没有要送的东西");
            addBlockTranslations(adder, "劳动公告板", "商店");
            addSupplyTranslations(adder, "双肩包", "小熊挎包", "鲸鱼挎包", "星星挎包", "给宠物背上，背包多 10 格",
                    "简单料理", "喂给宠物，一阵子干活更起劲",
                    "宠物铃铛", "叫回你的宠物，跨维度也听得见；响过一次要等半分钟");
            addPetNewsTranslations(adder, "%1$s 花 %2$s 个%4$s买了 %3$s",
                    "%1$s 花 %2$s 个%4$s买了 %3$s，当场吃掉了",
                    "%1$s 花 %2$s 个%4$s买了 %3$s，说要送给你",
                    "%1$s 把 %2$s 送给了你");
            addRecallTranslations(adder, "叫回了 %s 只宠物", "你没有宠物可叫",
                    "%s 没有应声，上次见到它的地方已经没它了");
            addShopScreenTranslations(adder, "商店", "买 %s", "卖 %s", "今天什么都不卖",
                    "上一页", "下一页");
            addIntentNameTranslations(adder, "跟着主人", "坐着", "闲逛", "捡东西", "去领工作牌",
                "收割", "种地", "送货", "拔草", "采蘑菇", "讨伐", "射箭", "演奏", "去买东西", "送礼物");
            addTaskTypeTranslations(adder, "除草", "%s 株", "街头演奏", "%s 秒",
                "近身讨伐", "远程讨伐", "%s 只");
            addBoardScreenTranslations(adder, "劳动公告板", "%s 张", "今天没有工作牌",
                    "限%s", "待领", "已领", "%s 领走了",
                    "Lv.%s", "每天 %s 张", "升级 %s",
                    "升到 Lv.%1$s：每天 %2$s 张", "解锁：%s",
                    "已升满", "你有 %1$s 个%2$s");
            addPetStatusTranslations(adder, "闲着", "背上双肩包或挎包，可以多装 10 格");
            addPetScreenTranslations(adder, "背包", "状态", "指令",
                    "没有领工作牌",
                    "干劲十足：还剩 %s",
                    "零花钱：%1$s 个%2$s",
                    "准备送给你的礼物",
                    "它会自己走过来递给你",
                    "跟在你身边，有怪就上",
                    "待在原地不动",
                    "在家附近干活、逛街、领牌子");
        } else {
            addCommonTranslations(adder, "Chiikawa", "Pet Backpack", "Follow", "Sit", "Free Roam");
            addDollTooltipTranslations(adder, "Try placing the doll on a cake?");
            addHandbookTranslations(adder, "Chiikawa's Work Handbook", "Right-click to see how everyone gets by",
                "This handbook is still blank", "Previous page", "Next page", "Wah!", "Yay!", "Yum!", "For you!", "Ow...");
            addHandbookPage(adder, "meet", "Meeting Them",
                "Out on the plains and in the snow, they wander about with a tool.",
                "Feed one something tasty to tame it, and it will follow you.",
                "Sneak and right-click to switch: follow, sit and wait, or roam free.",
                "Right-click to open its screen: its bag, how it is, and orders.");
            addHandbookPage(adder, "work", "Off to Work",
                "Put up a labor board. Every morning it puts up a few work slips.",
                "A pet with a tool takes a slip itself: a hoe weeds, a sword hunts.",
                "Slip in hand, off it goes to work. The slip says how much.",
                "Done! The pay goes straight into its bag, yours to take any time.");
            addHandbookPage(adder, "shop", "The Shop",
                "Put up a shop. A pet with money saved goes and buys what it likes.",
                "It picks by its own taste: Chiikawa loves snacks.",
                "You can sell crops and buy things at the counter too.",
                "Right-click it holding money to give it pocket money.");
            addHandbookPage(adder, "presents", "Presents",
                "Now and then a pet picks out a present for you...",
                "...carries it all the way to you...",
                "...and hands it over itself!",
                "What it bought and what it gave, it tells you in chat.");
            addHandbookPage(adder, "supplies", "Bags and Treats",
                "A rucksack on its back gives it ten more slots.",
                "The bear, whale and star pouches fit anyone.",
                "A simple dish makes it quicker and keener for a while.",
                "Give it a name with a name tag.");
            addHandbookPage(adder, "upgrade", "A Better Board",
                "Pay to upgrade a board, and it puts up more slips a day.",
                "From level two it puts up hunting slips.",
                "A sword goes in close; a bow shoots from afar.",
                "Hunting pays best, but a pet that falls loses the slip.");
            addHandbookPage(adder, "safety", "Falling and Coming Back",
                "A pet that falls leaves a doll with all its things inside.",
                "Put the doll on a cake, and back it comes, things and all.",
                "When a fight goes badly it backs off to you, then goes back in.",
                "Ring the pet bell and your pets come back, even from other worlds.");
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
                "Put MP3 / WAV files in the folder, then reload", "Some songs failed to import", "Please use MP3 audio files", "Open Folder", "Reload", "Previous page", "Next page");
            addIntentFailureTranslations(adder, "Can't follow the owner", "Owner is close by", "Back with the owner", "Target is out of range",
                "Target left the allowed area", "No item to pick up", "No crop to harvest", "No farmland to plant", "No container to deliver to",
                "No target to attack", "Attack is cooling down", "No arrows", "No new song to play", "The song is over",
                "Something more urgent comes first", "No weeds nearby", "No mushrooms nearby",
                "Only with a matching slip", "Only done at night",
                "Already carries a slip", "Resting after missing a slip", "No slip for it on a nearby board",
                "Just bought something", "Nothing here it wants and can afford", "Nothing to give");
            addBlockTranslations(adder, "Labor Board", "Shop");
            addSupplyTranslations(adder, "Rucksack", "Bear Pouch", "Whale Pouch", "Star Pouch", "Worn by a pet: ten more slots",
                    "Simple Dish", "Fed to a pet: keener on work for a while",
                    "Pet Bell", "Calls your pets home, even from another dimension; half a minute between rings");
            addPetNewsTranslations(adder, "%1$s spent %2$s × %4$s on %3$s",
                    "%1$s spent %2$s × %4$s on %3$s and ate it on the spot",
                    "%1$s spent %2$s × %4$s on %3$s, and says it is for you",
                    "%1$s gave you %2$s");
            addRecallTranslations(adder, "%s came running", "You have no pets to call",
                    "%s did not answer; it is gone from where it was last seen");
            addShopScreenTranslations(adder, "Shop", "Buy %s", "Sell %s", "Nothing for sale today",
                    "Previous page", "Next page");
            addIntentNameTranslations(adder, "Following its owner", "Sitting", "Wandering", "Picking up an item",
                "Fetching a slip", "Harvesting", "Planting", "Delivering", "Pulling weeds", "Picking mushrooms",
                "Fighting", "Shooting", "Performing", "Out shopping", "Bringing a present");
            addTaskTypeTranslations(adder, "Weeding", "%s weeds", "Street Performance", "%ss",
                "Monster Hunting", "Monster Shooting", "%s slain");
            addBoardScreenTranslations(adder, "Labor Board", "%s up", "No slips up today",
                    "%s only", "Open", "Taken", "Taken by %s",
                    "Lv.%s", "%s a day", "Upgrade %s",
                    "At Lv.%1$s: %2$s a day", "New work: %s",
                    "Fully upgraded", "You have %1$s × %2$s");
            addPetStatusTranslations(adder, "Idle", "A rucksack or a pouch adds ten more slots");
            addPetScreenTranslations(adder, "Backpack", "Status", "Orders",
                    "No slip taken",
                    "Keen on work: %s left",
                    "Pocket money: %1$s × %2$s",
                    "A present for you",
                    "It will bring it over itself",
                    "Stays by you and steps in when something attacks",
                    "Stays right where it is",
                    "Works, shops and takes slips around home");
        }
    }

    private static void addCommonTranslations(Adder adder, String tabName, String backpackMenu, String follow, String stay, String free) {
        adder.add("itemGroup.chiikawa", tabName);
        adder.add("menu.chiikawa.pet_backpack", backpackMenu);
        adder.add("screen.chiikawa.pet.order.follow", follow);
        adder.add("screen.chiikawa.pet.order.stay", stay);
        adder.add("screen.chiikawa.pet.order.free", free);
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
        String noSlip,
        String shopResting,
        String nothingToBuy,
        String noGift
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
        adder.add("intent.chiikawa.fail.shop_resting", shopResting);
        adder.add("intent.chiikawa.fail.nothing_to_buy", nothingToBuy);
        adder.add("intent.chiikawa.fail.no_gift", noGift);
    }

    /**
     * The shop screen. The prices go on the buttons — "buy 3" is what pressing it does —
     * so the words are a verb and a number and nothing else.
     */
    private static void addShopScreenTranslations(Adder adder, String title, String buy, String sell,
            String empty, String previousPage, String nextPage) {
        adder.add("screen.chiikawa.shop", title);
        adder.add("screen.chiikawa.shop.buy", buy);
        adder.add("screen.chiikawa.shop.sell", sell);
        adder.add("screen.chiikawa.shop.empty", empty);
        adder.add("screen.chiikawa.shop.previous_page", previousPage);
        adder.add("screen.chiikawa.shop.next_page", nextPage);
    }

    private static void addSupplyTranslations(Adder adder, String backpack, String bearPouch, String whalePouch,
            String starPouch, String bagTip, String simpleDish, String simpleDishTip, String petBell, String petBellTip) {
        adder.add("item.chiikawa.backpack", backpack);
        adder.add("item.chiikawa.bear_pouch", bearPouch);
        adder.add("item.chiikawa.whale_pouch", whalePouch);
        adder.add("item.chiikawa.star_pouch", starPouch);
        adder.add("tooltip.chiikawa.bag", bagTip);
        adder.add("item.chiikawa.simple_dish", simpleDish);
        adder.add("tooltip.chiikawa.simple_dish", simpleDishTip);
        adder.add("item.chiikawa.pet_bell", petBell);
        adder.add("tooltip.chiikawa.pet_bell", petBellTip);
    }

    /** What a rung bell tells its owner. */
    private static void addRecallTranslations(Adder adder, String came, String nobody, String missing) {
        adder.add("message.chiikawa.pet_bell.came", came);
        adder.add("message.chiikawa.pet_bell.nobody", nobody);
        adder.add("message.chiikawa.pet_bell.missing", missing);
    }

    /**
     * What a pet tells its owner in chat about its own spending: the pet, the price, the
     * thing; and the present it hands over. Numbered, since the languages put them in
     * different orders.
     */
    private static void addPetNewsTranslations(Adder adder, String bought, String boughtAte, String boughtGift,
            String given) {
        adder.add("message.chiikawa.shop.bought", bought);
        adder.add("message.chiikawa.shop.bought_ate", boughtAte);
        adder.add("message.chiikawa.shop.bought_gift", boughtGift);
        adder.add("message.chiikawa.gift.given", given);
    }

    private static void addBlockTranslations(Adder adder, String laborBoard, String shop) {
        adder.add("block.chiikawa.labor_board", laborBoard);
        adder.add("block.chiikawa.shop", shop);
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
        String playMusic,
        String shop,
        String giftOwner
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
        adder.add("intent.chiikawa.shop", shop);
        adder.add("intent.chiikawa.gift_owner", giftOwner);
    }

    /** Names of the slip types a labor board puts up, each with the unit it counts in. */
    private static void addTaskTypeTranslations(
        Adder adder,
        String weeding,
        String weedingAmount,
        String streetPerformance,
        String streetPerformanceAmount,
        String meleeHunting,
        String rangedHunting,
        String huntingAmount
    ) {
        adder.add("pet_task.chiikawa.weeding", weeding);
        adder.add("pet_task.chiikawa.weeding.amount", weedingAmount);
        adder.add("pet_task.chiikawa.street_performance", streetPerformance);
        adder.add("pet_task.chiikawa.street_performance.amount", streetPerformanceAmount);
        adder.add("pet_task.chiikawa.melee_hunting", meleeHunting);
        adder.add("pet_task.chiikawa.melee_hunting.amount", huntingAmount);
        adder.add("pet_task.chiikawa.ranged_hunting", rangedHunting);
        adder.add("pet_task.chiikawa.ranged_hunting.amount", huntingAmount);
    }

    /**
     * The board's screen. {@code open} and {@code taken} are badges on a row and have to
     * stay to a word or two; {@code takenBy} is the sentence under the cursor, where there
     * is room to say who.
     *
     * @param count how many slips are up, in the title bar
     * @param forJob which job may take a slip, beside its name
     * @param daily what a level buys, said in slips a day rather than in levels
     * @param upgrade the button that buys the next level, with its price on it
     */
    private static void addBoardScreenTranslations(Adder adder, String title, String count, String empty,
            String forJob, String open, String taken, String takenBy,
            String level, String daily, String upgrade, String upgradeHint, String unlocks, String maxLevel,
            String purse) {
        adder.add("screen.chiikawa.labor_board", title);
        adder.add("screen.chiikawa.labor_board.count", count);
        adder.add("screen.chiikawa.labor_board.empty", empty);
        adder.add("screen.chiikawa.labor_board.for_job", forJob);
        adder.add("screen.chiikawa.labor_board.open", open);
        adder.add("screen.chiikawa.labor_board.taken", taken);
        adder.add("screen.chiikawa.labor_board.taken_by", takenBy);
        adder.add("screen.chiikawa.labor_board.level", level);
        adder.add("screen.chiikawa.labor_board.daily", daily);
        adder.add("screen.chiikawa.labor_board.upgrade", upgrade);
        adder.add("screen.chiikawa.labor_board.upgrade_hint", upgradeHint);
        adder.add("screen.chiikawa.labor_board.unlocks", unlocks);
        adder.add("screen.chiikawa.labor_board.max_level", maxLevel);
        adder.add("screen.chiikawa.labor_board.purse", purse);
    }

    /**
     * What a pet is at. Only the idle case needs words of its own: a pet that is doing
     * something is named by the intent it is running, and a slip by its own type, both of
     * which already have names. How far along it is comes out as a bar and a count, which
     * read the same in every language.
     */
    /**
     * The pet screen's pages. The tab names are what the tabs say under the cursor; the
     * order hints are the one line under each order, so they say what the pet will do
     * rather than what the word means.
     */
    private static void addPetScreenTranslations(Adder adder, String backpack, String status, String orders,
            String noSlip, String eager, String money, String gift, String giftHint,
            String followHint, String stayHint, String freeHint) {
        adder.add("screen.chiikawa.pet.tab.backpack", backpack);
        adder.add("screen.chiikawa.pet.tab.status", status);
        adder.add("screen.chiikawa.pet.tab.orders", orders);
        adder.add("screen.chiikawa.pet.no_slip", noSlip);
        adder.add("screen.chiikawa.pet.eager", eager);
        adder.add("screen.chiikawa.pet.money", money);
        adder.add("screen.chiikawa.pet.gift", gift);
        adder.add("screen.chiikawa.pet.gift_hint", giftHint);
        adder.add("screen.chiikawa.pet.order.follow.hint", followHint);
        adder.add("screen.chiikawa.pet.order.stay.hint", stayHint);
        adder.add("screen.chiikawa.pet.order.free.hint", freeHint);
    }

    private static void addPetStatusTranslations(Adder adder, String doingNothing, String bagHint) {
        adder.add("screen.chiikawa.pet.doing.nothing", doingNothing);
        adder.add("screen.chiikawa.pet.bag_hint", bagHint);
    }

    private static void addHandbookTranslations(Adder adder, String name, String tip, String empty,
            String previousPage, String nextPage, String wa, String yay, String yum, String present, String ouch) {
        adder.add("item.chiikawa.handbook", name);
        adder.add("tooltip.chiikawa.handbook", tip);
        adder.add("screen.chiikawa.handbook.empty", empty);
        adder.add("screen.chiikawa.handbook.previous_page", previousPage);
        adder.add("screen.chiikawa.handbook.next_page", nextPage);
        adder.add("manual.chiikawa.say.wa", wa);
        adder.add("manual.chiikawa.say.yay", yay);
        adder.add("manual.chiikawa.say.yum", yum);
        adder.add("manual.chiikawa.say.present", present);
        adder.add("manual.chiikawa.say.ouch", ouch);
    }

    /** A handbook page's title and the lines under its panels, under the keys its page names. */
    private static void addHandbookPage(Adder adder, String page, String title, String... captions) {
        adder.add(ManualData.titleKey(page), title);
        for (int i = 0; i < captions.length; i++) {
            adder.add(ManualData.captionKey(page, i + 1), captions[i]);
        }
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
        String reload,
        String previousPage,
        String nextPage
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
        // Read out rather than written on the buttons: the arrows say it plainly enough.
        adder.add("screen.chiikawa.music_box.previous_page", previousPage);
        adder.add("screen.chiikawa.music_box.next_page", nextPage);
    }
}
