package com.dwinovo.chiikawa.gametest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method of a {@link GameTestHolder} class that settles the world before a batch
 * of cases runs: a public static method taking the {@code ServerLevel}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface BeforeBatch {
    /** The batch it settles the world for. */
    String batch();
}
