package com.dwinovo.chiikawa.qualification;

import net.minecraft.util.Mth;

/**
 * A pet's odds of passing its next grade, piece by piece, so an owner signing it up can see
 * what they come from: the grade's own odds, plus what practice, failing and the book add,
 * all scaled by how the pet takes to the licence, and never above the best the licence
 * allows.
 *
 * @param base the grade's own odds
 * @param practice what the slips practised since the last exam add
 * @param failing what the exams failed since the last pass add
 * @param book what having read the book adds, for this pet
 * @param aptitude what this pet's odds are multiplied by
 * @param max the best odds anyone gets
 */
public record PassOdds(float base, float practice, float failing, float book, float aptitude, float max) {
    /** @return the chance of passing, 0 to 1 */
    public float chance() {
        return Mth.clamp((base + practice + failing + book) * aptitude, 0.0F, max);
    }
}
