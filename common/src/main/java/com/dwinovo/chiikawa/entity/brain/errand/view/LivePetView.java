package com.dwinovo.chiikawa.entity.brain.errand.view;

import com.dwinovo.chiikawa.block.ExamDeskBlockEntity;
import com.dwinovo.chiikawa.camera.PhotoOutlook;
import com.dwinovo.chiikawa.camera.PhotoScout;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.constraint.AnchorSettings;
import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.constraint.PetConstraints;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.InvitationDesk;
import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import com.dwinovo.chiikawa.entity.brain.personality.PetPersonalities;
import com.dwinovo.chiikawa.gifting.GiftPool;
import com.dwinovo.chiikawa.init.InitBlockEntities;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.music.Playable;
import com.dwinovo.chiikawa.music.PlaybackMode;
import com.dwinovo.chiikawa.music.ServerMusicSystem;
import com.dwinovo.chiikawa.qualification.PetExams;
import com.dwinovo.chiikawa.shop.ShopBasket;
import com.dwinovo.chiikawa.shop.Wallet;
import com.dwinovo.chiikawa.social.PetInteractions;
import com.dwinovo.chiikawa.social.SocialRules;
import com.dwinovo.chiikawa.task.PetTask;
import com.dwinovo.chiikawa.task.PetWorkCounters;
import com.dwinovo.chiikawa.utils.Utils;
import com.dwinovo.chiikawa.whistle.WhistleHeard;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A view of a live pet, for one evaluation. Each fact is worked out the first time an errand asks
 * for it and kept, so that what a priority level does not look at costs nothing and every errand
 * that does look sees the same pet. Made afresh for the next evaluation; never kept.
 *
 * <p>Version-sensitive reads of the pet and its level live here and nowhere in the errands.
 */
public final class LivePetView implements PetView {
    private final AbstractPet pet;
    private final Lazy<GlobalPos> pos;
    private final Lazy<DayPhase> phase;
    private final Lazy<PetOwnership> ownership;
    private final Lazy<Personality> personality;
    private final Lazy<PetAnchor> anchor;
    private final Lazy<AnchorSettings.Follow> follow;
    private final Lazy<PerceivedTargets> targets;
    private final Lazy<Optional<PetTask>> slip;
    private final Lazy<Optional<GlobalPos>> ownerPos;
    private final Lazy<Boolean> eager;
    private final Lazy<Optional<GlobalPos>> offeringBoard;
    private final Lazy<Optional<GlobalPos>> shopWorthVisiting;
    private final Lazy<Optional<SocialIdea>> socialIdea;
    private final Lazy<Optional<InvitationDesk.Engagement>> engagement;
    private final Lazy<Boolean> carryingGift;
    private final Lazy<Boolean> presentToGive;
    private final Lazy<Boolean> attackCoolingDown;
    private final Lazy<Boolean> hasArrows;
    private final Lazy<Optional<Playable>> playableSelection;
    private final Lazy<Optional<PetExams.Summons>> examToSit;
    private final Lazy<Optional<GlobalPos>> resultsDesk;
    private final Lazy<Boolean> seatedAtExam;
    private final Lazy<Optional<GlobalPos>> whistleHeard;
    private final Lazy<PhotoOutlook> photo;

    private LivePetView(AbstractPet pet) {
        this.pet = pet;
        Level level = pet.level();
        this.pos = new Lazy<>(() -> GlobalPos.of(level.dimension(), pet.blockPosition()));
        // A level without a day cycle, like the Nether, stays at its fixed time of day.
        this.phase = new Lazy<>(() -> DayPhase.of(level.dimensionType().fixedTime().orElse(level.getDayTime())));
        this.ownership = new Lazy<>(() -> PetOwnership.of(pet));
        this.personality = new Lazy<>(() -> PetPersonalities.of(pet.getType()));
        this.anchor = new Lazy<>(() -> PetConstraints.anchorOf(pet, ownership()));
        this.follow = new Lazy<>(() -> AnchorSettings.current().map(AnchorSettings::follow).orElse(AnchorSettings.Follow.NONE));
        this.targets = new Lazy<>(() -> PerceivedTargets.capture(pet));
        this.slip = new Lazy<>(pet::getTask);
        this.ownerPos = new Lazy<>(() -> pet.getOwner() instanceof Player owner
            ? Optional.of(GlobalPos.of(owner.level().dimension(), owner.blockPosition())) : Optional.empty());
        this.eager = new Lazy<>(pet::isEager);
        this.offeringBoard = new Lazy<>(this::findOfferingBoard);
        this.shopWorthVisiting = new Lazy<>(this::findShopWorthVisiting);
        this.socialIdea = new Lazy<>(this::findSocialIdea);
        this.engagement = new Lazy<>(() -> pet.getMind().errands().engagement(level.getGameTime())
            .filter(asked -> PetInteractions.all().containsKey(asked.scene())));
        this.carryingGift = new Lazy<>(() -> pet.getOwner() instanceof Player && !pet.getPendingGift().isEmpty());
        this.presentToGive = new Lazy<>(() -> pet.getOwner() instanceof Player && !GiftPool.of(pet).isEmpty());
        this.attackCoolingDown = new Lazy<>(() -> pet.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_COOLING_DOWN));
        this.hasArrows = new Lazy<>(() -> !Utils.getArrow(pet).isEmpty());
        this.playableSelection = new Lazy<>(this::findPlayableSelection);
        this.examToSit = new Lazy<>(() -> PetExams.toSit(pet)
            .filter(summons -> ExamDeskBlockEntity.at(level, summons.desk()).isPresent()));
        this.resultsDesk = new Lazy<>(() -> PetExams.resultsDesk(pet)
            .filter(desk -> ExamDeskBlockEntity.at(level, desk).isPresent()));
        this.seatedAtExam = new Lazy<>(() -> PetExams.seatedAtExam(pet));
        this.whistleHeard = new Lazy<>(() -> WhistleHeard.by(pet));
        this.photo = new Lazy<>(() -> PhotoScout.outlook(pet));
    }

    /** @return a view of this pet as it is now */
    public static LivePetView of(AbstractPet pet) {
        return new LivePetView(pet);
    }

    @Override
    public GlobalPos pos() {
        return pos.get();
    }

    @Override
    public DayPhase phase() {
        return phase.get();
    }

    @Override
    public PetOwnership ownership() {
        return ownership.get();
    }

    @Override
    public Personality personality() {
        return personality.get();
    }

    @Override
    public PetAnchor anchor() {
        return anchor.get();
    }

    @Override
    public AnchorSettings.Follow follow() {
        return follow.get();
    }

    @Override
    public PerceivedTargets targets() {
        return targets.get();
    }

    @Override
    public Optional<PetTask> slip() {
        return slip.get();
    }

    @Override
    public Optional<GlobalPos> ownerPos() {
        return ownerPos.get();
    }

    @Override
    public boolean eager() {
        return eager.get();
    }

    @Override
    public Optional<GlobalPos> offeringBoard() {
        return offeringBoard.get();
    }

    @Override
    public Optional<GlobalPos> shopWorthVisiting() {
        return shopWorthVisiting.get();
    }

    @Override
    public Optional<SocialIdea> socialIdea() {
        return socialIdea.get();
    }

    @Override
    public Optional<InvitationDesk.Engagement> engagement() {
        return engagement.get();
    }

    @Override
    public boolean carryingGift() {
        return carryingGift.get();
    }

    @Override
    public boolean presentToGive() {
        return presentToGive.get();
    }

    @Override
    public boolean attackCoolingDown() {
        return attackCoolingDown.get();
    }

    @Override
    public boolean hasArrows() {
        return hasArrows.get();
    }

    @Override
    public Optional<Playable> playableSelection() {
        return playableSelection.get();
    }

    @Override
    public Optional<PetExams.Summons> examToSit() {
        return examToSit.get();
    }

    @Override
    public Optional<GlobalPos> resultsDesk() {
        return resultsDesk.get();
    }

    @Override
    public boolean seatedAtExam() {
        return seatedAtExam.get();
    }

    @Override
    public Optional<GlobalPos> whistleHeard() {
        return whistleHeard.get();
    }

    @Override
    public PhotoOutlook photo() {
        return photo.get();
    }

    @Override
    public Optional<PickableItem> pickable(UUID item) {
        return pet.level() instanceof ServerLevel level && level.getEntity(item) instanceof ItemEntity found
            ? PickableItem.of(pet, found) : Optional.empty();
    }

    @Override
    public Optional<UUID> nearestVisible(Onlooker kind, float range) {
        return pet.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
            .flatMap(seen -> seen.findClosest(entity -> kind.matches(entity) && pet.distanceToSqr(entity) <= range * range))
            .map(LivingEntity::getUUID);
    }

    @Override
    public Optional<GlobalPos> whereIs(UUID creature) {
        return pet.level() instanceof ServerLevel level && level.getEntity(creature) instanceof LivingEntity found && found.isAlive()
            ? Optional.of(GlobalPos.of(found.level().dimension(), found.blockPosition())) : Optional.empty();
    }

    private Optional<GlobalPos> findOfferingBoard() {
        Level level = pet.level();
        return pet.getBrain().getMemory(InitMemory.NEAREST_BOARD.get())
            .filter(level::isLoaded)
            .flatMap(pos -> level.getBlockEntity(pos, InitBlockEntities.LABOR_BOARD.get()))
            .filter(board -> board.offersSlipTo(pet))
            .map(board -> GlobalPos.of(level.dimension(), board.getBlockPos()));
    }

    /**
     * The shop the pet remembers, while there is a reason to go: something it likes, on
     * sale, and within what it is carrying. Asked every time the pet decides what to do,
     * so it only reads: the choosing of what to buy happens again at the counter.
     */
    private Optional<GlobalPos> findShopWorthVisiting() {
        Level level = pet.level();
        return pet.getBrain().getMemory(InitMemory.NEAREST_SHOP.get())
            .filter(level::isLoaded)
            .flatMap(pos -> level.getBlockEntity(pos, InitBlockEntities.SHOP.get()))
            .filter(shop -> ShopBasket.wantsAnything(personality(), shop.catalog(),
                Wallet.count(pet.getBackpack()), ShopBasket.carriedIn(pet.getBackpack())))
            .map(shop -> GlobalPos.of(level.dimension(), shop.getBlockPos()));
    }

    /**
     * The scene the sensor thought of, while it is still one to play: the pet it means to visit would play along now and
     * the pet has what the scene takes. The sensor thought of it a moment ago, and either may have changed since.
     */
    private Optional<SocialIdea> findSocialIdea() {
        long now = pet.level().getGameTime();
        return pet.getBrain().getMemory(InitMemory.INTERACTION_PLAN.get())
            .filter(idea -> SocialRules.hasToHandOver(pet, idea.interaction()))
            .flatMap(idea -> {
                OptionalInt rank = SocialRules.accepts(idea.id(), idea.interaction(), pet, idea.partner(), now);
                return rank.isEmpty() ? Optional.empty() : Optional.of(new SocialIdea(idea.id(), idea.interaction(), idea.self(),
                    rank.getAsInt(), idea.partner().getUUID(),
                    GlobalPos.of(idea.partner().level().dimension(), idea.partner().blockPosition())));
            });
    }

    /**
     * The song on the held music box, as the pet would play on from it: asked every time the pet decides what to do, so
     * it only reads. What it last started is the pet's own memory; a slip that counts seconds of music is a street
     * performance.
     */
    private Optional<Playable> findPlayableSelection() {
        boolean onSlip = pet.getTask().filter(task -> task.counter().equals(PetWorkCounters.PLAY_MUSIC_SECOND)).isPresent();
        return Playable.of(Playable.held(pet.getMainHandItem()), PlaybackMode.get(pet.getMainHandItem()),
            pet.getBrain().getMemory(InitMemory.MUSICIAN_LAST_MUSIC_SIGNATURE.get()), onSlip,
            track -> ServerMusicSystem.library(pet.level().getServer()).getTrack(track).isPresent());
    }
}
