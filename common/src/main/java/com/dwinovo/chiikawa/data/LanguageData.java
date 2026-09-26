package com.dwinovo.chiikawa.data;

import com.dwinovo.chiikawa.voice.VoiceMoment;
import java.util.List;

public final class LanguageData {
    /** The languages the mod is written in; English is also what the others are held to. */
    public static final List<String> LOCALES = List.of("en_us", "zh_cn", "ja_jp");

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
            addHandbookTranslations(adder, "吉伊的打工手册", "右键打开，查看宠物的各种玩法", "这本手册还是空白的",
                "上一页", "下一页");
            addHandbookPage(adder, "meet", "遇见吉伊",
                "平原、热带草原、沙漠、沼泽和雪原中会出现野生宠物，手上各拿着工具。",
                "手持面包、曲奇等食物右键野生宠物，每次有 30% 的几率驯服。",
                "潜行右键自己的宠物，在跟随、坐下、自由活动之间切换。",
                "直接右键宠物打开界面，可以管理背包、查看状态、切换指令。");
            addHandbookPage(adder, "work", "去打工",
                "放置劳动公告板后，每天日出时会贴出 3 张工作牌。",
                "宠物按主手的工具去领牌，例如拿锄头的领除草牌。",
                "领到牌后，宠物会去完成牌上写的工作量。",
                "报酬直接放进宠物的背包，主人打开界面就能取出。");
            addHandbookPage(adder, "shop", "去商店",
                "放置商店后，自由活动的宠物会用背包里的绿宝石去买东西。",
                "每个角色喜欢买的东西不同，吉伊最常买零食。",
                "右键柜台打开价目表，玩家也可以在这里买卖物品。",
                "手持绿宝石右键自己的宠物，可以给它零花钱。");
            addHandbookPage(adder, "presents", "送礼物",
                "宠物在商店购物时，有时会顺便给主人买一份礼物。",
                "买好后，它会拿着礼物走到主人身边。",
                "礼物直接放进主人的物品栏，物品栏满了就放在脚边。",
                "买礼物和送出礼物时，主人的聊天栏都会有提示。");
            addHandbookPage(adder, "supplies", "背包和用品",
                "给宠物装备双肩包，背包增加 10 格。",
                "小熊、鲸鱼、星星挎包同样增加 10 格，所有宠物都能装备。",
                "喂宠物简单料理后，5 分钟内移动更快，也更愿意干活。",
                "可以用命名牌给宠物命名。");
            addHandbookPage(adder, "upgrade", "升级公告板",
                "在公告板界面花费绿宝石升级，每升一级每天多贴一张牌。",
                "升到 2 级后会出现讨伐牌。",
                "拿剑的宠物负责近战讨伐，拿弓的负责远程讨伐。",
                "讨伐报酬最高，但宠物倒下时这张牌会失败。");
            addHandbookPage(adder, "safety", "倒下与召回",
                "宠物倒下后会变成玩偶，背包里的物品都保存在玩偶中。",
                "手持玩偶右键已放置的蛋糕，宠物会带着物品复活。",
                "生命值低于 35% 时，宠物会先撤出战斗，恢复到 60% 再回去。",
                "使用宠物铃铛，所有宠物都会传送到你身边，包括其他维度中的。");
            addHandbookPage(adder, "friends", "伙伴之间",
                "宠物说话时头顶会出现对话气泡，16 格内的玩家都能看到。",
                "两只宠物都空闲时，偶尔会演一段原作中的场景，比如飞鼠缠着吉伊。",
                "獭师父会请吉伊和小八吃东西，栗子馒头会给刚除完草的宠物送咖啡。",
                "小八街头演奏时，附近空闲的宠物会过来坐下听，时不时鼓掌。");
            addJobTranslations(adder, "职业", "无", "农夫", "剑士", "弓箭手", "音乐家", "未知");
            addEntityTranslations(adder, "乌萨奇", "小八", "吉伊", "狮萨", "飞鼠", "栗子馒头", "獭师父", "古本屋");
            addItemTagTranslations(adder, "农夫工具", "剑士工具", "弓箭手工具", "音乐家工具", "驯服食物", "种植作物", "运送物品", "可拾取物品", "请客的吃食");
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
                "小八的刺叉",
                "吉伊的刺叉",
                "獭师父的剑"
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
            addSocialTranslations(adder, "找伙伴玩", "陪伙伴玩", "附近没有想一起玩的伙伴", "没有伙伴过来找它");
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
            // What the pets say (design 0.1.1, section 3), in the words Chinese fans know the lines by.
            addVoiceLines(adder, "chiikawa", VoiceMoment.TAME, "哇……");
            addVoiceLines(adder, "chiikawa", VoiceMoment.HURT, "不要！！", "呜……呜……");
            addVoiceLines(adder, "chiikawa", VoiceMoment.HUNT, "呀——！！！");
            addVoiceLines(adder, "chiikawa", VoiceMoment.PAID, "！！");
            addVoiceLines(adder, "chiikawa", VoiceMoment.SHOP, "哇……！");
            addVoiceLines(adder, "chiikawa", VoiceMoment.GIFT, "嗯……！");
            addVoiceLines(adder, "chiikawa", VoiceMoment.REVIVE, "啊……！");
            addVoiceLines(adder, "chiikawa", VoiceMoment.IDLE, "嘿咻……", "呼……");
            addVoiceLines(adder, "hachiware", VoiceMoment.TAME, "也就是说……！？");
            addVoiceLines(adder, "hachiware", VoiceMoment.HURT, "痛痛痛……");
            addVoiceLines(adder, "hachiware", VoiceMoment.HUNT, "总会有办法的——！！");
            addVoiceLines(adder, "hachiware", VoiceMoment.PAID, "太好啦～！！");
            addVoiceLines(adder, "hachiware", VoiceMoment.SHOP, "这不就是……最棒的嘛！");
            addVoiceLines(adder, "hachiware", VoiceMoment.GIFT, "给，这个送你！");
            addVoiceLines(adder, "hachiware", VoiceMoment.REVIVE, "哭出来了！！");
            addVoiceLines(adder, "hachiware", VoiceMoment.IDLE, "哼哼～♪");
            addVoiceLines(adder, "usagi", VoiceMoment.TAME, "呀哈！");
            addVoiceLines(adder, "usagi", VoiceMoment.HURT, "哈啊？");
            addVoiceLines(adder, "usagi", VoiceMoment.HUNT, "乌拉——！！");
            addVoiceLines(adder, "usagi", VoiceMoment.PAID, "噗噜噜噜");
            addVoiceLines(adder, "usagi", VoiceMoment.SHOP, "噗噜呀");
            addVoiceLines(adder, "usagi", VoiceMoment.GIFT, "乌拉");
            addVoiceLines(adder, "usagi", VoiceMoment.REVIVE, "呀哈——！！");
            addVoiceLines(adder, "usagi", VoiceMoment.IDLE, "哼嗯", "噗噜噜噜", "乌拉");
            addVoiceLines(adder, "momonga", VoiceMoment.TAME, "终于成功啦！！");
            addVoiceLines(adder, "momonga", VoiceMoment.HURT, "快安慰我。");
            addVoiceLines(adder, "momonga", VoiceMoment.PAID, "快夸我。");
            addVoiceLines(adder, "momonga", VoiceMoment.SHOP, "给我！");
            addVoiceLines(adder, "momonga", VoiceMoment.GIFT, "快夸我。");
            addVoiceLines(adder, "momonga", VoiceMoment.REVIVE, "快安慰我。");
            addVoiceLines(adder, "momonga", VoiceMoment.IDLE, "快夸我。", "给我！");
            addVoiceLines(adder, "kurimanju", VoiceMoment.SHOP, "哈——……");
            addVoiceLines(adder, "rakko", VoiceMoment.TAME, "……还不错。");
            addVoiceLines(adder, "rakko", VoiceMoment.HUNT, "上了。");
            addVoiceLines(adder, "rakko", VoiceMoment.PAID, "哼。");
            addVoiceLines(adder, "rakko", VoiceMoment.SHOP, "……好吃。");
            addVoiceLines(adder, "rakko", VoiceMoment.GIFT, "……给你。");
            addVoiceLines(adder, "rakko", VoiceMoment.REVIVE, "……还差得远呢。");
            addVoiceLines(adder, "shisa", VoiceMoment.TAME, "嗨赛！", "请多关照！");
            addVoiceLines(adder, "shisa", VoiceMoment.HURT, "好痛！");
            addVoiceLines(adder, "shisa", VoiceMoment.HUNT, "我、我会加油的……！");
            addVoiceLines(adder, "shisa", VoiceMoment.PAID, "开心狮萨！");
            addVoiceLines(adder, "shisa", VoiceMoment.SHOP, "我开动啦！");
            addVoiceLines(adder, "shisa", VoiceMoment.GIFT, "请收下！");
            addVoiceLines(adder, "shisa", VoiceMoment.REVIVE, "没事的啦～");
            addVoiceLines(adder, "shisa", VoiceMoment.IDLE, "嗨赛！", "好想喝香片茶～");
            addVoiceLines(adder, "furuhonya", VoiceMoment.TAME, "蟹蟹！");
            addVoiceLines(adder, "furuhonya", VoiceMoment.GIFT, "给你……");
            addVoiceLines(adder, "furuhonya", VoiceMoment.IDLE, "蟹蟹");
            addVoiceLines(adder, "chiikawa", VoiceMoment.CLUNG_TO, "不要！！");
            addVoiceLines(adder, "chiikawa", VoiceMoment.TREATED, "哇……！");
            addVoiceLines(adder, "chiikawa", VoiceMoment.GIVEN_COFFEE, "哇……");
            addVoiceLines(adder, "chiikawa", VoiceMoment.LISTEN, "哇……");
            addVoiceLines(adder, "hachiware", VoiceMoment.CLUNG_TO, "诶，怎么了怎么了！？");
            addVoiceLines(adder, "hachiware", VoiceMoment.CRAB_GREETING, "蟹蟹～！");
            addVoiceLines(adder, "hachiware", VoiceMoment.TREATED, "谢谢老师！！");
            addVoiceLines(adder, "hachiware", VoiceMoment.GIVEN_COFFEE, "谢谢～！");
            addVoiceLines(adder, "hachiware", VoiceMoment.LISTEN, "好好听啊！");
            addVoiceLines(adder, "usagi", VoiceMoment.CLUNG_TO, "哈啊？");
            addVoiceLines(adder, "usagi", VoiceMoment.GIVEN_COFFEE, "呀哈");
            addVoiceLines(adder, "usagi", VoiceMoment.LISTEN, "噗噜噜噜");
            addVoiceLines(adder, "momonga", VoiceMoment.CLING, "快夸我。");
            addVoiceLines(adder, "momonga", VoiceMoment.TURNED_DOWN, "快安慰我。");
            addVoiceLines(adder, "momonga", VoiceMoment.CRAB_GREETING, "蟹！");
            addVoiceLines(adder, "momonga", VoiceMoment.GIVEN_COFFEE, "再给我点！");
            addVoiceLines(adder, "momonga", VoiceMoment.LISTEN, "再弹一首！");
            addVoiceLines(adder, "rakko", VoiceMoment.TREAT, "……吃吧。");
            addVoiceLines(adder, "rakko", VoiceMoment.GIVEN_COFFEE, "……帮大忙了。");
            addVoiceLines(adder, "shisa", VoiceMoment.CLUNG_TO, "那、那个……");
            addVoiceLines(adder, "shisa", VoiceMoment.GIVEN_COFFEE, "谢谢您！");
            addVoiceLines(adder, "shisa", VoiceMoment.LISTEN, "弹得真好～！");
            addVoiceLines(adder, "furuhonya", VoiceMoment.CRAB_GREETING, "蟹蟹");
            addVoiceLines(adder, "furuhonya", VoiceMoment.GIVEN_COFFEE, "谢谢……");
        } else if ("ja_jp".equals(locale)) {
            addCommonTranslations(adder, "ちいかわ", "持ち物", "ついてくる", "おすわり", "自由行動");
            addDollTooltipTranslations(adder, "ぬいぐるみをケーキに置いてみる？");
            addHandbookTranslations(adder, "ちいかわのお仕事ハンドブック", "右クリックで開いて、遊び方を確認できます",
                "このハンドブックはまだまっしろ", "前のページ", "次のページ");
            addHandbookPage(adder, "meet", "ちいかわたちとの出会い",
                "平原・サバンナ・砂漠・湿地・雪原に、道具を持った野生の子が現れます。",
                "食べ物を持って野生の子を右クリック。1回につき30%の確率でなつきます。",
                "スニークしながら右クリックで、ついてくる・おすわり・自由行動を切り替えます。",
                "右クリックで画面を開き、持ち物・状態・指示を確認できます。");
            addHandbookPage(adder, "work", "お仕事へ",
                "労働掲示板を置くと、毎日日の出に仕事カードが3枚貼り出されます。",
                "メインハンドの道具に合ったカードを取りに行きます。クワなら草むしりです。",
                "カードを取ると、書かれた量の仕事をしに行きます。",
                "報酬はその子の持ち物に直接入り、画面を開けば受け取れます。");
            addHandbookPage(adder, "shop", "お店へ",
                "お店を置くと、自由行動の子が持ち物のエメラルドで買い物に行きます。",
                "買うものは子によって違い、ちいかわはおやつをよく買います。",
                "カウンターを右クリックすると価格表が開き、プレイヤーも売り買いできます。",
                "エメラルドを持って自分の子を右クリックすると、おこづかいを渡せます。");
            addHandbookPage(adder, "presents", "プレゼント",
                "お店で買い物をするとき、ときどき飼い主へのプレゼントも買います。",
                "買ったあとは、プレゼントを持って飼い主のところまで歩いてきます。",
                "プレゼントはインベントリに直接入り、いっぱいなら足元に置かれます。",
                "買ったときと渡したときに、チャット欄でお知らせが届きます。");
            addHandbookPage(adder, "supplies", "カバンとごはん",
                "リュックを装備すると、持ち物が10マス増えます。",
                "くま・くじら・星のポーチも10マス増え、どの子でも装備できます。",
                "かんたんごはんを食べると、5分間移動が速くなり、仕事にも積極的になります。",
                "名札で名前をつけられます。");
            addHandbookPage(adder, "upgrade", "掲示板のレベルアップ",
                "掲示板の画面でエメラルドを払うと、レベルが上がり1日のカードが1枚増えます。",
                "レベル2から討伐のカードが出るようになります。",
                "剣を持った子は近くで、弓を持った子は遠くから討伐します。",
                "討伐は報酬が最も高いですが、倒れるとそのカードは失敗です。");
            addHandbookPage(adder, "safety", "倒れたとき・呼び戻し",
                "倒れた子はぬいぐるみになり、持ち物はすべてその中に残ります。",
                "ぬいぐるみを持って、置いたケーキを右クリックすると持ち物ごと復活します。",
                "体力が35%を下回ると戦いから離れ、60%まで回復してから戻ります。",
                "ペットベルを使うと、別のディメンションの子も含めて全員が手元に戻ります。");
            addHandbookPage(adder, "friends", "なかまどうし",
                "しゃべると頭の上にふきだしが出て、16ブロック以内のプレイヤーに見えます。",
                "手の空いた2匹は、ときどき原作の一場面を演じます。たとえばモモンガのしがみつき。",
                "ラッコはごちそうを、くりまんじゅうは草むしり帰りの子にコーヒーを渡します。",
                "ハチワレが路上演奏していると、近くの手の空いた子が座って聴き、拍手します。");
            addJobTranslations(adder, "職業", "なし", "農家", "剣士", "弓使い", "音楽家", "不明");
            addEntityTranslations(adder, "うさぎ", "ハチワレ", "ちいかわ", "シーサー", "モモンガ", "くりまんじゅう", "ラッコ", "古本屋");
            addItemTagTranslations(adder, "農家の道具", "剣士の道具", "弓使いの道具", "音楽家の道具", "なつかせる食べ物", "植える作物", "運ぶアイテム", "拾えるアイテム", "おごる食べ物");
            addItemTranslations(
                adder,
                "うさぎのスポーンエッグ",
                "ハチワレのスポーンエッグ",
                "ちいかわのスポーンエッグ",
                "シーサーのスポーンエッグ",
                "モモンガのスポーンエッグ",
                "くりまんじゅうのスポーンエッグ",
                "ラッコのスポーンエッグ",
                "古本屋のスポーンエッグ",
                "うさぎのぬいぐるみ",
                "ハチワレのぬいぐるみ",
                "ちいかわのぬいぐるみ",
                "シーサーのぬいぐるみ",
                "モモンガのぬいぐるみ",
                "くりまんじゅうのぬいぐるみ",
                "ラッコのぬいぐるみ",
                "古本屋のぬいぐるみ",
                "うさぎの討伐棒",
                "ハチワレのさすまた",
                "ちいかわのさすまた",
                "ラッコの剣"
            );
            addMusicBoxTranslations(adder, "オルゴール", "曲が選ばれていません", "曲：%s", "曲を選ぶ", "読み込み中", "読み込み失敗", "再生できる曲がありません",
                "MP3・WAVファイルをフォルダに入れて、更新を押してください", "一部の曲を読み込めませんでした", "MP3形式の音声を使ってください", "フォルダを開く", "更新", "前のページ", "次のページ");
            addIntentFailureTranslations(adder, "飼い主についていけない", "飼い主がすぐ近くにいる", "飼い主のそばに戻った", "目標が届かないところにいる",
                "目標が行動範囲の外に出た", "近くに拾えるアイテムがない", "収穫できる作物がない", "種をまける耕地がない", "しまえる入れ物がない",
                "攻撃する相手がいない", "攻撃のクールダウン中", "矢がない", "新しく演奏できる曲がない", "演奏が終わった",
                "もっと大事なことが先", "近くに雑草がない", "近くにキノコがない",
                "合うお仕事カードを取ったときだけやる", "夜だけやる",
                "もうお仕事カードを持っている", "カードを取りそこねたので、ひと休み", "近くの掲示板に合うカードがない",
                "さっき買い物したばかり", "お店に欲しくて買えるものがない", "渡すものを持っていない");
            addBlockTranslations(adder, "労働掲示板", "お店");
            addSupplyTranslations(adder, "リュック", "くまのポーチ", "くじらのポーチ", "星のポーチ", "ペットに背負わせると、持ち物が10マス増える",
                    "かんたんごはん", "ペットに食べさせると、しばらくお仕事にやる気が出る",
                    "ペットベル", "ペットを呼び戻す。別のディメンションでも聞こえる。一度鳴らすと30秒待つ");
            addPetNewsTranslations(adder, "%1$sが%4$s%2$s個で%3$sを買った",
                    "%1$sが%4$s%2$s個で%3$sを買って、その場で食べた",
                    "%1$sが%4$s%2$s個で%3$sを買った。あなたへのプレゼントだって",
                    "%1$sが%2$sをくれた");
            addRecallTranslations(adder, "ペットが%s匹戻ってきた", "呼べるペットがいない",
                    "%sから返事がない。最後に見かけた場所にはもういない");
            addShopScreenTranslations(adder, "お店", "買う %s", "売る %s", "今日は売り物がありません",
                    "前のページ", "次のページ");
            addIntentNameTranslations(adder, "飼い主についていく", "おすわり中", "ぶらぶら", "アイテムを拾う", "お仕事カードを取りに行く",
                "収穫", "種まき", "運ぶ", "草むしり", "キノコ採り", "討伐", "弓で射る", "演奏", "お買い物", "プレゼントを渡す");
            addSocialTranslations(adder, "なかまのところへ", "なかまに付き合う", "近くに遊びたいなかまがいない", "誰も来ていない");
            addTaskTypeTranslations(adder, "草むしり", "%s本", "路上演奏", "%s秒",
                "近接討伐", "遠距離討伐", "%s体");
            addBoardScreenTranslations(adder, "労働掲示板", "%s枚", "今日はお仕事カードがありません",
                    "%s限定", "募集中", "受注済み", "%sが受けた",
                    "Lv.%s", "1日%s枚", "レベルアップ %s",
                    "Lv.%1$sで1日%2$s枚", "新しいお仕事：%s",
                    "最大レベル", "%2$sを%1$s個持っています");
            addPetStatusTranslations(adder, "ひま", "リュックかポーチを背負うと、持ち物が10マス増える");
            addPetScreenTranslations(adder, "持ち物", "ようす", "指示",
                    "お仕事カードなし",
                    "やる気まんまん：残り%s",
                    "おこづかい：%2$s%1$s個",
                    "あなたへのプレゼント",
                    "自分で持ってきて渡してくれる",
                    "そばにいて、敵が来たら戦う",
                    "その場から動かない",
                    "家の近くで働いたり、買い物したり、カードを取ったり");
            // What the pets say (design 0.1.1, section 3): the series' own lines.
            addVoiceLines(adder, "chiikawa", VoiceMoment.TAME, "ワァ…");
            addVoiceLines(adder, "chiikawa", VoiceMoment.HURT, "ヤダッ!!", "ウッ…ウッ…");
            addVoiceLines(adder, "chiikawa", VoiceMoment.HUNT, "ヤーッ!!!");
            addVoiceLines(adder, "chiikawa", VoiceMoment.PAID, "!!");
            addVoiceLines(adder, "chiikawa", VoiceMoment.SHOP, "ワァ…!");
            addVoiceLines(adder, "chiikawa", VoiceMoment.GIFT, "ン…!");
            addVoiceLines(adder, "chiikawa", VoiceMoment.REVIVE, "ハッ…!");
            addVoiceLines(adder, "chiikawa", VoiceMoment.IDLE, "ンショ…", "フゥ…");
            addVoiceLines(adder, "hachiware", VoiceMoment.TAME, "ってコト!?");
            addVoiceLines(adder, "hachiware", VoiceMoment.HURT, "イテテ…");
            addVoiceLines(adder, "hachiware", VoiceMoment.HUNT, "なんとかなれーッ!!");
            addVoiceLines(adder, "hachiware", VoiceMoment.PAID, "ヤッタ〜!!");
            addVoiceLines(adder, "hachiware", VoiceMoment.SHOP, "これって…最高じゃん!");
            addVoiceLines(adder, "hachiware", VoiceMoment.GIFT, "はい、これ!");
            addVoiceLines(adder, "hachiware", VoiceMoment.REVIVE, "泣いちゃった!!");
            addVoiceLines(adder, "hachiware", VoiceMoment.IDLE, "フンフ〜ン♪");
            addVoiceLines(adder, "usagi", VoiceMoment.TAME, "ヤハ!");
            addVoiceLines(adder, "usagi", VoiceMoment.HURT, "ハァ？");
            addVoiceLines(adder, "usagi", VoiceMoment.HUNT, "ウラァ!!");
            addVoiceLines(adder, "usagi", VoiceMoment.PAID, "プルルル");
            addVoiceLines(adder, "usagi", VoiceMoment.SHOP, "プルャ");
            addVoiceLines(adder, "usagi", VoiceMoment.GIFT, "ウラ");
            addVoiceLines(adder, "usagi", VoiceMoment.REVIVE, "ヤハーッ!!");
            addVoiceLines(adder, "usagi", VoiceMoment.IDLE, "フゥン", "プルルル", "ウラ");
            addVoiceLines(adder, "momonga", VoiceMoment.TAME, "ついにやったゾ!!");
            addVoiceLines(adder, "momonga", VoiceMoment.HURT, "慰めろ");
            addVoiceLines(adder, "momonga", VoiceMoment.PAID, "褒めろ");
            addVoiceLines(adder, "momonga", VoiceMoment.SHOP, "よこせッ");
            addVoiceLines(adder, "momonga", VoiceMoment.GIFT, "褒めろ");
            addVoiceLines(adder, "momonga", VoiceMoment.REVIVE, "慰めろ");
            addVoiceLines(adder, "momonga", VoiceMoment.IDLE, "褒めろ", "よこせッ");
            addVoiceLines(adder, "kurimanju", VoiceMoment.SHOP, "ハーッ…");
            addVoiceLines(adder, "rakko", VoiceMoment.TAME, "…悪くない");
            addVoiceLines(adder, "rakko", VoiceMoment.HUNT, "いくぞ");
            addVoiceLines(adder, "rakko", VoiceMoment.PAID, "フッ");
            addVoiceLines(adder, "rakko", VoiceMoment.SHOP, "…うまい");
            addVoiceLines(adder, "rakko", VoiceMoment.GIFT, "…やる");
            addVoiceLines(adder, "rakko", VoiceMoment.REVIVE, "…まだまだだな");
            addVoiceLines(adder, "shisa", VoiceMoment.TAME, "はいさい!", "よろしくお願いします!");
            addVoiceLines(adder, "shisa", VoiceMoment.HURT, "アガッ!");
            addVoiceLines(adder, "shisa", VoiceMoment.HUNT, "が、がんばります…!");
            addVoiceLines(adder, "shisa", VoiceMoment.PAID, "うれシーサー!");
            addVoiceLines(adder, "shisa", VoiceMoment.SHOP, "いただきます!");
            addVoiceLines(adder, "shisa", VoiceMoment.GIFT, "これ、どうぞ!");
            addVoiceLines(adder, "shisa", VoiceMoment.REVIVE, "なんくるないさ〜");
            addVoiceLines(adder, "shisa", VoiceMoment.IDLE, "はいさい!", "さんぴん茶、飲みたいな〜");
            addVoiceLines(adder, "furuhonya", VoiceMoment.TAME, "カニ!");
            addVoiceLines(adder, "furuhonya", VoiceMoment.GIFT, "どうぞ…");
            addVoiceLines(adder, "furuhonya", VoiceMoment.IDLE, "カニ");
            addVoiceLines(adder, "chiikawa", VoiceMoment.CLUNG_TO, "ヤダッ!!");
            addVoiceLines(adder, "chiikawa", VoiceMoment.TREATED, "ワァ…!");
            addVoiceLines(adder, "chiikawa", VoiceMoment.GIVEN_COFFEE, "ワァ…");
            addVoiceLines(adder, "chiikawa", VoiceMoment.LISTEN, "ワァ…");
            addVoiceLines(adder, "hachiware", VoiceMoment.CLUNG_TO, "えっ、なになに!?");
            addVoiceLines(adder, "hachiware", VoiceMoment.CRAB_GREETING, "カニ〜!");
            addVoiceLines(adder, "hachiware", VoiceMoment.TREATED, "ありがとうございます、先生!!");
            addVoiceLines(adder, "hachiware", VoiceMoment.GIVEN_COFFEE, "ありがと〜!");
            addVoiceLines(adder, "hachiware", VoiceMoment.LISTEN, "いい曲じゃん!");
            addVoiceLines(adder, "usagi", VoiceMoment.CLUNG_TO, "ハァ？");
            addVoiceLines(adder, "usagi", VoiceMoment.GIVEN_COFFEE, "ヤハ");
            addVoiceLines(adder, "usagi", VoiceMoment.LISTEN, "プルルル");
            addVoiceLines(adder, "momonga", VoiceMoment.CLING, "褒めろ");
            addVoiceLines(adder, "momonga", VoiceMoment.TURNED_DOWN, "慰めろ");
            addVoiceLines(adder, "momonga", VoiceMoment.CRAB_GREETING, "カニッ");
            addVoiceLines(adder, "momonga", VoiceMoment.GIVEN_COFFEE, "もっとよこせッ");
            addVoiceLines(adder, "momonga", VoiceMoment.LISTEN, "もっと弾け");
            addVoiceLines(adder, "rakko", VoiceMoment.TREAT, "…食え");
            addVoiceLines(adder, "rakko", VoiceMoment.GIVEN_COFFEE, "…助かる");
            addVoiceLines(adder, "shisa", VoiceMoment.CLUNG_TO, "あ、あの…");
            addVoiceLines(adder, "shisa", VoiceMoment.GIVEN_COFFEE, "ありがとうございます!");
            addVoiceLines(adder, "shisa", VoiceMoment.LISTEN, "じょうずですね〜!");
            addVoiceLines(adder, "furuhonya", VoiceMoment.CRAB_GREETING, "カニ");
            addVoiceLines(adder, "furuhonya", VoiceMoment.GIVEN_COFFEE, "ありがとう…");
        } else {
            addCommonTranslations(adder, "Chiikawa", "Pet Backpack", "Follow", "Sit", "Free Roam");
            addDollTooltipTranslations(adder, "Try placing the doll on a cake?");
            addHandbookTranslations(adder, "Chiikawa's Work Handbook", "Right-click to open: how everything works",
                "This handbook is still blank", "Previous page", "Next page");
            addHandbookPage(adder, "meet", "Meeting Them",
                "Wild pets roam plains, savannas, deserts, swamps and snowfields.",
                "Right-click a wild pet with food; each try has a 30% chance to tame it.",
                "Sneak and right-click your pet to switch: Follow, Sit, Free Roam.",
                "Right-click your pet to open its screen: bag, status and orders.");
            addHandbookPage(adder, "work", "Off to Work",
                "Place a labor board. Each sunrise it puts up 3 work slips.",
                "Pets take slips for the tool in their main hand: a hoe takes weeding.",
                "With a slip, the pet goes to do the amount written on it.",
                "The pay goes into the pet's bag; open its screen to take it out.");
            addHandbookPage(adder, "shop", "The Shop",
                "Place a shop, and free-roaming pets spend the emeralds in their bags.",
                "Each pet buys what it likes; Chiikawa mostly buys snacks.",
                "Right-click the counter for the price list; players can trade too.",
                "Right-click your pet holding emeralds to give it pocket money.");
            addHandbookPage(adder, "presents", "Presents",
                "When a pet goes shopping, it sometimes buys its owner a present.",
                "Then it walks the present over to its owner.",
                "The present goes into your inventory, or at your feet if it is full.",
                "A chat message tells you when it buys a present and when it gives it.");
            addHandbookPage(adder, "supplies", "Bags and Treats",
                "A backpack adds 10 slots to a pet's bag.",
                "Bear, whale and star pouches also add 10 slots; any pet can wear one.",
                "A simple dish makes a pet faster and keener to work for 5 minutes.",
                "Use a name tag to name your pet.");
            addHandbookPage(adder, "upgrade", "A Better Board",
                "Pay emeralds on the board's screen to level it up: one more slip a day.",
                "From level 2, the board puts up hunting slips.",
                "Pets with swords hunt up close; pets with bows shoot from range.",
                "Hunting pays the most, but the slip fails if the pet falls.");
            addHandbookPage(adder, "safety", "Falling and Coming Back",
                "A pet that falls becomes a doll, with everything it carried inside.",
                "Right-click a placed cake with the doll to bring the pet back.",
                "Below 35% health a pet leaves the fight, and returns at 60%.",
                "Use the pet bell to bring all your pets to you, from any dimension.");
            addHandbookPage(adder, "friends", "Among Friends",
                "When a pet talks, a speech bubble shows to players within 16 blocks.",
                "Two idle pets sometimes act out a scene, like Momonga clinging on.",
                "Rakko treats Chiikawa and Hachiware; Kurimanju brings weeders coffee.",
                "When Hachiware busks, idle pets nearby sit, listen and clap.");
            addJobTranslations(adder, "Job", "None", "Farmer", "Fencer", "Archer", "Musician", "Unknown");
            addEntityTranslations(adder, "Usagi", "Hachiware", "Chiikawa", "Shisa", "Momonga", "Kurimanju", "Rakko", "Furuhonya");
            addItemTagTranslations(adder, "Farmer Tools", "Fencer Tools", "Archer Tools", "Musician Tools", "Tame Foods", "Plant Crops", "Deliver Items", "Pickable Items", "Pet Treats");
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
                "Usagi's Subjugation Stick",
                "Hachiware's Sasumata",
                "Chiikawa's Sasumata",
                "Rakko's Sword"
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
            addSocialTranslations(adder, "Visiting a friend", "Playing along", "Nobody nearby it feels like playing with",
                "Nobody is coming over");
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
            // What the pets say (design 0.1.1, section 3), in the words English-speaking fans know the lines by.
            addVoiceLines(adder, "chiikawa", VoiceMoment.TAME, "Wah...");
            addVoiceLines(adder, "chiikawa", VoiceMoment.HURT, "Nooo!!", "Hic... hic...");
            addVoiceLines(adder, "chiikawa", VoiceMoment.HUNT, "Yaaah!!!");
            addVoiceLines(adder, "chiikawa", VoiceMoment.PAID, "!!");
            addVoiceLines(adder, "chiikawa", VoiceMoment.SHOP, "Wah...!");
            addVoiceLines(adder, "chiikawa", VoiceMoment.GIFT, "Mm...!");
            addVoiceLines(adder, "chiikawa", VoiceMoment.REVIVE, "Hah...!");
            addVoiceLines(adder, "chiikawa", VoiceMoment.IDLE, "Hup...", "Phew...");
            addVoiceLines(adder, "hachiware", VoiceMoment.TAME, "Does that mean...!?");
            addVoiceLines(adder, "hachiware", VoiceMoment.HURT, "Ow ow ow...");
            addVoiceLines(adder, "hachiware", VoiceMoment.HUNT, "Something'll work out!!");
            addVoiceLines(adder, "hachiware", VoiceMoment.PAID, "Yaaay!!");
            addVoiceLines(adder, "hachiware", VoiceMoment.SHOP, "Isn't this... the best!");
            addVoiceLines(adder, "hachiware", VoiceMoment.GIFT, "Here, this is for you!");
            addVoiceLines(adder, "hachiware", VoiceMoment.REVIVE, "I'm crying!!");
            addVoiceLines(adder, "hachiware", VoiceMoment.IDLE, "Hmm hmm~♪");
            addVoiceLines(adder, "usagi", VoiceMoment.TAME, "Yaha!");
            addVoiceLines(adder, "usagi", VoiceMoment.HURT, "Haah?");
            addVoiceLines(adder, "usagi", VoiceMoment.HUNT, "Uraaa!!");
            addVoiceLines(adder, "usagi", VoiceMoment.PAID, "Prrrrr");
            addVoiceLines(adder, "usagi", VoiceMoment.SHOP, "Purya");
            addVoiceLines(adder, "usagi", VoiceMoment.GIFT, "Ura");
            addVoiceLines(adder, "usagi", VoiceMoment.REVIVE, "Yahaaa!!");
            addVoiceLines(adder, "usagi", VoiceMoment.IDLE, "Hmmn", "Prrrrr", "Ura");
            addVoiceLines(adder, "momonga", VoiceMoment.TAME, "I finally did it!!");
            addVoiceLines(adder, "momonga", VoiceMoment.HURT, "Comfort me.");
            addVoiceLines(adder, "momonga", VoiceMoment.PAID, "Praise me.");
            addVoiceLines(adder, "momonga", VoiceMoment.SHOP, "Gimme!");
            addVoiceLines(adder, "momonga", VoiceMoment.GIFT, "Praise me.");
            addVoiceLines(adder, "momonga", VoiceMoment.REVIVE, "Comfort me.");
            addVoiceLines(adder, "momonga", VoiceMoment.IDLE, "Praise me.", "Gimme!");
            addVoiceLines(adder, "kurimanju", VoiceMoment.SHOP, "Haaah...");
            addVoiceLines(adder, "rakko", VoiceMoment.TAME, "...Not bad.");
            addVoiceLines(adder, "rakko", VoiceMoment.HUNT, "Let's go.");
            addVoiceLines(adder, "rakko", VoiceMoment.PAID, "Heh.");
            addVoiceLines(adder, "rakko", VoiceMoment.SHOP, "...Tasty.");
            addVoiceLines(adder, "rakko", VoiceMoment.GIFT, "...Here.");
            addVoiceLines(adder, "rakko", VoiceMoment.REVIVE, "...Still a long way to go.");
            addVoiceLines(adder, "shisa", VoiceMoment.TAME, "Haisai!", "Pleased to meet you!");
            addVoiceLines(adder, "shisa", VoiceMoment.HURT, "Ow!");
            addVoiceLines(adder, "shisa", VoiceMoment.HUNT, "I-I'll do my best...!");
            addVoiceLines(adder, "shisa", VoiceMoment.PAID, "Happy-shisa!");
            addVoiceLines(adder, "shisa", VoiceMoment.SHOP, "Thanks for the treat!");
            addVoiceLines(adder, "shisa", VoiceMoment.GIFT, "Please, take this!");
            addVoiceLines(adder, "shisa", VoiceMoment.REVIVE, "It'll all work out~");
            addVoiceLines(adder, "shisa", VoiceMoment.IDLE, "Haisai!", "I could go for some sanpin tea~");
            addVoiceLines(adder, "furuhonya", VoiceMoment.TAME, "Crab!");
            addVoiceLines(adder, "furuhonya", VoiceMoment.GIFT, "For you...");
            addVoiceLines(adder, "furuhonya", VoiceMoment.IDLE, "Crab");
            addVoiceLines(adder, "chiikawa", VoiceMoment.CLUNG_TO, "Nooo!!");
            addVoiceLines(adder, "chiikawa", VoiceMoment.TREATED, "Wah...!");
            addVoiceLines(adder, "chiikawa", VoiceMoment.GIVEN_COFFEE, "Wah...");
            addVoiceLines(adder, "chiikawa", VoiceMoment.LISTEN, "Wah...");
            addVoiceLines(adder, "hachiware", VoiceMoment.CLUNG_TO, "Huh, what is it!?");
            addVoiceLines(adder, "hachiware", VoiceMoment.CRAB_GREETING, "Crab~!");
            addVoiceLines(adder, "hachiware", VoiceMoment.TREATED, "Thank you, Sensei!!");
            addVoiceLines(adder, "hachiware", VoiceMoment.GIVEN_COFFEE, "Thanks~!");
            addVoiceLines(adder, "hachiware", VoiceMoment.LISTEN, "What a song!");
            addVoiceLines(adder, "usagi", VoiceMoment.CLUNG_TO, "Haah?");
            addVoiceLines(adder, "usagi", VoiceMoment.GIVEN_COFFEE, "Yaha");
            addVoiceLines(adder, "usagi", VoiceMoment.LISTEN, "Prrrrr");
            addVoiceLines(adder, "momonga", VoiceMoment.CLING, "Praise me.");
            addVoiceLines(adder, "momonga", VoiceMoment.TURNED_DOWN, "Comfort me.");
            addVoiceLines(adder, "momonga", VoiceMoment.CRAB_GREETING, "Crab!");
            addVoiceLines(adder, "momonga", VoiceMoment.GIVEN_COFFEE, "More! Gimme!");
            addVoiceLines(adder, "momonga", VoiceMoment.LISTEN, "Play another!");
            addVoiceLines(adder, "rakko", VoiceMoment.TREAT, "...Eat.");
            addVoiceLines(adder, "rakko", VoiceMoment.GIVEN_COFFEE, "...Appreciated.");
            addVoiceLines(adder, "shisa", VoiceMoment.CLUNG_TO, "U-um...");
            addVoiceLines(adder, "shisa", VoiceMoment.GIVEN_COFFEE, "Thank you so much!");
            addVoiceLines(adder, "shisa", VoiceMoment.LISTEN, "You play so well~!");
            addVoiceLines(adder, "furuhonya", VoiceMoment.CRAB_GREETING, "Crab");
            addVoiceLines(adder, "furuhonya", VoiceMoment.GIVEN_COFFEE, "Thank you...");
        }
    }

    /** A pet's lines for one moment, numbered from 1 in the order given; see {@link PetVoiceData}. */
    private static void addVoiceLines(Adder adder, String pet, VoiceMoment moment, String... lines) {
        for (int i = 0; i < lines.length; i++) {
            adder.add(PetVoiceData.lineKey(pet, moment, i + 1), lines[i]);
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

    /**
     * Pets meeting pets: what a pet going over to another is doing, what the one it goes to
     * is doing, and why either is not.
     */
    private static void addSocialTranslations(Adder adder, String socialize, String cooperate, String noPartner,
            String notAsked) {
        adder.add("intent.chiikawa.socialize", socialize);
        adder.add("intent.chiikawa.cooperate", cooperate);
        adder.add("intent.chiikawa.fail.no_partner", noPartner);
        adder.add("intent.chiikawa.fail.not_asked", notAsked);
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

    /**
     * The handbook itself. What the pets in it say is not here: they say their own lines,
     * the ones written under {@link #addVoiceLines}.
     */
    private static void addHandbookTranslations(Adder adder, String name, String tip, String empty,
            String previousPage, String nextPage) {
        adder.add("item.chiikawa.handbook", name);
        adder.add("tooltip.chiikawa.handbook", tip);
        adder.add("screen.chiikawa.handbook.empty", empty);
        adder.add("screen.chiikawa.handbook.previous_page", previousPage);
        adder.add("screen.chiikawa.handbook.next_page", nextPage);
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
        String pickableItems,
        String petTreats
    ) {
        adder.add("tag.item.chiikawa.entity_farmer_tools", farmerTools);
        adder.add("tag.item.chiikawa.entity_fencer_tools", fencerTools);
        adder.add("tag.item.chiikawa.entity_archer_tools", archerTools);
        adder.add("tag.item.chiikawa.entity_musician_tools", musicianTools);
        adder.add("tag.item.chiikawa.entity_tame_foods", tameFoods);
        adder.add("tag.item.chiikawa.entity_plant_crops", plantCrops);
        adder.add("tag.item.chiikawa.entity_deliver_items", deliverItems);
        adder.add("tag.item.chiikawa.entity_pickable_items", pickableItems);
        adder.add("tag.item.chiikawa.pet_treats", petTreats);
    }

    private static void addItemTranslations(Adder adder, String usagiEgg, String hachiwareEgg, String chiikawaEgg,
        String shisaEgg, String momongaEgg, String kurimanjuEgg, String rakkoEgg, String furuhonyaEgg,
        String usagiDoll, String hachiwareDoll, String chiikawaDoll,
        String shisaDoll, String momongaDoll, String kurimanjuDoll, String rakkoDoll, String furuhonyaDoll,
        String usagiWeapon, String hachiwareWeapon, String chiikawaWeapon, String rakkoSword) {
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
        adder.add("item.chiikawa.rakko_sword", rakkoSword);
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
