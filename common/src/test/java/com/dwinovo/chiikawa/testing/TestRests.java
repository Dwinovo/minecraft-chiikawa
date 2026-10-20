package com.dwinovo.chiikawa.testing;

import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestLedger;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestSpec;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/** Errand settings and rest storage for tests of what runs an errand, without a pet or a data pack. */
public final class TestRests {
    private TestRests() {
    }

    /**
     * @return settings of an errand with no tuning, that gives up after {@code maxTicks} and rests as given
     */
    public static ErrandSettings<String> settings(int maxTicks, ErrandSettings.Rests rests) {
        return new ErrandSettings<>(50, 0.5F, maxTicks, rests, List.of(), List.of(), Optional.empty(), "");
    }

    /** @return rests of these many ticks for each way an errand ends */
    public static ErrandSettings.Rests ticks(int done, int gaveUp, int interrupted) {
        return ErrandSettings.Rests.of(RestSpec.ticks(done), RestSpec.ticks(gaveUp), RestSpec.ticks(interrupted));
    }

    /** @return a ledger in which no errand is resting, and nothing it is told is kept */
    public static RestLedger noRests() {
        return new RestLedger(errand -> new Store(), errand -> Optional.empty());
    }

    /** A store that keeps its rests in a map, one for the errand and one for each subject, and counts what is written. */
    public static final class Store implements RestStore {
        private final Map<String, Long> until = new HashMap<>();
        private int writes;

        @Override
        public long remaining(@Nullable String subject, long now) {
            return Math.max(0L, until.getOrDefault(subject == null ? "" : subject, 0L) - now);
        }

        @Override
        public void restFor(@Nullable String subject, long ticks, long now) {
            writes++;
            until.put(subject == null ? "" : subject, now + ticks);
        }

        /** @return how many times a rest was written */
        public int writes() {
            return writes;
        }
    }
}
