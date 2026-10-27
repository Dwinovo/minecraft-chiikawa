package com.dwinovo.chiikawa.entity.brain.errand.run;

/** What a step makes of the pet that other systems have reason to ask about. */
public enum StepTag {
    /** Sat down, as at an exam desk: not to be called away by a whistle. */
    SEATED,
    /** Playing a scene with another pet. */
    IN_SCENE,
    /** Playing music for whoever is near. */
    PLAYING_MUSIC,
    /** Frightened, and running from what hurt it or cowering until it is safe. */
    SCARED,
    /** Lying down asleep: the pet does not stir for the little things, and shows it is asleep. */
    SLEEPING,
    /** Waiting to be praised by its owner, and showing that it wants to be. */
    ASKING
}
