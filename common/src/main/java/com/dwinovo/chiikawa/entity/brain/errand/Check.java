package com.dwinovo.chiikawa.entity.brain.errand;

import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * Result of an errand's continue condition, or of why it has nothing to offer.
 *
 * @param ok whether the condition holds
 * @param reasonKey translation key explaining a failure, {@code null} when {@code ok}
 * @param lack what the pet is short of, when that is why the check failed
 */
public record Check(boolean ok, @Nullable String reasonKey, Optional<LackKey> lack) {
    public static final Check OK = new Check(true, null, Optional.empty());
    private static final String PREFIX = "intent.chiikawa.fail.";

    /**
     * @param reason the part after {@code intent.chiikawa.fail.}
     * @return a failed check with that reason
     */
    public static Check fail(String reason) {
        return new Check(false, PREFIX + reason, Optional.empty());
    }

    /**
     * @param reason the part after {@code intent.chiikawa.fail.}
     * @param lack what the pet is short of
     * @return a failed check with that reason, that says what the pet needs to make it pass
     */
    public static Check lack(String reason, LackKey lack) {
        return new Check(false, PREFIX + reason, Optional.of(lack));
    }

    /** @return the reason without the translation key around it, for the log; empty when {@code ok} */
    public String reason() {
        return reasonKey == null ? "" : reasonKey.substring(PREFIX.length());
    }
}
