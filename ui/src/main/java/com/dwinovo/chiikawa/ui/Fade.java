package com.dwinovo.chiikawa.ui;

/**
 * How far something has faded in, and how long it still holds before fading out.
 *
 * <p>Anything switched on and off by where the cursor happens to be needs this. The
 * crosshair is never quite still — a step, a breath on the mouse, and it grazes the edge
 * of what it was on — so a label tied straight to it blinks. Fading in says "this appeared
 * because you asked"; holding for a moment before fading out forgives the graze.
 *
 * <p>Out is slower than in: arriving should feel prompt, leaving should not look like a
 * glitch. The hold is shorter than either, long enough to cover a wobble and too short to
 * feel like the label is refusing to leave.
 */
public record Fade(float alpha, float holdLeft) {
    /** Not shown at all, and nothing owed. */
    public static final Fade HIDDEN = new Fade(0.0F, 0.0F);

    public static final float IN_SECONDS = 0.12F;
    public static final float HOLD_SECONDS = 0.15F;
    public static final float OUT_SECONDS = 0.25F;

    /**
     * @param shown whether the thing is being asked for right now
     * @param deltaSeconds time since this was last worked out
     */
    public Fade step(boolean shown, float deltaSeconds) {
        float delta = Math.max(0.0F, deltaSeconds);
        if (shown) {
            return new Fade(Math.min(1.0F, alpha + delta / IN_SECONDS), HOLD_SECONDS);
        }
        float afterHold = delta - holdLeft;
        if (afterHold <= 0.0F) {
            return new Fade(alpha, holdLeft - delta);
        }
        // Whatever is left of the step once the hold is used up starts the fade out, so a
        // long frame cannot swallow a whole hold and leave the fade untouched.
        return new Fade(Math.max(0.0F, alpha - afterHold / OUT_SECONDS), 0.0F);
    }

    public boolean isVisible() {
        return alpha > 0.0F;
    }
}
