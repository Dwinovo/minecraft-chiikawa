package com.dwinovo.chiikawa.gametest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method of a {@link GameTestHolder} class as an in-game case: a public static
 * method taking the case's {@code GameTestHelper}. {@link GameTestCatalog} hands each to
 * the game as a test of its own.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GameTest {
    /** The floor the case runs on, a structure of the holder's namespace: {@code floor16} and the like. */
    String template();

    /** The cases that run under one {@link BeforeBatch}, at one time of day. */
    String batch();

    /** How long the case has to succeed, in ticks. */
    int timeoutTicks() default 100;
}
