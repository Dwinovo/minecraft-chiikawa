package com.dwinovo.chiikawa.ui;

/**
 * Something the game can draw in an {@link UiStyle#ICON} box — an item, usually.
 *
 * <p>The library never looks inside one. It works out where the box goes and hands the
 * icon straight back to the surface that made it, which is what keeps this module free of
 * Minecraft while still laying out screens that are full of items.
 */
public interface Icon {
    /** Nothing to draw. A slot holding it is an empty well, which is a fine thing to be. */
    Icon NONE = new Icon() {
    };
}
