package com.dwinovo.chiikawa.init;

import com.dwinovo.chiikawa.platform.Services;
import java.util.function.Supplier;
import net.minecraft.world.entity.schedule.Activity;

public final class InitActivity {
    // Farmer harvest activity.
    public static final Supplier<Activity> FARMER_HARVEST =
        Services.PLATFORM_REGISTRY.farmerHarvestActivity();
    // Farmer plant activity.
    public static final Supplier<Activity> FARMER_PLANT =
        Services.PLATFORM_REGISTRY.farmerPlantActivity();
    // Deliver activity.
    public static final Supplier<Activity> DELEVER =
        Services.PLATFORM_REGISTRY.deleverActivity();
    // Pulling up weeds.
    public static final Supplier<Activity> WEED =
        Services.PLATFORM_REGISTRY.weedActivity();
    // Picking mushrooms.
    public static final Supplier<Activity> PICK_MUSHROOM =
        Services.PLATFORM_REGISTRY.pickMushroomActivity();
    // Fencer combat activity.
    public static final Supplier<Activity> FENCER_FIGHT =
        Services.PLATFORM_REGISTRY.fencerFightActivity();
    // Archer ranged-attack activity.
    public static final Supplier<Activity> ARCHER_SHOOT =
        Services.PLATFORM_REGISTRY.archerShootActivity();
    // Musician performance activity.
    public static final Supplier<Activity> MUSICIAN_PLAY =
        Services.PLATFORM_REGISTRY.musicianPlayActivity();
    // Walking back to the owner.
    public static final Supplier<Activity> FOLLOW_OWNER =
        Services.PLATFORM_REGISTRY.followOwnerActivity();
    // Sitting still.
    public static final Supplier<Activity> STAY =
        Services.PLATFORM_REGISTRY.stayActivity();
    // Picking up a nearby item.
    public static final Supplier<Activity> PICK_UP =
        Services.PLATFORM_REGISTRY.pickUpActivity();
    // Walking to a labor board to take a slip.
    public static final Supplier<Activity> TAKE_TASK =
        Services.PLATFORM_REGISTRY.takeTaskActivity();
    // Walking to a shop to spend what the pet has earned.
    public static final Supplier<Activity> SHOP =
        Services.PLATFORM_REGISTRY.shopActivity();
    // Taking the owner a present.
    public static final Supplier<Activity> GIFT_OWNER =
        Services.PLATFORM_REGISTRY.giftOwnerActivity();

    private InitActivity() {
    }

    public static void init() {
    }
}
