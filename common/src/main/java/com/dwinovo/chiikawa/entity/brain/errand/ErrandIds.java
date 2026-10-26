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
    public static final ResourceLocation TAKE_TASK = id("take_task");
    public static final ResourceLocation SHOP = id("shop");
    public static final ResourceLocation OFFER_GIFT = id("offer_gift");
    public static final ResourceLocation GIFT_OWNER = id("gift_owner");
    public static final ResourceLocation SOCIALIZE = id("socialize");
    public static final ResourceLocation COOPERATE = id("cooperate");
    public static final ResourceLocation TAKE_EXAM = id("take_exam");
    public static final ResourceLocation CHECK_RESULTS = id("check_results");
    public static final ResourceLocation ANSWER_WHISTLE = id("answer_whistle");
    public static final ResourceLocation TAKE_PHOTO = id("take_photo");
    public static final ResourceLocation DEFEND_SELF = id("defend_self");
    public static final ResourceLocation FLEE = id("flee");

    private ErrandIds() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
    }
}
