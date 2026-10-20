package com.dwinovo.chiikawa.entity.brain.errand.rest;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.Outcome;
import com.dwinovo.chiikawa.entity.brain.errand.run.Running;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;

/**
 * How long a pet leaves each errand alone after it has ended, and the only thing that writes it.
 * What is written comes from the errand's settings: a number of ticks, or a named number asked of
 * the system it lives in (see {@link RestSources}).
 *
 * <p>Two ways of writing, because two things happen. An errand that ends is told by the executor and
 * only ever lengthens a rest: a photo stored while the camera is still up has already rested the
 * pet for the camera's cooldown, and an errand that gives up after it must not cut that short. A
 * report, told by a step as it does something or later by whatever was waited on, replaces: the shutter
 * rests the pet for the upload, and a photo that did not come back shortens that to the retry.
 */
public final class RestLedger {
    private static final String LOG_PREFIX = "[chiikawa-errand] ";
    private static final Set<ResourceLocation> REPORTED = ConcurrentHashMap.newKeySet();

    private final Function<ResourceLocation, RestStore> stores;
    private final Function<ResourceLocation, Optional<ErrandSettings<?>>> settings;
    private final Consumer<String> errors;

    /**
     * @param stores where each errand's rests are kept
     * @param settings how each errand goes, for the reports it is told; the settings it started with
     *                 serve for its outcome
     */
    public RestLedger(Function<ResourceLocation, RestStore> stores,
            Function<ResourceLocation, Optional<ErrandSettings<?>>> settings) {
        this(stores, settings, Constants.LOG::error);
    }

    /**
     * @param errors where to say that a rest asks a source nobody has registered, which the errand's
     *               author must be told but a pet need not be told twice
     */
    public RestLedger(Function<ResourceLocation, RestStore> stores,
            Function<ResourceLocation, Optional<ErrandSettings<?>>> settings, Consumer<String> errors) {
        this.stores = stores;
        this.settings = settings;
        this.errors = errors;
    }

    /** Forgets which sources have been said to be missing, as a reload of the data does. */
    public static void forgetReported() {
        REPORTED.clear();
    }

    /** @return whether the errand is resting as a whole */
    public boolean resting(ResourceLocation errand, long now) {
        return remaining(errand, now) > 0;
    }

    /** @return whether the errand is resting, as a whole or on this subject */
    public boolean resting(ResourceLocation errand, String subject, long now) {
        return remaining(errand, subject, now) > 0;
    }

    /** @return how many ticks the errand has left of resting as a whole, 0 for none */
    public long remaining(ResourceLocation errand, long now) {
        return stores.apply(errand).remaining(null, now);
    }

    /** @return how many ticks the errand has left of resting, as a whole or on this subject, 0 for none */
    public long remaining(ResourceLocation errand, String subject, long now) {
        RestStore store = stores.apply(errand);
        return Math.max(store.remaining(null, now), store.remaining(subject, now));
    }

    /**
     * The errand has ended. Rests as its settings say for how it ended, never for less than it was
     * already resting. For the executor alone.
     *
     * @param running the errand, as it was when it ended
     * @param outcome how it ended
     * @param now the current game time
     */
    public void afterOutcome(Running running, Outcome outcome, long now) {
        ErrandSettings.Rests rests = running.settings().rests();
        RestSpec spec = switch (outcome) {
            case Outcome.Done done -> rests.done();
            case Outcome.GaveUp gaveUp -> rests.gaveUp();
            case Outcome.Interrupted interrupted -> rests.interrupted();
        };
        long ticks = ticks(running.errand(), spec, new RestContext(running.plan().restKey()));
        if (ticks <= 0) {
            return;
        }
        String subject = spec.per() == RestSpec.Per.SUBJECT ? running.plan().subject().orElse(null) : null;
        RestStore store = stores.apply(running.errand());
        store.restFor(subject, Math.max(ticks, store.remaining(subject, now)), now);
    }

    /**
     * A step has done something the errand rests for, or something the errand was waiting on has come
     * back. Rests as the errand's settings say for that report, instead of what it was resting, whether
     * that is more or less. A report the settings do not name changes nothing. A report rests the errand
     * as a whole, whatever its other rests are on. For the mind alone.
     *
     * @param errand the errand
     * @param report what came back, as the settings name it
     * @param context what the rest is about
     * @param now the current game time
     */
    public void afterReport(ResourceLocation errand, String report, RestContext context, long now) {
        RestSpec spec = settings.apply(errand).map(found -> found.rests().reports().get(report)).orElse(null);
        if (spec != null) {
            stores.apply(errand).restFor(null, ticks(errand, spec, context), now);
        }
    }

    private long ticks(ResourceLocation errand, RestSpec spec, RestContext context) {
        return spec.amount().map(ticks -> (long) ticks, source -> RestSources.resolver(source)
            .map(resolver -> resolver.ticks(context))
            .orElseGet(() -> missing(errand, source)));
    }

    /** A source nobody registered rests for nothing, and is said to once. */
    private long missing(ResourceLocation errand, ResourceLocation source) {
        if (REPORTED.add(source)) {
            errors.accept(LOG_PREFIX + errand + " rests by " + source + ", which nothing has registered; resting for 0");
        }
        return 0L;
    }
}
