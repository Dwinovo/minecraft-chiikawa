package com.dwinovo.chiikawa.testing;

import com.dwinovo.chiikawa.camera.PhotoOutlook;
import com.dwinovo.chiikawa.data.QualificationData;
import com.dwinovo.chiikawa.entity.brain.combat.Arms;
import com.dwinovo.chiikawa.entity.brain.combat.Ground;
import com.dwinovo.chiikawa.entity.brain.constraint.AnchorSettings;
import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.InvitationDesk;
import com.dwinovo.chiikawa.entity.brain.errand.view.AttackTarget;
import com.dwinovo.chiikawa.entity.brain.errand.view.Attacker;
import com.dwinovo.chiikawa.entity.brain.errand.view.DayPhase;
import com.dwinovo.chiikawa.entity.brain.errand.view.Onlooker;
import com.dwinovo.chiikawa.entity.brain.errand.view.PerceivedTargets;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.entity.brain.errand.view.PickableItem;
import com.dwinovo.chiikawa.entity.brain.errand.view.SocialIdea;
import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import com.dwinovo.chiikawa.gifting.GiftChoice;
import com.dwinovo.chiikawa.gifting.GiftOutlook;
import com.dwinovo.chiikawa.music.MusicBoxSelection;
import com.dwinovo.chiikawa.music.Playable;
import com.dwinovo.chiikawa.music.PlaybackMode;
import com.dwinovo.chiikawa.qualification.PetExams;
import com.dwinovo.chiikawa.task.PetTask;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.item.Items;

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
    private boolean carryingGift;
    private Optional<SocialIdea> socialIdea = Optional.empty();
    private Optional<InvitationDesk.Engagement> engagement = Optional.empty();
    private boolean eager;
    private boolean attackCoolingDown;
    private boolean hasArrows;
    private boolean hasSeeds;
    private Optional<Attacker> attacker = Optional.empty();
    private Set<Ground> foeGrounds = EnumSet.allOf(Ground.class);
    private Arms arms = Arms.NONE;
    private Optional<Playable> playableSelection = Optional.empty();
    private boolean songPlayed;
    private Optional<PetExams.Summons> examToSit = Optional.empty();
    private Optional<GlobalPos> resultsDesk = Optional.empty();
    private Optional<GlobalPos> whistleHeard = Optional.empty();
    private PhotoOutlook photo = PhotoOutlook.NONE;
    private GiftOutlook gift = GiftOutlook.NONE;
    private Optional<GlobalPos> ownerBedside = Optional.empty();
    private boolean seatedAtExam;
    private Optional<GlobalPos> ownerPos = Optional.empty();
    private final Map<UUID, GlobalPos> creatures = new HashMap<>();
    private final Map<Onlooker, UUID> onlookers = new HashMap<>();

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

    /** A shop the pet knows about and has both a reason and the money to visit. */
    public TestView shopWorthVisiting(GlobalPos shop) {
        this.shopWorthVisiting = Optional.of(shop);
        return this;
    }

    /** The pet has something for its owner that it has not handed over yet. */
    public TestView holdingGift() {
        this.carryingGift = true;
        return this;
    }

    /** The pet has thought of playing a scene with a pet standing here. */
    public TestView socialPartner(GlobalPos partner) {
        this.socialIdea = Optional.of(TestScenes.idea(partner));
        this.creatures.put(TestScenes.PARTNER, partner);
        return this;
    }

    /** Another pet has asked this one to play a scene, and been told yes. */
    public TestView askedToPlay() {
        this.engagement = Optional.of(TestScenes.asked());
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

    /** The pet carries a seed. */
    public TestView carryingSeeds() {
        this.hasSeeds = true;
        return this;
    }

    /** Something hurt the pet lately, and is standing here. */
    public TestView attackedBy(UUID who, GlobalPos where) {
        this.attacker = Optional.of(new Attacker(who, where));
        this.creatures.put(who, where);
        return this;
    }

    /** The grounds the pet's orders let it fight on; all of them, until a test says otherwise. */
    public TestView fightingOn(Ground... grounds) {
        this.foeGrounds = Set.of(grounds);
        return this;
    }

    /** What the pet has to fight with; nothing, until a test says otherwise. */
    public TestView arms(Arms arms) {
        this.arms = arms;
        return this;
    }

    /** The held music box selects a song the pet would play now. */
    public TestView withSong() {
        this.playableSelection = Optional.of(new Playable(new MusicBoxSelection("song", "Song", 1), PlaybackMode.ONCE, false));
        return this;
    }

    /** The held music box selects a song the pet has played already: it plays it again only on a slip. */
    public TestView withSongPlayed() {
        withSong();
        this.songPlayed = true;
        return this;
    }

    /** A desk the pet could sit its exam at now. */
    public TestView examDesk(GlobalPos board) {
        this.examToSit = Optional.of(new PetExams.Summons(QualificationData.WEEDING, board));
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

    /** The pet has a present in mind that it is ready to give, a cookie it has in its bag, and its owner may be given more. */
    public TestView withPresent() {
        return gift(new GiftOutlook(true, 3, List.of(new GiftChoice.Pick(2, Items.COOKIE, 1))));
    }

    /** What the pet could give its owner now. */
    public TestView gift(GiftOutlook gift) {
        this.gift = gift;
        return this;
    }

    /** The owner is asleep in a bed, and a pet waits at this place by it. */
    public TestView ownerAsleepBeside(GlobalPos bedside) {
        this.ownerBedside = Optional.of(bedside);
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

    /** Someone of this kind the pet can see, near enough for any habit of looking at them. */
    public TestView onlooker(Onlooker kind, UUID who) {
        this.onlookers.put(kind, who);
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

    /** A creature told of by {@link #creature}, or the foe the pet perceives. */
    @Override
    public Optional<GlobalPos> whereIs(UUID creature) {
        return Optional.ofNullable(creatures.get(creature))
            .or(() -> targets.attackTarget().filter(foe -> foe.id().equals(creature)).map(AttackTarget::where));
    }

    /** Whoever {@link #onlooker} told of, at any range. */
    @Override
    public Optional<UUID> nearestVisible(Onlooker kind, float range) {
        return Optional.ofNullable(onlookers.get(kind));
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
    public Optional<SocialIdea> socialIdea() {
        return socialIdea;
    }

    @Override
    public Optional<InvitationDesk.Engagement> engagement() {
        return engagement;
    }

    @Override
    public boolean carryingGift() {
        return carryingGift;
    }

    @Override
    public GiftOutlook gift() {
        return gift;
    }

    @Override
    public Optional<GlobalPos> ownerBedside() {
        return ownerBedside;
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
    public boolean hasSeeds() {
        return hasSeeds;
    }

    @Override
    public Optional<Attacker> attacker() {
        return attacker;
    }

    @Override
    public Set<Ground> foeGrounds() {
        return foeGrounds;
    }

    @Override
    public Arms arms() {
        return arms;
    }

    @Override
    public Optional<Playable> playable(boolean onSlip) {
        return playableSelection.filter(song -> onSlip || !songPlayed).map(song -> new Playable(song.selection(), song.mode(), onSlip));
    }

    @Override
    public Optional<PetExams.Summons> examToSit() {
        return examToSit;
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
