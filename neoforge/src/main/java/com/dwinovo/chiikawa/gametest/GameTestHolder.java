package com.dwinovo.chiikawa.gametest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A class of in-game cases, filed under a namespace. The game stopped declaring its cases
 * by annotation in 1.21.5 and NeoForge's own holder went with it; {@link GameTestRegistration}
 * finds these and hands the game what it now asks for.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GameTestHolder {
    /** The namespace the cases and their templates are under. */
    String value();
}
