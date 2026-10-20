package com.dwinovo.chiikawa.entity.brain;

import com.dwinovo.chiikawa.camera.PhotoResults;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.constraint.PetConstraints;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandType;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandView;
import com.dwinovo.chiikawa.entity.brain.errand.Errands;
import com.dwinovo.chiikawa.entity.brain.errand.Outcome;
import com.dwinovo.chiikawa.entity.brain.errand.decide.Continuation;
import com.dwinovo.chiikawa.entity.brain.errand.decide.Decision;
import com.dwinovo.chiikawa.entity.brain.errand.decide.DecisionLog;
import com.dwinovo.chiikawa.entity.brain.errand.decide.DecisionTrace;
import com.dwinovo.chiikawa.entity.brain.errand.decide.Permits;
import com.dwinovo.chiikawa.entity.brain.errand.impl.CheckResultsErrand;
import com.dwinovo.chiikawa.entity.brain.errand.impl.TakeExamErrand;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Cue;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Inbox;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.InvitationDesk;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestContext;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestLedger;
import com.dwinovo.chiikawa.entity.brain.errand.run.Executor;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Running;
import com.dwinovo.chiikawa.entity.brain.errand.step.LiveHandle;
import com.dwinovo.chiikawa.entity.brain.errand.view.DayPhase;
import com.dwinovo.chiikawa.entity.brain.errand.view.LivePetView;
import com.dwinovo.chiikawa.init.InitRegistry;
import com.dwinovo.chiikawa.qualification.ExamStage;
import com.dwinovo.chiikawa.qualification.PetExams;
import com.dwinovo.chiikawa.task.SlipWages;
import com.dwinovo.chiikawa.voice.PetSpeech;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * What decides what one pet does, and the only thing that does: it listens to what the pet is told,
 * decides what to do about it, and has the {@link Executor} do it. One for each pet, made with it.
 *
 * <p>Every {@link #DECISION_INTERVAL} ticks (staggered by entity id), right after the pet is told
 * something that may change its mind, or whenever it is on no errand, it ends the errand it is on if
 * its capability no longer offers it, its directive no longer permits it, it no longer wants to go on
 * or what it heads for is out of the leash, then looks at the generic errands plus those of its
 * capability. A pet goes on with what it is doing unless an errand of a strictly higher priority can
 * start; otherwise it starts the best of the highest priority there is. How much each matters is
 * data, see {@link ErrandSettings}.
 *
 * <p>Runs before the brain ticks, so that the walk and the gaze it sets are carried out by the sinks
 * in the same tick. An errand that ends done is followed by the next in the same tick; one that is cut short or
 * gives up is followed at the pet's next.
 *
 * <p>What a pet is told is handled in two halves, because the inbox is lost with the pet when it is
 * removed from the world, a chunk unloading included. The first half is {@link #keep}, run as the pet is
 * told, on the server thread: whatever the message makes durable, what is saved with the pet or the world,
 * is written there and then, and only there. A rest is saved with the pet, so the rest a message makes the
 * pet take is part of that half. The second half is {@link #handle}, run when the pet reads its inbox at its
 * next tick: what concerns deciding and the errand under way, a step's ending, a nudge. A message with a
 * first half is a {@link Message.Durable} one, and is queued for the second as {@link Message.Settled}.
 */
public final class PetMind {
    public static final int DECISION_INTERVAL = 10;

    /**
     * A look at the pet's mind that changes nothing in it and draws nothing from its random.
     *
     * @param running the errand the pet is on
     * @param gameTime the current game time
     * @param phase the part of the day
     * @param trace how each errand fared in a decision made now, if the pet were on none; the errands of
     *              every priority are in it, not only those down to the first that could start
     */
    public record Look(Optional<Running> running, long gameTime, DayPhase phase, DecisionTrace trace) {
    }

    private final AbstractPet pet;
    private final Inbox inbox = new Inbox();
    private final InvitationDesk desk = new InvitationDesk();
    private final DecisionLog log = new DecisionLog();
    private final RestLedger rests;
    private final Handle handle;
    private final Executor executor;
    private final ErrandView errands;
    /** Set when the pet has been told something that may change its mind, until it next decides. */
    private boolean decideNow;

    /** @param pet the pet, which shows what the mind derives: see {@code AbstractPet#intent} */
    public PetMind(AbstractPet pet) {
        this.pet = pet;
        this.rests = new RestLedger(
            errand -> Errands.get(errand).restStore().of(pet.getBrain(), errand),
            errand -> Optional.ofNullable(ErrandSettings.all().get(errand)));
        this.handle = new LiveHandle(pet);
        this.executor = new Executor(handle, pet, rests, log);
        this.errands = ErrandView.of(executor, desk);
    }

    /**
     * Tells a pet something. The only way in from outside: what the message makes durable is applied now,
     * which is why this is for the server thread, and the rest is read at the pet's next tick. A no-op on
     * a client, and on a pet whose mind is off it waits in the inbox, which holds the latest few.
     *
     * @throws IllegalStateException for a message with something to keep, told from another thread
     */
    public static void post(AbstractPet pet, Message message) {
        if (pet.level().isClientSide()) {
            return;
        }
        PetMind mind = pet.getMind();
        Message queued = message instanceof Message.Durable durable ? mind.keep(durable) : message;
        mind.inbox.post(queued, pet.level().getGameTime());
    }

    /**
     * Has a pet do something at once and in passing, which is not queued and moves nothing of its body: a pet
     * whose own mind is off does it too. A no-op on a client.
     */
    public static void cue(AbstractPet pet, Cue cue) {
        if (!pet.level().isClientSide()) {
            pet.getMind().apply(cue);
        }
    }

    /**
     * A step of the errand the pet is on reports how it is faring: the pet rests the errand as its settings say
     * for the report, at once. For the executor's steps alone, through their handle.
     */
    public void report(String report) {
        executor.running().ifPresent(running -> rests.afterReport(running.errand(), report,
            new RestContext(running.plan().restKey()), pet.level().getGameTime()));
    }

    /** Has the pet decide again at its next tick instead of waiting for its turn. */
    public static void nudge(AbstractPet pet, String cause) {
        post(pet, new Message.Nudge(cause));
    }

    /** @return what the pet is doing, as the systems around it may ask */
    public ErrandView errands() {
        return errands;
    }

    /** @return the pet's most recent errands and what became of them */
    public DecisionLog log() {
        return log;
    }

    /** Forgets what the pet was told and has said yes to, as when it is removed from the world. */
    public void remove() {
        inbox.clear();
        desk.clear();
    }

    public void tick(ServerLevel level) {
        long now = level.getGameTime();
        for (Message message : inbox.drain(now)) {
            handle(message, now);
        }
        boolean idle = executor.running().isEmpty();
        boolean due = decideNow || (now + pet.getId()) % DECISION_INTERVAL == 0;
        if (idle || (due && executor.interruptible())) {
            String trigger = idle ? "no_errand" : decideNow ? "told" : "periodic";
            decideNow = false;
            decide(now, trigger);
        }
        executor.tick(now);
        if (executor.running().isEmpty() && endedDoneIn(now)) {
            // An errand that did what it set out to hands over to the next in the tick it ends, so that a pet that goes
            // from one short errand to the next, such as one habit to another, is never on none for a tick: the player
            // is told what it is doing, and the checks that ask are answered, in every tick.
            decide(now, "done");
        }
    }

    private boolean endedDoneIn(long now) {
        return log.lastEnded().filter(ended -> ended.gameTime() == now && ended.outcome() instanceof Outcome.Done).isPresent();
    }

    /** @return how each errand would fare now, without changing anything */
    public Look look(ServerLevel level) {
        LivePetView view = LivePetView.of(pet);
        DecisionTrace trace = new DecisionTrace();
        Decision.decide(offered(), view, permits(view), rests, Integer.MIN_VALUE, null, level.getGameTime(), trace);
        return new Look(executor.running(), level.getGameTime(), view.phase(), trace);
    }

    /**
     * Whoever is photographed turns to the camera, if it is not looking at something of its own, strikes a pose and says
     * something; whoever has been paid shows it.
     */
    private void apply(Cue cue) {
        switch (cue) {
            case Cue.Pose pose -> {
                if (pet.level() instanceof ServerLevel level && level.getEntity(pose.photographer()) instanceof LivingEntity photographer) {
                    executor.glance(photographer, pose.lookTicks());
                }
                handle.gesture(pose.gesture());
                handle.say(pose.line());
            }
            case Cue.Paid paid -> SlipWages.rejoice(pet);
        }
    }

    private void handle(Message message, long now) {
        switch (message) {
            case Message.Nudge nudge -> decideNow = true;
            case Message.DirectiveChanged changed -> decideNow = true;
            case Message.CapabilityChanged changed -> decideNow = true;
            case Message.Hurt hurt -> decideNow = true;
            case Message.Moved moved -> {
                executor.requestEnd(Outcome.interrupted("moved"));
                decideNow = true;
            }
            case Message.Settled settled -> settled(settled, now);
            case Message.Durable durable -> throw new IllegalStateException(durable + " was queued without being kept");
            default -> executor.deliver(message, now);
        }
    }

    /**
     * The first half of what a pet is told that has something to keep: applies what it makes durable, the rests
     * it makes the pet take and the changes to its licences included, on the server thread, and leaves what is
     * left of it to do at the pet's next tick.
     */
    private Message.Settled keep(Message.Durable message) {
        MinecraftServer server = pet.level().getServer();
        if (server != null && !server.isSameThread()) {
            throw new IllegalStateException("Told " + message + " from another thread; hop onto the server's with execute first");
        }
        long now = pet.level().getGameTime();
        Message.Durable kept = switch (message) {
            case Message.PhotoResult result -> {
                rests.afterReport(ErrandIds.TAKE_PHOTO, PhotoResults.received(pet, result), RestContext.of(result.camera()), now);
                yield result;
            }
            case Message.ExamCalledOff calledOff -> {
                PetExams.excuse(pet, calledOff.id());
                yield calledOff;
            }
            // A pet on its way to hear them at the desk hears them there, once it has read what is on it.
            case Message.ResultsDue due -> new Message.ResultsDue(due.id(),
                visitingResultsOf(due.id()) ? Optional.empty() : PetExams.grant(pet, due.id()));
            case Message.SlipCompleted completed -> {
                SlipWages.pay(pet, completed.task());
                yield completed;
            }
        };
        return new Message.Settled(kept);
    }

    /** The second half of what a pet is told that has something to keep. */
    private void settled(Message.Settled settled, long now) {
        switch (settled.message()) {
            // The photo errand may be holding the pose for it, and puts the camera away.
            case Message.PhotoResult result -> executor.deliver(result, now);
            case Message.ExamCalledOff calledOff -> {
                PetExams.tellCalledOff(pet, calledOff.id(), calledOff.reason());
                // A pet at the paper of an exam that is off gets up from it.
                errands.running().map(Running::plan)
                    .filter(plan -> plan instanceof TakeExamErrand.Sitting sitting && sitting.qualification().equals(calledOff.id()))
                    .ifPresent(plan -> executor.requestEnd(Outcome.interrupted("called_off")));
                decideNow = true;
            }
            case Message.ResultsDue due -> {
                due.heard().ifPresent(result -> PetExams.show(pet, due.id(), result));
                decideNow = true;
            }
            // Paid, it can go for another slip.
            case Message.SlipCompleted completed -> decideNow = true;
        }
    }

    /** @return whether the pet is on its way to the desk the results of that exam are up at */
    private boolean visitingResultsOf(ResourceLocation exam) {
        Optional<GlobalPos> desk = pet.licences().get(exam).paper().map(ExamStage.Sat::desk);
        return errands.running().map(Running::plan)
            .filter(plan -> plan instanceof CheckResultsErrand.Visit visit && Optional.of(visit.desk()).equals(desk))
            .isPresent();
    }

    private void decide(long now, String trigger) {
        LivePetView view = LivePetView.of(pet);
        Permits permits = permits(view);
        List<ErrandType<?, ?>> offered = offered();
        forgetUnavailable(offered, permits);

        Optional<Running> was = executor.running();
        ErrandType<?, ?> previous = justBefore(was, log, now);
        String cause = trigger;
        int floor = Integer.MIN_VALUE;
        if (was.isPresent()) {
            Optional<Outcome> end = Continuation.endOf(was.get(), previous, offered.contains(previous), view, permits);
            if (end.isPresent()) {
                if (!executor.end(end.get(), now)) {
                    return;
                }
                cause = describe(end.get());
            } else {
                floor = was.get().priority();
            }
        }

        Optional<Decision.Started> started = Decision.decide(offered, view, permits, rests, floor, pet.getRandom(), now, null);
        if (started.isPresent()) {
            if (executor.running().isPresent()) {
                if (!executor.end(Outcome.interrupted("higher"), now)) {
                    return;
                }
                cause = "higher";
            }
            start(started.get(), previous, now);
            log.switched(now, was.map(Running::errand).orElse(null), started.get().type().id(), cause, started.get().ranking());
        } else if (was.isPresent() && executor.running().isEmpty()) {
            log.switched(now, was.get().errand(), null, cause, List.of());
        }
    }

    /**
     * @return the errand the pet was on a moment ago: the one it is on, or the one that ended in the tick before, since an
     *         errand that ends by itself, such as a fight that is over for a foe, leaves the pet on none when it decides
     */
    @Nullable
    static ErrandType<?, ?> justBefore(Optional<Running> was, DecisionLog log, long now) {
        return was.map(Running::errand)
            .or(() -> log.lastEnded().filter(ended -> ended.gameTime() >= now - 1).map(DecisionLog.Ended::errand))
            .map(Errands::get).orElse(null);
    }

    private void start(Decision.Started started, @Nullable ErrandType<?, ?> previous, long now) {
        executor.start(started.type().id(), started.offer().plan(), started.settings(), started.routine(), now);
        // Setting off to fight, rather than turning from one way of fighting to another, is when a pet
        // shouts its battle cry.
        started.settings().startVoice().ifPresent(voice -> {
            if (previous == null || previous.category() != started.type().category()) {
                PetSpeech.say(pet, voice);
            }
        });
    }

    private Permits permits(LivePetView view) {
        return category -> PetConstraints.allows(pet, view.ownership(), category);
    }

    /** The generic errands plus those of the pet's capability. */
    private List<ErrandType<?, ?>> offered() {
        List<ErrandType<?, ?>> offered = new ArrayList<>(Errands.generic());
        for (ResourceLocation id : InitRegistry.getCapabilityFromId(pet.getPetJobId()).errands()) {
            offered.add(Errands.get(id));
        }
        return offered;
    }

    /** Erases the progress memories of errands the pet can currently not pursue. */
    private void forgetUnavailable(List<ErrandType<?, ?>> offered, Permits permits) {
        for (ErrandType<?, ?> type : Errands.all()) {
            if (type.forgetWhenUnavailable().isEmpty()) {
                continue;
            }
            ErrandCategory category = type.category();
            boolean available = offered.contains(type) && permits.allows(category)
                && ErrandSettings.of(type.id()).isPresent();
            if (!available) {
                type.forgetWhenUnavailable().forEach(pet.getBrain()::eraseMemory);
            }
        }
    }

    private static String describe(Outcome outcome) {
        return switch (outcome) {
            case Outcome.Done done -> "done";
            case Outcome.GaveUp gaveUp -> gaveUp.reason();
            case Outcome.Interrupted interrupted -> interrupted.cause();
        };
    }
}
