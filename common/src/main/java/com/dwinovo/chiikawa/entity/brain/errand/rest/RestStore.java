package com.dwinovo.chiikawa.entity.brain.errand.rest;

import com.dwinovo.chiikawa.init.InitMemory;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.jetbrains.annotations.Nullable;

/**
 * Where one errand of one pet keeps how long it is resting. Only the {@link RestLedger} writes it.
 *
 * <p>Four kinds of storage hold the rests, see {@link Kind}; the ledger does not care which.
 */
public interface RestStore {
    /**
     * @param subject what the rest is on, or {@code null} for the errand as a whole; a store that keeps
     *                one rest for the errand ignores it
     * @param now the current game time
     * @return how many ticks are left, 0 for none
     */
    long remaining(@Nullable String subject, long now);

    /**
     * Sets the rest, whatever it was.
     *
     * @param subject what the rest is on, see {@link #remaining}
     * @param ticks how long from now it lasts; none or less ends it
     * @param now the current game time
     */
    void restFor(@Nullable String subject, long ticks, long now);

    /** Where an errand's rests live, before it is a particular pet's. */
    @FunctionalInterface
    interface Kind {
        /**
         * @param brain the pet's brain
         * @param errand the errand
         * @return that errand's rests in that pet
         */
        RestStore of(Brain<?> brain, ResourceLocation errand);
    }

    /**
     * One rest for the errand in a memory of its own that expires, as the errands that have been resting
     * since before this ledger did. A memory that is saved keeps the time that is left through a save.
     */
    static Kind memory(Supplier<MemoryModuleType<Unit>> memory) {
        return (brain, errand) -> new RestStore() {
            @Override
            public long remaining(@Nullable String subject, long now) {
                return brain.getTimeUntilExpiry(memory.get());
            }

            @Override
            public void restFor(@Nullable String subject, long ticks, long now) {
                if (ticks > 0) {
                    brain.setMemoryWithExpiry(memory.get(), Unit.INSTANCE, ticks);
                } else {
                    brain.eraseMemory(memory.get());
                }
            }
        };
    }

    /**
     * Scaffolding, deleted with {@code LegacyActivityStep} in T17: the errand's own behavior still keeps
     * its rest, in the memory or the sensor it has always used, and its start condition still reads it. So
     * the ledger has nothing to say about it: it never rests the errand and writes nothing, and there is
     * no second writer. An errand leaves this for one of the other kinds when it goes native.
     */
    static Kind legacy() {
        return (brain, errand) -> new RestStore() {
            @Override
            public long remaining(@Nullable String subject, long now) {
                return 0L;
            }

            @Override
            public void restFor(@Nullable String subject, long ticks, long now) {
            }
        };
    }

    /** The photo errand's rest, in the saved memory it has always been in. */
    static Kind photoCooldown() {
        return memory(InitMemory.PHOTO_COOLDOWN);
    }

    /** The gift errand's rest, in the saved memory it has always been in. */
    static Kind giftCooldown() {
        return memory(InitMemory.GIFT_COOLDOWN);
    }

    /** The play_music errand's rest, in the memory it has always been in; not saved. */
    static Kind musicRetry() {
        return memory(InitMemory.MUSICIAN_RESTING);
    }

    /** The shop errand's rest, in the memory it has always been in. */
    static Kind shopCooldown() {
        return memory(InitMemory.SHOP_COOLDOWN);
    }

    /** The take_task errand's rest, in the memory it has always been in; not saved. */
    static Kind takeTaskCooldown() {
        return memory(InitMemory.TAKE_TASK_COOLDOWN);
    }

    /** A rest for each thing the errand was about, in the one memory all such errands share; not saved. */
    static Kind subjects() {
        return (brain, errand) -> new RestStore() {
            @Override
            public long remaining(@Nullable String subject, long now) {
                return brain.getMemory(InitMemory.SUBJECT_RESTS.get())
                    .map(rests -> rests.remaining(errand, subject == null ? "" : subject, now)).orElse(0L);
            }

            @Override
            public void restFor(@Nullable String subject, long ticks, long now) {
                SubjectRests rests = brain.getMemory(InitMemory.SUBJECT_RESTS.get()).orElse(SubjectRests.NONE);
                brain.setMemory(InitMemory.SUBJECT_RESTS.get(), rests.with(errand, subject == null ? "" : subject, ticks, now));
            }
        };
    }

    /** One rest for the errand, in the memory of rests that every errand without a store of its own shares; saved. */
    static Kind shared() {
        return (brain, errand) -> new RestStore() {
            @Override
            public long remaining(@Nullable String subject, long now) {
                return brain.getMemory(InitMemory.ERRAND_RESTS.get())
                    .map(rests -> Math.max(0L, rests.getOrDefault(errand, 0L) - now)).orElse(0L);
            }

            @Override
            public void restFor(@Nullable String subject, long ticks, long now) {
                Map<ResourceLocation, Long> next = new HashMap<>();
                brain.getMemory(InitMemory.ERRAND_RESTS.get()).ifPresent(rests -> rests.forEach((other, end) -> {
                    if (now < end && !other.equals(errand)) {
                        next.put(other, end);
                    }
                }));
                if (ticks > 0) {
                    next.put(errand, now + ticks);
                }
                if (next.isEmpty()) {
                    brain.eraseMemory(InitMemory.ERRAND_RESTS.get());
                } else {
                    brain.setMemory(InitMemory.ERRAND_RESTS.get(), Map.copyOf(next));
                }
            }
        };
    }
}
