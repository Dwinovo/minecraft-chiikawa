package com.dwinovo.chiikawa.entity.brain.errand.inbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dwinovo.chiikawa.entity.brain.errand.inbox.InvitationDesk.Engagement;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/** Who a pet says yes to when another asks it to play. */
class InvitationDeskTest {
    private static final UUID ASKER = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    private static final ResourceLocation SCENE = ResourceLocation.fromNamespaceAndPath("chiikawa", "share_a_snack");
    private static final int ASK_PRIORITY = 30;
    private static final long NOW = 100L;

    private final InvitationDesk desk = new InvitationDesk();

    @Test
    void aPetThatIsDoingNothingSaysYes() {
        assertTrue(desk.consider(invitation(ASKER, ASK_PRIORITY, 160), true, false, OptionalInt.empty(), NOW));

        assertEquals(Optional.of(new Engagement(ASKER, SCENE, 1, 160)), desk.engagement(NOW));
    }

    @Test
    void aPetPotteringAboutSaysYesToAnErrandThatMattersMore() {
        assertTrue(desk.consider(invitation(ASKER, ASK_PRIORITY, 160), true, false, OptionalInt.of(10), NOW));
    }

    @Test
    void aPetOnAnErrandOfTheSamePriorityOrMoreSaysNo() {
        assertFalse(desk.consider(invitation(ASKER, ASK_PRIORITY, 160), true, false, OptionalInt.of(30), NOW),
            "picking up is as important as the visit");
        assertFalse(desk.consider(invitation(ASKER, ASK_PRIORITY, 160), true, false, OptionalInt.of(50), NOW),
            "work is more");

        assertEquals(Optional.empty(), desk.engagement(NOW));
    }

    @Test
    void aPetItsOwnerWillNotLetPlaySaysNo() {
        assertFalse(desk.consider(invitation(ASKER, ASK_PRIORITY, 160), false, false, OptionalInt.empty(), NOW));
        assertEquals(Optional.empty(), desk.engagement(NOW));
    }

    @Test
    void aPetThatPlayedTheSameSceneWithTheSamePetLatelySaysNo() {
        assertFalse(desk.consider(invitation(ASKER, ASK_PRIORITY, 160), true, true, OptionalInt.empty(), NOW));
    }

    @Test
    void aPetAlreadyAskedByAnotherSaysNoButAnswersTheSameAskerAgain() {
        desk.consider(invitation(ASKER, ASK_PRIORITY, 160), true, false, OptionalInt.empty(), NOW);

        assertFalse(desk.consider(invitation(OTHER, ASK_PRIORITY, 170), true, false, OptionalInt.empty(), NOW + 1));
        assertEquals(ASKER, desk.engagement(NOW + 1).orElseThrow().initiator());

        assertTrue(desk.consider(invitation(ASKER, ASK_PRIORITY, 200), true, false, OptionalInt.empty(), NOW + 2));
        assertEquals(200, desk.engagement(NOW + 2).orElseThrow().expiresAt());
    }

    @Test
    void anInvitationThatHasLapsedIsNotAnswered() {
        assertFalse(desk.consider(invitation(ASKER, ASK_PRIORITY, NOW), true, false, OptionalInt.empty(), NOW));
    }

    @Test
    void whenTheAskerStopsWaitingTheEngagementIsGoneAndAnotherMayAsk() {
        desk.consider(invitation(ASKER, ASK_PRIORITY, 160), true, false, OptionalInt.empty(), NOW);

        assertTrue(desk.engagement(159).isPresent());
        assertEquals(Optional.empty(), desk.engagement(160));
        assertTrue(desk.consider(invitation(OTHER, ASK_PRIORITY, 400), true, false, OptionalInt.empty(), 160));
        assertEquals(OTHER, desk.engagement(160).orElseThrow().initiator());
    }

    @Test
    void theAskerWithdrawingClearsItButAStrangerDoesNot() {
        desk.consider(invitation(ASKER, ASK_PRIORITY, 160), true, false, OptionalInt.empty(), NOW);

        assertFalse(desk.withdrawn(OTHER));
        assertTrue(desk.engagement(NOW).isPresent());

        assertTrue(desk.withdrawn(ASKER));
        assertEquals(Optional.empty(), desk.engagement(NOW));
        assertFalse(desk.withdrawn(ASKER), "there is nothing left to withdraw");
    }

    @Test
    void clearForgetsTheEngagement() {
        desk.consider(invitation(ASKER, ASK_PRIORITY, 160), true, false, OptionalInt.empty(), NOW);

        desk.clear();

        assertEquals(Optional.empty(), desk.engagement(NOW));
    }

    private static Message.Invitation invitation(UUID from, int askPriority, long expiresAt) {
        return new Message.Invitation(from, SCENE, 1, askPriority, expiresAt);
    }
}
