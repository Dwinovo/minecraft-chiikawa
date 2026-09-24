package com.dwinovo.chiikawa.gametest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A class of in-game cases, all filed under one namespace. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GameTestHolder {
    /** The namespace the cases, their batches and their floors go by. */
    String value();
}
