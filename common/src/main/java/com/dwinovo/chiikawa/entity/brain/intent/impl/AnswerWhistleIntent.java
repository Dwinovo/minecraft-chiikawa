package com.dwinovo.chiikawa.entity.brain.intent.impl;

import com.dwinovo.chiikawa.entity.brain.intent.IntentCategory;
import com.dwinovo.chiikawa.entity.brain.intent.IntentCheck;
import com.dwinovo.chiikawa.entity.brain.intent.IntentContext;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntent;
import com.dwinovo.chiikawa.entity.brain.intent.PetIntents;
import com.dwinovo.chiikawa.init.InitActivity;
import com.dwinovo.chiikawa.init.InitMemory;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;

/**
 * A wild pet that heard a whistle goes over to see who blew it, and looks at them until
 * its curiosity runs out. It is never tamed by it: when the memory expires the pet goes
 * back to what it was doing.
 *
 * <p>Scored above foraging and below fighting and taking a slip: a wild pet is curious,
 * not so curious that it lets a fight or a job go.
 */
public final class AnswerWhistleIntent implements PetIntent {
    private static final float SCORE = 0.55F;

    @Override
    public Identifier id() {
        return PetIntents.ANSWER_WHISTLE;
    }

    @Override
    public IntentCategory category() {
        return IntentCategory.CURIOUS;
    }

    @Override
    public Activity activity() {
        return InitActivity.ANSWER_WHISTLE.get();
    }

    @Override
    public IntentCheck canRun(IntentContext ctx) {
        return ctx.whistleHeard().isPresent() ? IntentCheck.OK : IntentCheck.fail("no_whistle");
    }

    @Override
    public float score(IntentContext ctx) {
        return SCORE;
    }

    @Override
    public Set<MemoryModuleType<?>> forgetWhenUnavailable() {
        return Set.of(InitMemory.WHISTLE_HEARD.get());
    }
}
