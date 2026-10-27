package com.dwinovo.chiikawa.entity.brain.errand.run;

/**
 * The small picture over a pet's head that says what is on its mind, for everyone who looks at it: not a thing it does,
 * and so never in the way of what it does. Worked out by {@link PetIcons} and published by the executor alone.
 */
public enum PetIcon {
    /** Nothing to show. */
    NONE,
    /** It has a present for its owner, in its paws or in mind and about to be. */
    GIFT,
    /** It has no work to do and is looking for some. */
    SEEKING_WORK,
    /** Frightened, and running from or hiding from what hurt it. */
    SCARED,
    /** It is short of something its work needs. */
    MISSING,
    /** It is asleep. */
    SLEEPY;

    /** @return the icon with this id on the wire, none for an id nobody has */
    public static PetIcon byId(int id) {
        PetIcon[] all = values();
        return id >= 0 && id < all.length ? all[id] : NONE;
    }

    /** @return this icon's id on the wire; the icons are not saved, so only this build's server and client need agree */
    public byte id() {
        return (byte) ordinal();
    }
}
