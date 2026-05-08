package com.dwinovo.chiikawa.anim.state;

/**
 * Code-bounded sustained loop activity. The third animation-need bucket
 * (alongside one-shot {@link PetAction}/{@link PetReaction} edge events and
 * the always-running decorative loops registered via
 * {@link com.dwinovo.chiikawa.anim.render.ChiikawaEntityRenderer#addLoopingController}).
 *
 * <h2>Why this exists</h2>
 * Short events ({@code triggerAction(HARVEST)}) play the harvest swing exactly
 * once and stop — duration is decided by the animation's baked length.
 * Decorative loops (blink, breath) play forever — duration is "always". This
 * enum covers the third case: <em>looping</em> animations whose duration is
 * set by gameplay code (a Brain behavior runs for N seconds; a hold-disc
 * session expires after a server-side timer; an "eating" task ends when the
 * food is consumed).
 *
 * <p>The mainstream pattern across vanilla Minecraft (Allay's
 * {@code dancingAnimationState} + {@code DATA_DANCING}, Sniffer's
 * {@code DIGGING} pose), Bedrock {@code animation_controllers}'
 * {@code query.is_purring}-style transitions, and GeckoLib mods'
 * {@code setAnimation(thenLoop)} + synced bool is a synced flag plus a
 * predicate that reads it. {@link PetActivity} is that flag.
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>Server-side Brain behavior calls {@code pet.setActivity(X)} in its
 *       {@code start()}.</li>
 *   <li>The synced byte propagates to the client (
 *       {@link com.dwinovo.chiikawa.entity.AbstractPet}'s
 *       {@code SynchedEntityData}).</li>
 *   <li>The client's
 *       {@link PetAnimationResolver} returns the activity's animation name
 *       as the highest-priority candidate.</li>
 *   <li>The main controller crossfades into the loop.</li>
 *   <li>When the behavior ends (timeout, condition no longer met,
 *       interrupted), {@code stop()} calls {@code pet.setActivity(NONE)}.</li>
 *   <li>Resolver falls back to mode/locomotion-derived idle; main controller
 *       crossfades out.</li>
 * </ol>
 *
 * <h2>Why not have a startLoop / stopLoop API instead</h2>
 * Researched all major systems (vanilla, Bedrock, GeckoLib, YSM, Doggy
 * Talents Next): zero of them use imperative start/stop. Reasons:
 * <ul>
 *   <li>Idempotency: handler returns the same value every tick, no
 *       "double-start" bookkeeping needed.</li>
 *   <li>Reconnect-safe: synced flags reach a fresh client on first packet.
 *       A start packet missed during disconnect would leave the loop never
 *       starting.</li>
 *   <li>Network ordering: only the current state is on the wire; out-of-order
 *       arrivals are impossible because there's only one value.</li>
 *   <li>The fade-in / fade-out is the existing {@code main} controller's
 *       crossfade, free.</li>
 * </ul>
 *
 * <p>Network IDs are bucketed by category (10 = personality / interaction,
 * future ranges reserved: 20-29 social, 30-39 vital). Append-only — once a
 * value ships, its network ID is frozen. Add new values at the end of the
 * relevant range; never reorder.
 */
public enum PetActivity {
    /**
     * No code-bounded activity in progress; resolver falls through to
     * mode/locomotion-derived idle.
     *
     * <p>This is currently the only value — the framework is in place
     * (synced byte, resolver short-circuit, getActivity/setActivity API),
     * but no specific activities are wired up yet. Add new values here as
     * features need them, with stable network IDs in 10-unit bands
     * (10-19 personality, 20-29 social, 30-39 vital, etc.). Append-only:
     * once a value ships, its network ID is frozen.
     */
    NONE(0, null);

    private static final PetActivity[] BY_NETWORK_ID = buildLookup();

    private final int networkId;
    /** Resolver-side animation name to play, or {@code null} for {@link #NONE}. */
    private final String animationName;

    PetActivity(int networkId, String animationName) {
        this.networkId = networkId;
        this.animationName = animationName;
    }

    public int networkId() {
        return networkId;
    }

    /** Animation file name to look up for this activity, or {@code null} when no activity is set. */
    public String animationName() {
        return animationName;
    }

    /** Decode a synced byte into a {@link PetActivity}. Unknown ids fall back to {@link #NONE}. */
    public static PetActivity fromNetworkId(int id) {
        if (id < 0 || id >= BY_NETWORK_ID.length) return NONE;
        PetActivity a = BY_NETWORK_ID[id];
        return a == null ? NONE : a;
    }

    private static PetActivity[] buildLookup() {
        int max = 0;
        for (PetActivity a : values()) max = Math.max(max, a.networkId);
        PetActivity[] out = new PetActivity[max + 1];
        for (PetActivity a : values()) out[a.networkId] = a;
        return out;
    }
}
