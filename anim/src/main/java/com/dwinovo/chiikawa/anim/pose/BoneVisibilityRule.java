package com.dwinovo.chiikawa.anim.pose;

/**
 * Predicate over per-frame state that decides whether a named bone should be
 * drawn. Registered against a {@link BoneVisibility}, evaluated once per frame
 * when the frame's state is captured, the result kept as the frame's hidden-bone
 * flags.
 *
 * <p>This is the long-term home for "this bone shows when X" logic — it does
 * not require modifying the {@code .geo.json} or {@code .animation.json}
 * (Blockbench's default export stays untouched), and it does not require a
 * Molang variable bridge to be authored alongside each animation. The rule is
 * a pure Java lambda over the same state the rest of the pipeline reads, so it
 * composes naturally with controllers and interceptors.
 *
 * <h2>Bedrock spec note</h2>
 * The Bedrock geometry schema does <em>not</em> declare a {@code visibility}
 * field on bones, and animation channels are restricted to {@code rotation /
 * position / scale} — there is no visibility channel. Mojang's canonical
 * mechanism is {@code render_controllers.json}'s {@code part_visibility} +
 * Molang. Our rule API is the GeckoLib v5 equivalent (per-frame flag driven
 * from game state), without Molang's parsing overhead.
 *
 * <h2>Default semantics</h2>
 * Bones with no rule registered default to <em>visible</em>. A bone with one
 * or more rules is visible when <em>any</em> rule returns {@code true} (OR
 * semantics — registering multiple rules expands the conditions a bone can
 * appear under). To make a bone "hidden by default, shown only under
 * condition X", register a single rule whose body is exactly that condition;
 * the absence of a vote means hidden.
 *
 * <h2>Children</h2>
 * Hiding a bone hides its entire subtree — the model drawer skips a hidden
 * bone without recursing into its children. This matches the intent
 * 90% of the time (hide a guitar bone → its strings/tuners children also
 * disappear) and is more conservative than GeckoLib v5's split
 * {@code skipRender / skipChildrenRender} flags, which is a known footgun.
 *
 * @param <S> the caller's per-frame state the rule reads, see {@link BoneVisibility}
 */
@FunctionalInterface
public interface BoneVisibilityRule<S> {
    /**
     * @return {@code true} if the bone should render this frame; {@code false}
     *         to vote for hiding it
     */
    boolean isVisible(S state);
}
