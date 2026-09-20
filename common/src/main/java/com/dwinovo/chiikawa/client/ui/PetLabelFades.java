package com.dwinovo.chiikawa.client.ui;

import com.dwinovo.chiikawa.ui.Fade;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.Util;

/**
 * How far each pet's label has faded in. Client-side and no one's business but the
 * renderer's: what a label is doing this frame is not worth an entity field, let alone a
 * saved one.
 *
 * <p>Only pets being pointed at, or just now let go of, are in here — an entry that has
 * faded out is dropped, and one nobody has asked about for a while is swept up, so walking
 * away from a pet leaves nothing behind.
 */
public final class PetLabelFades {
    /** Long enough that a pet off the edge of the screen for a moment keeps its place. */
    private static final long STALE_MILLIS = 1000L;

    private static final Map<Integer, Timed> BY_ENTITY = new HashMap<>();

    private PetLabelFades() {
    }

    private record Timed(Fade fade, long millis) {
    }

    /** Where a label is now, without moving it on — for deciding whether to draw at all. */
    public static Fade current(int entityId) {
        Timed timed = BY_ENTITY.get(entityId);
        return timed == null ? Fade.HIDDEN : timed.fade();
    }

    /**
     * Moves this label on by however long it has been since the last frame it was in.
     *
     * @param shown whether the owner is asking about this pet right now
     */
    public static Fade step(int entityId, boolean shown) {
        long now = Util.getMillis();
        Timed previous = BY_ENTITY.get(entityId);
        float delta = previous == null ? 0.0F : (now - previous.millis()) / 1000.0F;
        Fade fade = (previous == null ? Fade.HIDDEN : previous.fade()).step(shown, delta);

        if (!fade.isVisible() && !shown) {
            BY_ENTITY.remove(entityId);
        } else {
            BY_ENTITY.put(entityId, new Timed(fade, now));
        }
        sweep(now);
        return fade;
    }

    /** Drops labels of pets that have stopped being rendered — unloaded, dead, left behind. */
    private static void sweep(long now) {
        BY_ENTITY.entrySet().removeIf(entry -> now - entry.getValue().millis() > STALE_MILLIS);
    }
}
