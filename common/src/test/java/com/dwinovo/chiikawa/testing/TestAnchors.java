package com.dwinovo.chiikawa.testing;

import com.dwinovo.chiikawa.entity.brain.constraint.AnchorSettings;
import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import net.minecraft.core.GlobalPos;

/**
 * Anchors of the sizes the mod ships, for tests that need an area to be as big as it is in the
 * game. The numbers are written out here because the settings are loaded from data a test does
 * not have.
 */
public final class TestAnchors {
    public static final AnchorSettings SETTINGS = new AnchorSettings(
        new AnchorSettings.Follow(7.0, 2, 12.0, 16.0, 20.0),
        new AnchorSettings.Area(13.0, 20.0),
        new AnchorSettings.Area(24.0, 32.0));

    private TestAnchors() {
    }

    /** A pet at its owner's heel. */
    public static PetAnchor following(GlobalPos owner) {
        return new PetAnchor(owner, SETTINGS.follow().reach(), SETTINGS.follow().leash(), true, true);
    }

    /** A pet roaming free round its home. */
    public static PetAnchor free(GlobalPos home) {
        return new PetAnchor(home, SETTINGS.free().reach(), SETTINGS.free().leash(), false, true);
    }

    /** A wild pet round where it spawned. */
    public static PetAnchor wild(GlobalPos home) {
        return new PetAnchor(home, SETTINGS.wild().reach(), SETTINGS.wild().leash(), false, true);
    }
}
