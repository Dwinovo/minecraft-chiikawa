package com.dwinovo.chiikawa.entity.brain;

/**
 * Short-lived AI intents requested by player interaction or other systems.
 *
 * <p>A command is not the animation state itself. Behaviors consume commands
 * from brain memory, decide whether they can run, and then may set a
 * {@link com.dwinovo.chiikawa.anim.state.PetActivity}, trigger an action, or
 * perform some other gameplay effect.
 */
public enum PetCommand {
    PLAY_MUSIC
}
