package com.dwinovo.chiikawa.entity.brain.errand.inbox;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message.MusicStopped;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message.Nudge;
import java.util.List;
import org.junit.jupiter.api.Test;

/** What a pet is told, and when it hears it. */
class InboxTest {
    private final Inbox inbox = new Inbox();

    @Test
    void messagesComeOutInTheOrderTheyWerePosted() {
        inbox.post(new MusicStopped("a"), 5);
        inbox.post(new Message.DirectiveChanged(), 5);
        inbox.post(new MusicStopped("b"), 5);

        assertEquals(List.of(new MusicStopped("a"), new Message.DirectiveChanged(), new MusicStopped("b")), inbox.drain(5));
        assertEquals(List.of(), inbox.drain(5), "each is read once");
    }

    @Test
    void aMessageWaitsUntilItIsDue() {
        inbox.post(new MusicStopped("later"), 50);
        inbox.post(new MusicStopped("now"), 10);

        assertEquals(List.of(), inbox.drain(9));
        assertEquals(List.of(new MusicStopped("now")), inbox.drain(10));
        assertEquals(List.of(), inbox.drain(49));
        assertEquals(1, inbox.size());
        assertEquals(List.of(new MusicStopped("later")), inbox.drain(50));
    }

    @Test
    void messagesDueTogetherAreInTheOrderPostedAndOthersInTheOrderDue() {
        inbox.post(new MusicStopped("second"), 20);
        inbox.post(new MusicStopped("first"), 10);
        inbox.post(new MusicStopped("also second"), 20);

        assertEquals(List.of(new MusicStopped("first"), new MusicStopped("second"), new MusicStopped("also second")),
            inbox.drain(100));
    }

    @Test
    void nudgesWhileOneWaitsAreFoldedIntoIt() {
        inbox.post(new Nudge("fed"), 10);
        inbox.post(new Nudge("paid"), 10);
        inbox.post(new Nudge("hurt"), 12);

        assertEquals(1, inbox.size());
        assertEquals(List.of(new Nudge("fed")), inbox.drain(10), "the first cause stands");
    }

    @Test
    void aNudgeDueEarlierPullsTheWaitingOneForward() {
        inbox.post(new Nudge("fed"), 30);
        inbox.post(new Nudge("paid"), 10);

        assertEquals(1, inbox.size());
        assertEquals(List.of(new Nudge("fed")), inbox.drain(10));
    }

    @Test
    void aNudgeAfterTheOneBeforeWasReadWaitsOnItsOwn() {
        inbox.post(new Nudge("fed"), 10);
        inbox.drain(10);
        inbox.post(new Nudge("paid"), 11);

        assertEquals(List.of(new Nudge("paid")), inbox.drain(11));
    }

    @Test
    void whatIsNotANudgeIsNeverFolded() {
        inbox.post(new Message.Moved("recall"), 10);
        inbox.post(new Message.Moved("recall"), 10);

        assertEquals(2, inbox.size());
    }

    @Test
    void whenItIsFullTheOldestIsForgottenAndTheNewestKept() {
        for (int i = 0; i < Inbox.CAPACITY + 3; i++) {
            inbox.post(new MusicStopped(Integer.toString(i)), 0);
        }

        assertEquals(Inbox.CAPACITY, inbox.size());
        List<Message> read = inbox.drain(0);
        assertEquals(new MusicStopped("3"), read.get(0), "0, 1 and 2 were forgotten");
        assertEquals(new MusicStopped(Integer.toString(Inbox.CAPACITY + 2)), read.get(read.size() - 1));
    }

    @Test
    void aNudgeFoldedIntoAnotherTakesNoRoomWhenFull() {
        inbox.post(new Nudge("fed"), 0);
        for (int i = 1; i < Inbox.CAPACITY; i++) {
            inbox.post(new MusicStopped(Integer.toString(i)), 0);
        }
        inbox.post(new Nudge("paid"), 0);

        assertEquals(Inbox.CAPACITY, inbox.size());
        assertEquals(new Nudge("fed"), inbox.drain(0).get(0), "nothing was forgotten to make room for it");
    }

    @Test
    void clearForgetsEverythingDueOrNot() {
        inbox.post(new MusicStopped("now"), 0);
        inbox.post(new MusicStopped("later"), 99);

        inbox.clear();

        assertEquals(0, inbox.size());
        assertEquals(List.of(), inbox.drain(1000));
    }
}
