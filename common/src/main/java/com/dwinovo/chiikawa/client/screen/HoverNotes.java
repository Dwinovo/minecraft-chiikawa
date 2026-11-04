package com.dwinovo.chiikawa.client.screen;

import com.dwinovo.chiikawa.ui.Rect;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * What waits under the cursor on a screen: patches of it with something to say. A screen notes them as it draws, so they are
 * where the things are, and asks which one the cursor is on.
 */
final class HoverNotes {
    private final List<Note> notes = new ArrayList<>();

    /** A patch of the screen with something to say under the cursor. */
    private record Note(Rect area, List<String> lines) {
    }

    void clear() {
        notes.clear();
    }

    void add(Rect area, List<String> lines) {
        if (!lines.isEmpty()) {
            notes.add(new Note(area, lines));
        }
    }

    /** @return what is said where the cursor is; of two patches over each other, the one noted last */
    Optional<List<String>> at(int mouseX, int mouseY) {
        List<String> found = null;
        for (Note note : notes) {
            if (note.area().contains(mouseX, mouseY)) {
                found = note.lines();
            }
        }
        return Optional.ofNullable(found);
    }
}
