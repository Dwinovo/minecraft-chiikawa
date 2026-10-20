package com.dwinovo.chiikawa.init;

import com.dwinovo.chiikawa.platform.Services;
import java.util.function.Supplier;
import net.minecraft.world.entity.schedule.Activity;

public final class InitActivity {
    // Walking back to the owner.
    public static final Supplier<Activity> FOLLOW_OWNER =
        Services.PLATFORM_REGISTRY.followOwnerActivity();
    // Sitting still.
    public static final Supplier<Activity> STAY =
        Services.PLATFORM_REGISTRY.stayActivity();
    // Going over to another pet to play a scene with it.
    public static final Supplier<Activity> SOCIALIZE =
        Services.PLATFORM_REGISTRY.socializeActivity();
    // Playing along while another pet comes over.
    public static final Supplier<Activity> COOPERATE =
        Services.PLATFORM_REGISTRY.cooperateActivity();

    // A wild pet going over to see who whistled.
    public static final Supplier<Activity> ANSWER_WHISTLE =
        Services.PLATFORM_REGISTRY.answerWhistleActivity();

    private InitActivity() {
    }

    public static void init() {
    }
}
