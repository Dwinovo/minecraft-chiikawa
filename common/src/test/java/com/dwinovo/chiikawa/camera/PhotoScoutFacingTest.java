package com.dwinovo.chiikawa.camera;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

/** A pet with nobody to photograph faces the sun when it is low: east at sunrise, west at sunset. */
class PhotoScoutFacingTest {
    /** The level's sun angle for a time of day, as its day length gives it: a full turn is a day, noon is nought. */
    private static float sunAngle(float timeOfDay) {
        return timeOfDay * (float) (Math.PI * 2.0);
    }

    @Test
    void atSunriseItFacesEast() {
        // Day time 0 is the sun on the horizon in the east: three quarters of the way round.
        assertEquals(new Vec3(1.0, 0.0, 0.0), PhotoScout.towardsSun(sunAngle(0.75F)));
        assertEquals(new Vec3(1.0, 0.0, 0.0), PhotoScout.towardsSun(sunAngle(0.8F)), "a little after dawn");
    }

    @Test
    void atSunsetItFacesWest() {
        assertEquals(new Vec3(-1.0, 0.0, 0.0), PhotoScout.towardsSun(sunAngle(0.25F)));
        assertEquals(new Vec3(-1.0, 0.0, 0.0), PhotoScout.towardsSun(sunAngle(0.2F)), "a little before dusk");
    }
}
