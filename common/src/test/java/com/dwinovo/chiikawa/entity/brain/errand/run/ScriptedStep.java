package com.dwinovo.chiikawa.entity.brain.errand.run;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

/** A step that says what it is told to, and counts what it was asked. */
final class ScriptedStep implements Step {
    private final Deque<StepStatus> script = new ArrayDeque<>();
    int begun;
    int ticked;
    int released;
    boolean interruptible = true;
    boolean holdsGaze;
    PetActivity pose = PetActivity.NONE;
    String performance = "";
    Set<StepTag> tags = Set.of();
    StepStatus onMessage = StepStatus.RUNNING;
    Message told;

    /** @return a step that goes on until it is told to say otherwise */
    static ScriptedStep running() {
        return new ScriptedStep();
    }

    /** @return a step that is done on its first tick */
    static ScriptedStep done() {
        return running().then(StepStatus.DONE);
    }

    /** Says this on the tick after the ones before it; after the last, it goes on. */
    ScriptedStep then(StepStatus status) {
        script.add(status);
        return this;
    }

    @Override
    public void begin(Handle handle) {
        begun++;
    }

    @Override
    public StepStatus tick(Handle handle) {
        ticked++;
        StepStatus next = script.poll();
        return next == null ? StepStatus.RUNNING : next;
    }

    @Override
    public StepStatus onMessage(Handle handle, Message message) {
        told = message;
        return onMessage;
    }

    @Override
    public boolean interruptible() {
        return interruptible;
    }

    @Override
    public void release(Handle handle) {
        released++;
    }

    @Override
    public boolean holdsGaze() {
        return holdsGaze;
    }

    @Override
    public PetActivity pose() {
        return pose;
    }

    @Override
    public String performance() {
        return performance;
    }

    @Override
    public Set<StepTag> tags() {
        return tags;
    }
}
