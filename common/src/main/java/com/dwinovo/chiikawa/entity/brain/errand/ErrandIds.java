package com.dwinovo.chiikawa.entity.brain.errand;

import com.dwinovo.chiikawa.Constants;
import net.minecraft.resources.Identifier;

/**
 * The id of every errand a pet can be on. It is also the value of the synced {@code INTENT} string, the end of the
 * {@code intent.chiikawa.*} translation keys and the key of a personality's multipliers.
 */
public final class ErrandIds {
    public static final Identifier FOLLOW_OWNER = id("follow_owner");
    public static final Identifier STAY = id("stay");
    public static final Identifier WANDER = id("wander");
    public static final Identifier PICK_UP_ITEM = id("pick_up_item");
    public static final Identifier HARVEST = id("harvest");
    public static final Identifier PLANT = id("plant");
    public static final Identifier DELIVER = id("deliver");
    public static final Identifier WEED = id("weed");
    public static final Identifier PICK_MUSHROOM = id("pick_mushroom");
    public static final Identifier TAKE_TASK = id("take_task");
    public static final Identifier SHOP = id("shop");
    public static final Identifier GIFT_OWNER = id("gift_owner");
    public static final Identifier SOCIALIZE = id("socialize");
    public static final Identifier COOPERATE = id("cooperate");
    public static final Identifier TAKE_EXAM = id("take_exam");
    public static final Identifier CHECK_RESULTS = id("check_results");
    public static final Identifier ANSWER_WHISTLE = id("answer_whistle");
    public static final Identifier TAKE_PHOTO = id("take_photo");

    private ErrandIds() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Constants.MOD_ID, path);
    }
}
