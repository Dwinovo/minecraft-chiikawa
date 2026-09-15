package com.dwinovo.chiikawa.entity.brain.intent;

/**
 * What kind of thing an intent does. The owner's directive permits or forbids
 * whole categories; see {@link com.dwinovo.chiikawa.entity.brain.constraint.PetConstraints#allows}.
 */
public enum IntentCategory {
    FOLLOW_OWNER,
    STAY,
    WANDER,
    WORK,
    COMBAT,
    PICK_UP
}
