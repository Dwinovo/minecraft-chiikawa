package com.dwinovo.chiikawa.entity.brain.errand;

import com.dwinovo.chiikawa.Constants;
import net.minecraft.resources.ResourceLocation;

/**
 * The id of every errand a pet can be on. It is also the value of the synced {@code INTENT} string, the end of the
 * {@code intent.chiikawa.*} translation keys and the key of a personality's multipliers.
 */
public final class ErrandIds {
    public static final ResourceLocation FOLLOW_OWNER = id("follow_owner");
    public static final ResourceLocation STAY = id("stay");
    public static final ResourceLocation WANDER = id("wander");
    public static final ResourceLocation PICK_UP_ITEM = id("pick_up_item");
    public static final ResourceLocation HARVEST = id("harvest");
    public static final ResourceLocation PLANT = id("plant");
    public static final ResourceLocation DELIVER = id("deliver");
    public static final ResourceLocation WEED = id("weed");
    public static final ResourceLocation PICK_MUSHROOM = id("pick_mushroom");
    public static final ResourceLocation MELEE = id("melee");
    public static final ResourceLocation RANGED = id("ranged");
    public static final ResourceLocation PLAY_MUSIC = id("play_music");
    public static final ResourceLocation TAKE_TASK = id("take_task");
    public static final ResourceLocation SHOP = id("shop");
    public static final ResourceLocation GIFT_OWNER = id("gift_owner");
    public static final ResourceLocation SOCIALIZE = id("socialize");
    public static final ResourceLocation COOPERATE = id("cooperate");
    public static final ResourceLocation TAKE_EXAM = id("take_exam");
    public static final ResourceLocation CHECK_RESULTS = id("check_results");
    public static final ResourceLocation ANSWER_WHISTLE = id("answer_whistle");
    public static final ResourceLocation TAKE_PHOTO = id("take_photo");

    private ErrandIds() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
    }
}
