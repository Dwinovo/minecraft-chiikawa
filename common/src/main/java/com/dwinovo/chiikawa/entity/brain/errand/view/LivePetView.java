package com.dwinovo.chiikawa.entity.brain.errand.view;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.block.ExamDeskBlockEntity;
import com.dwinovo.chiikawa.camera.PhotoOutlook;
import com.dwinovo.chiikawa.camera.PhotoScout;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.constraint.AnchorSettings;
import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.constraint.PetConstraints;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import com.dwinovo.chiikawa.entity.brain.personality.PetPersonalities;
import com.dwinovo.chiikawa.entity.brain.task.musician.PlayMusicBehavior;
import com.dwinovo.chiikawa.gifting.GiftErrand;
import com.dwinovo.chiikawa.init.InitBlockEntities;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.qualification.PetExams;
import com.dwinovo.chiikawa.shop.ShopBasket;
import com.dwinovo.chiikawa.shop.Wallet;
import com.dwinovo.chiikawa.social.InteractionPlan;
import com.dwinovo.chiikawa.task.PetTask;
import com.dwinovo.chiikawa.utils.Utils;
import com.dwinovo.chiikawa.whistle.WhistleHeard;
import java.util.Optional;
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
    private final Lazy<Boolean> takeTaskCoolingDown;
    private final Lazy<Boolean> shopCoolingDown;
    private final Lazy<Optional<GlobalPos>> socialPartner;
    private final Lazy<Boolean> playingAlong;
    private final Lazy<Boolean> carryingGift;
    private final Lazy<Boolean> presentToGive;
    private final Lazy<Boolean> attackCoolingDown;
    private final Lazy<Boolean> hasArrows;
    private final Lazy<Boolean> hasPlayableSelection;
    private final Lazy<Boolean> playingMusic;
    private final Lazy<Optional<GlobalPos>> examDesk;
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
        this.takeTaskCoolingDown = new Lazy<>(() -> pet.getBrain().hasMemoryValue(InitMemory.TAKE_TASK_COOLDOWN.get()));
        this.shopCoolingDown = new Lazy<>(() -> pet.getBrain().hasMemoryValue(InitMemory.SHOP_COOLDOWN.get()));
        this.socialPartner = new Lazy<>(() -> pet.getBrain().getMemory(InitMemory.INTERACTION_PLAN.get())
            .map(InteractionPlan::partner)
            .filter(AbstractPet::isAlive)
            .map(partner -> GlobalPos.of(partner.level().dimension(), partner.blockPosition())));
        this.playingAlong = new Lazy<>(() -> pet.getBrain().hasMemoryValue(InitMemory.INTERACTION_RESERVATION.get()));
        this.carryingGift = new Lazy<>(() -> !pet.getPendingGift().isEmpty());
        this.presentToGive = new Lazy<>(() -> GiftErrand.thinksOfAnother(pet));
        this.attackCoolingDown = new Lazy<>(() -> pet.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_COOLING_DOWN));
        this.hasArrows = new Lazy<>(() -> !Utils.getArrow(pet).isEmpty());
        this.hasPlayableSelection = new Lazy<>(() -> PlayMusicBehavior.playableSelection(pet).isPresent());
        this.playingMusic = new Lazy<>(() -> pet.getActivity() == PetActivity.PLAY_GUITAR);
        this.examDesk = new Lazy<>(() -> PetExams.toSit(pet)
            .map(PetExams.Summons::desk)
            .filter(desk -> ExamDeskBlockEntity.at(level, desk).isPresent()));
        this.resultsDesk = new Lazy<>(() -> PetExams.resultsDesk(pet)
            .filter(desk -> ExamDeskBlockEntity.at(level, desk).isPresent()));
        this.seatedAtExam = new Lazy<>(() -> PetExams.seatedAtExam(pet));
        this.whistleHeard = new Lazy<>(() -> pet.getBrain().getMemory(InitMemory.WHISTLE_HEARD.get())
            .filter(heard -> heard.from() <= level.getGameTime())
            .map(WhistleHeard::where));
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
    public boolean takeTaskCoolingDown() {
        return takeTaskCoolingDown.get();
    }

    @Override
    public boolean shopCoolingDown() {
        return shopCoolingDown.get();
    }

    @Override
    public Optional<GlobalPos> socialPartner() {
        return socialPartner.get();
    }

    @Override
    public boolean playingAlong() {
        return playingAlong.get();
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
    public boolean hasPlayableSelection() {
        return hasPlayableSelection.get();
    }

    @Override
    public boolean playingMusic() {
        return playingMusic.get();
    }

    @Override
    public Optional<GlobalPos> examDesk() {
        return examDesk.get();
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
}
