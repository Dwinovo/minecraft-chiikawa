package com.dwinovo.chiikawa.entity.brain.errand.inbox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * What has been told to a pet and not yet taken up, for its mind to read once a tick. Messages come
 * out in the order they are due, and those due together in the order they were posted. A message
 * may be posted for later, such as an order whistled to a yard that answers one at a time.
 *
 * <p>Bounded: a pet that is told more than {@link #CAPACITY} things before it listens forgets the
 * oldest, since what it was told last is the most likely to still be so. A {@link Message.Nudge} is
 * only worth one: another while one waits is folded into it, due at the earlier time.
 */
public final class Inbox {
    public static final int CAPACITY = 32;

    private record Entry(Message message, long dueAt) {
    }

    private final List<Entry> entries = new ArrayList<>();

    /**
     * Posts a message.
     *
     * @param message what is told
     * @param dueAt the game time it is read at the earliest; the current one for now
     */
    public void post(Message message, long dueAt) {
        if (message instanceof Message.Nudge) {
            for (int i = 0; i < entries.size(); i++) {
                Entry waiting = entries.get(i);
                if (waiting.message() instanceof Message.Nudge) {
                    if (dueAt < waiting.dueAt()) {
                        entries.set(i, new Entry(waiting.message(), dueAt));
                    }
                    return;
                }
            }
        }
        if (entries.size() == CAPACITY) {
            entries.remove(0);
        }
        entries.add(new Entry(message, dueAt));
    }

    /**
     * Takes the messages that are due.
     *
     * @param now the current game time
     * @return them, in the order they are due; the rest stay
     */
    public List<Message> drain(long now) {
        List<Entry> due = new ArrayList<>();
        for (Iterator<Entry> iterator = entries.iterator(); iterator.hasNext(); ) {
            Entry entry = iterator.next();
            if (entry.dueAt() <= now) {
                due.add(entry);
                iterator.remove();
            }
        }
        // A stable sort, so those due together keep the order they were posted in.
        due.sort(Comparator.comparingLong(Entry::dueAt));
        return due.stream().map(Entry::message).toList();
    }

    /** @return how many messages wait, due or not */
    public int size() {
        return entries.size();
    }

    /** Forgets everything, as when the pet is removed from the world. */
    public void clear() {
        entries.clear();
    }
}
