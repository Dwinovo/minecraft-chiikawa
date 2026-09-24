package com.dwinovo.chiikawa.gametest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * One in-game case: a static method taking the {@link net.minecraft.gametest.framework.GameTestHelper},
 * run on its template, with the cases of the same batch.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GameTest {
    /** The structure the case runs on. */
    String template();

    /** The cases run together, in a world settled once for all of them by its {@link BeforeBatch}. */
    String batch();

    /** How long the case has before it fails for want of a verdict. */
    int timeoutTicks() default 100;
}
