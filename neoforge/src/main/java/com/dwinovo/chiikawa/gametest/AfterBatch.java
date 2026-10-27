package com.dwinovo.chiikawa.gametest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method of a {@link GameTestHolder} class that puts the world back after a batch
 * of cases has run: a public static method taking the {@code ServerLevel}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AfterBatch {
    /** The batch it puts the world back for. */
    String batch();
}
