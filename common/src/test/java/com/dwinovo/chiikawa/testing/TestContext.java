package com.dwinovo.chiikawa.testing;

import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.entity.brain.intent.DayPhase;
import com.dwinovo.chiikawa.entity.brain.intent.IntentContext;
import com.dwinovo.chiikawa.entity.brain.intent.PerceivedTargets;
import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import com.dwinovo.chiikawa.task.PetTask;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;

/**
 * Builds an {@link IntentContext} for tests: an owned pet by day with a default
 * personality, nothing perceived and nothing carried, unless a test says otherwise.
 */
public final class TestContext {
    public static final PetOwnership OWNED = new PetOwnership.Owned(UUID.fromString("00000000-0000-0000-0000-000000000001"));

    private final GlobalPos petPos;
    private final PetAnchor anchor;
    private DayPhase phase = DayPhase.DAY;
    private PetOwnership ownership = OWNED;
    private Personality personality = Personality.DEFAULT;
    private PerceivedTargets targets = PerceivedTargets.NONE;
    private Optional<PetTask> task = Optional.empty();
    private Optional<GlobalPos> offeringBoard = Optional.empty();
    private boolean takeTaskCoolingDown;

    private TestContext(GlobalPos petPos, PetAnchor anchor) {
        this.petPos = petPos;
        this.anchor = anchor;
    }

    public static TestContext at(GlobalPos petPos, PetAnchor anchor) {
        return new TestContext(petPos, anchor);
    }

    public TestContext phase(DayPhase phase) {
        this.phase = phase;
        return this;
    }

    public TestContext ownership(PetOwnership ownership) {
        this.ownership = ownership;
        return this;
    }

    public TestContext personality(Personality personality) {
        this.personality = personality;
        return this;
    }

    public TestContext targets(PerceivedTargets targets) {
        this.targets = targets;
        return this;
    }

    public TestContext task(PetTask task) {
        this.task = Optional.of(task);
        return this;
    }

    public TestContext offeringBoard(GlobalPos board) {
        this.offeringBoard = Optional.of(board);
        return this;
    }

    public TestContext takeTaskCoolingDown() {
        this.takeTaskCoolingDown = true;
        return this;
    }

    public IntentContext build() {
        return new IntentContext(petPos, phase, ownership, personality, anchor, targets, task, offeringBoard,
            takeTaskCoolingDown, false, false, false, false);
    }
}
