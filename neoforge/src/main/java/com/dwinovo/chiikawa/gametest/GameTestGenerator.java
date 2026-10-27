package com.dwinovo.chiikawa.gametest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method of a {@link GameTestHolder} class that makes in-game cases from data: a
 * public static method taking nothing and returning the {@link GeneratedGameTest}s.
 * {@link GameTestCatalog} hands each to the game as a test of its own, as it does a
 * {@link GameTest} method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GameTestGenerator {
}
