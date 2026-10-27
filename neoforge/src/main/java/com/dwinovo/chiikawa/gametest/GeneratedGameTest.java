package com.dwinovo.chiikawa.gametest;

import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * One case a {@link GameTestGenerator} makes: what a {@link GameTest} method says in its
 * annotation, and the case itself.
 *
 * @param name the case's name, as a method of the holder would be named
 * @param batch the cases that run under one {@link BeforeBatch}, at one time of day
 * @param template the floor the case runs on, a structure of the holder's namespace: {@code floor16} and the like
 * @param timeoutTicks how long the case has to succeed, in ticks
 * @param body the case
 */
public record GeneratedGameTest(String name, String batch, String template, int timeoutTicks,
                                Consumer<GameTestHelper> body) {
}
