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

    private InitActivity() {
    }

    public static void init() {
    }
}
