package com.dwinovo.chiikawa.entity.brain.errand.view;

import com.dwinovo.chiikawa.camera.PhotoOutlook;
import com.dwinovo.chiikawa.entity.brain.constraint.AnchorSettings;
import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import com.dwinovo.chiikawa.qualification.PetExams;
import com.dwinovo.chiikawa.task.PetTask;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;

/**
 * What an errand may know of a pet when it is only being considered: read-only questions with
 * plain answers. It holds no entity, no brain and no random, and has no setter, so an errand
 * cannot change the world while it is weighed, nor answer two ways to the same question; the
 * live pet's answers are worked out only when asked and kept for one evaluation, see
 * {@link LivePetView}. Tests build one by hand.
 *
 * <p>It holds the anchor derived from the directive but never the directive itself, so that no
 * errand bypasses the permission table.
 */
public interface PetView {
    /** @return the pet's block position */
    GlobalPos pos();

    /** @return the part of the day in the pet's level */
    DayPhase phase();

    /** @return who the pet belongs to */
    PetOwnership ownership();

    /** @return how the pet's kind leans */
    Personality personality();

    /** @return where the pet may act */
    PetAnchor anchor();

    /** @return how far a pet at heel walks back from, as the anchor settings have it */
    AnchorSettings.Follow follow();

    /** @return what the sensors remember */
    PerceivedTargets targets();

    /** @return the slip the pet carries */
    Optional<PetTask> slip();

    /** @return where its owner is, while the owner is a player in the pet's level */
    Optional<GlobalPos> ownerPos();

    /** @return whether the pet is still in the mood after a proper meal */
    boolean eager();

    /** @return the nearest labor board, while it has a slip the pet would take */
    Optional<GlobalPos> offeringBoard();

    /** @return the nearest shop, while it sells something the pet likes and can afford */
    Optional<GlobalPos> shopWorthVisiting();

    /** @return the pet this one has thought of playing a scene with */
    Optional<GlobalPos> socialPartner();

    /** @return whether another pet is on its way to play a scene with this one */
    boolean playingAlong();

    /** @return whether the pet has something for its owner that it has not handed over */
    boolean carryingGift();

    /** @return whether the pet has something in its bag to give its owner, and is ready to think of giving them something */
    boolean presentToGive();

    /** @return whether the pet's attack cooldown is running */
    boolean attackCoolingDown();

    /** @return whether the pet carries arrows */
    boolean hasArrows();

    /** @return whether the held music box selects a song the pet would play now */
    boolean hasPlayableSelection();

    /** @return whether the pet is performing */
    boolean playingMusic();

    /** @return the exam the pet is signed up for and the desk it is to sit it at, while it can still sit it there */
    Optional<PetExams.Summons> examToSit();

    /** @return the exam desk the pet sat an exam at, while it has the results to go and see there this morning */
    Optional<GlobalPos> resultsDesk();

    /** @return whether the pet is in its chair at an exam desk, at the paper or handing it in, by what its errand says */
    boolean seatedAtExam();

    /** @return where a whistle was blown that this wild pet has set off for, once it has had time to react */
    Optional<GlobalPos> whistleHeard();

    /** @return whether the pet could take a photo now, and of what */
    PhotoOutlook photo();

    /**
     * @param item the id of an item the pet means to pick up
     * @return that item, while it is still there and still one the pets pick up; asked afresh of the world each time, not
     *         taken from what the sensor remembers, which may be a few ticks behind
     */
    Optional<PickableItem> pickable(UUID item);

    /**
     * @param creature the id of a creature, such as the one a pet means to photograph
     * @return where it is now, while it is alive in the pet's level; asked afresh of the world each time, since it moves
     */
    Optional<GlobalPos> whereIs(UUID creature);
}
