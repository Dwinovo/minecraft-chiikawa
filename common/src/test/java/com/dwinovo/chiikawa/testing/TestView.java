package com.dwinovo.chiikawa.testing;

import com.dwinovo.chiikawa.camera.PhotoOutlook;
import com.dwinovo.chiikawa.entity.brain.constraint.AnchorSettings;
import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.entity.brain.errand.view.DayPhase;
import com.dwinovo.chiikawa.entity.brain.errand.view.PerceivedTargets;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.entity.brain.errand.view.PickableItem;
import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import com.dwinovo.chiikawa.task.PetTask;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;

/**
 * A {@link PetView} built by hand for tests: an owned pet by day with a default personality,
 * nothing perceived and nothing carried, unless a test says otherwise.
 */
public final class TestView implements PetView {
    public static final PetOwnership OWNED = new PetOwnership.Owned(UUID.fromString("00000000-0000-0000-0000-000000000001"));

    private final GlobalPos pos;
    private final PetAnchor anchor;
    private DayPhase phase = DayPhase.DAY;
    private PetOwnership ownership = OWNED;
    private Personality personality = Personality.DEFAULT;
    private PerceivedTargets targets = PerceivedTargets.NONE;
    private Optional<PetTask> slip = Optional.empty();
    private Optional<GlobalPos> offeringBoard = Optional.empty();
    private Optional<GlobalPos> shopWorthVisiting = Optional.empty();
    private boolean takeTaskCoolingDown;
    private boolean shopCoolingDown;
    private boolean carryingGift;
    private Optional<GlobalPos> socialPartner = Optional.empty();
    private boolean playingAlong;
    private boolean eager;
    private boolean attackCoolingDown;
    private boolean hasArrows;
    private boolean hasPlayableSelection;
    private boolean playingMusic;
    private Optional<GlobalPos> examDesk = Optional.empty();
    private Optional<GlobalPos> resultsDesk = Optional.empty();
    private Optional<GlobalPos> whistleHeard = Optional.empty();
    private PhotoOutlook photo = PhotoOutlook.NONE;
    private boolean presentToGive;
    private boolean seatedAtExam;
    private Optional<GlobalPos> ownerPos = Optional.empty();
    private final Map<UUID, GlobalPos> creatures = new HashMap<>();

    private TestView(GlobalPos pos, PetAnchor anchor) {
        this.pos = pos;
        this.anchor = anchor;
    }

    public static TestView at(GlobalPos pos, PetAnchor anchor) {
        return new TestView(pos, anchor);
    }

    public TestView phase(DayPhase phase) {
        this.phase = phase;
        return this;
    }

    public TestView ownership(PetOwnership ownership) {
        this.ownership = ownership;
        return this;
    }

    public TestView personality(Personality personality) {
        this.personality = personality;
        return this;
    }

    public TestView targets(PerceivedTargets targets) {
        this.targets = targets;
        return this;
    }

    public TestView slip(PetTask slip) {
        this.slip = Optional.of(slip);
        return this;
    }

    public TestView offeringBoard(GlobalPos board) {
        this.offeringBoard = Optional.of(board);
        return this;
    }

    public TestView takeTaskResting() {
        this.takeTaskCoolingDown = true;
        return this;
    }

    /** A shop the pet knows about and has both a reason and the money to visit. */
    public TestView shopWorthVisiting(GlobalPos shop) {
        this.shopWorthVisiting = Optional.of(shop);
        return this;
    }

    public TestView shopResting() {
        this.shopCoolingDown = true;
        return this;
    }

    /** The pet has something for its owner that it has not handed over yet. */
    public TestView holdingGift() {
        this.carryingGift = true;
        return this;
    }

    /** The pet has thought of playing a scene with a pet standing here. */
    public TestView socialPartner(GlobalPos partner) {
        this.socialPartner = Optional.of(partner);
        return this;
    }

    /** Another pet is on its way over to play a scene with this one. */
    public TestView askedToPlay() {
        this.playingAlong = true;
        return this;
    }

    /** The pet has been fed something worth working off. */
    public TestView fed() {
        this.eager = true;
        return this;
    }

    public TestView attackResting() {
        this.attackCoolingDown = true;
        return this;
    }

    public TestView armedWithArrows() {
        this.hasArrows = true;
        return this;
    }

    /** The held music box selects a song the pet would play now. */
    public TestView withSong() {
        this.hasPlayableSelection = true;
        return this;
    }

    public TestView performing() {
        this.playingMusic = true;
        return this;
    }

    /** A desk the pet could sit its exam at now. */
    public TestView examDesk(GlobalPos board) {
        this.examDesk = Optional.of(board);
        return this;
    }

    /** A desk the pet could go and see its results at this morning. */
    public TestView resultsDesk(GlobalPos board) {
        this.resultsDesk = Optional.of(board);
        return this;
    }

    /** A whistle this wild pet has heard and set off for. */
    public TestView whistleHeard(GlobalPos where) {
        this.whistleHeard = Optional.of(where);
        return this;
    }

    /** The pet has something in its bag it may give its owner, and is ready to. */
    public TestView withPresent() {
        this.presentToGive = true;
        return this;
    }

    /** What the pet could take a photo of, and whether it may. */
    public TestView photo(PhotoOutlook photo) {
        this.photo = photo;
        return this;
    }

    /** The pet is in its chair at an exam desk. */
    public TestView seated() {
        this.seatedAtExam = true;
        return this;
    }

    /** A creature that is somewhere in the pet's world, such as a friend it means to photograph. */
    public TestView creature(UUID id, GlobalPos where) {
        this.creatures.put(id, where);
        return this;
    }

    /** Where the pet's owner is. */
    public TestView ownerPos(GlobalPos owner) {
        this.ownerPos = Optional.of(owner);
        return this;
    }

    @Override
    public GlobalPos pos() {
        return pos;
    }

    @Override
    public DayPhase phase() {
        return phase;
    }

    @Override
    public PetOwnership ownership() {
        return ownership;
    }

    @Override
    public Personality personality() {
        return personality;
    }

    @Override
    public PetAnchor anchor() {
        return anchor;
    }

    @Override
    public AnchorSettings.Follow follow() {
        return TestAnchors.SETTINGS.follow();
    }

    @Override
    public PerceivedTargets targets() {
        return targets;
    }

    /** The item the pet perceives is the only one there is. */
    @Override
    public Optional<PickableItem> pickable(UUID item) {
        return targets.pickableItem().filter(perceived -> perceived.id().equals(item));
    }

    @Override
    public Optional<GlobalPos> whereIs(UUID creature) {
        return Optional.ofNullable(creatures.get(creature));
    }

    @Override
    public Optional<PetTask> slip() {
        return slip;
    }

    @Override
    public Optional<GlobalPos> ownerPos() {
        return ownerPos;
    }

    @Override
    public boolean eager() {
        return eager;
    }

    @Override
    public Optional<GlobalPos> offeringBoard() {
        return offeringBoard;
    }

    @Override
    public Optional<GlobalPos> shopWorthVisiting() {
        return shopWorthVisiting;
    }

    @Override
    public boolean takeTaskCoolingDown() {
        return takeTaskCoolingDown;
    }

    @Override
    public boolean shopCoolingDown() {
        return shopCoolingDown;
    }

    @Override
    public Optional<GlobalPos> socialPartner() {
        return socialPartner;
    }

    @Override
    public boolean playingAlong() {
        return playingAlong;
    }

    @Override
    public boolean carryingGift() {
        return carryingGift;
    }

    @Override
    public boolean presentToGive() {
        return presentToGive;
    }

    @Override
    public boolean attackCoolingDown() {
        return attackCoolingDown;
    }

    @Override
    public boolean hasArrows() {
        return hasArrows;
    }

    @Override
    public boolean hasPlayableSelection() {
        return hasPlayableSelection;
    }

    @Override
    public boolean playingMusic() {
        return playingMusic;
    }

    @Override
    public Optional<GlobalPos> examDesk() {
        return examDesk;
    }

    @Override
    public Optional<GlobalPos> resultsDesk() {
        return resultsDesk;
    }

    @Override
    public boolean seatedAtExam() {
        return seatedAtExam;
    }

    @Override
    public Optional<GlobalPos> whistleHeard() {
        return whistleHeard;
    }

    @Override
    public PhotoOutlook photo() {
        return photo;
    }
}
