package com.dwinovo.chiikawa.init;

import com.dwinovo.chiikawa.platform.Services;
import java.util.function.Supplier;
import net.minecraft.world.entity.schedule.Activity;

public final class InitActivity {
    // A wild pet going over to see who whistled.
    public static final Supplier<Activity> ANSWER_WHISTLE =
        Services.PLATFORM_REGISTRY.answerWhistleActivity();

    private InitActivity() {
    }

    public static void init() {
    }
}
