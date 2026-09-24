package com.dwinovo.chiikawa.entity.brain.intent;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
import com.dwinovo.chiikawa.entity.brain.constraint.PetConstraints;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.entity.brain.personality.Personality;
import com.dwinovo.chiikawa.entity.brain.personality.PetPersonalities;
import com.dwinovo.chiikawa.entity.brain.task.musician.PlayMusicBehavior;
import com.dwinovo.chiikawa.init.InitBlockEntities;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.shop.ShopBasket;
import com.dwinovo.chiikawa.shop.Wallet;
import com.dwinovo.chiikawa.social.InteractionPlan;
import com.dwinovo.chiikawa.task.PetTask;
import com.dwinovo.chiikawa.utils.Utils;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.Level;

/**
 * Plain-data snapshot an intent's conditions and score read, built once per
 * evaluation. It holds the anchor derived from the directive but never the
 * directive itself, and no entity or level, so intents cannot bypass the
 * permission table or change the world while being scored. Tests build it by hand.
 *
 * @param petPos the pet's block position
 * @param phase the part of the day in the pet's level
 * @param ownership who the pet belongs to
 * @param personality how the pet's kind leans
 * @param anchor where the pet may act
 * @param targets what the sensors remember
 * @param task the slip the pet carries
 * @param offeringBoard the nearest labor board, while it has a slip the pet would take
 * @param shopWorthVisiting the nearest shop, while it sells something the pet likes and can afford
 * @param shopCoolingDown whether the pet has just bought something
 * @param socialPartner the pet this one has thought of playing a scene with
 * @param playingAlong whether another pet is on its way to play a scene with this one
 * @param carryingGift whether the pet has something for its owner that it has not handed over
 * @param eager whether the pet is still in the mood after a proper meal
 * @param takeTaskCoolingDown whether the pet recently failed to take a slip
 * @param attackCoolingDown whether the pet's attack cooldown is running
 * @param hasArrows whether the pet carries arrows
 * @param hasPlayableSelection whether the held music box selects a song the pet would play now
 * @param playingMusic whether the pet is performing
 */
public record IntentContext(
    GlobalPos petPos,
    DayPhase phase,
    PetOwnership ownership,
    Personality personality,
    PetAnchor anchor,
    PerceivedTargets targets,
    Optional<PetTask> task,
    Optional<GlobalPos> offeringBoard,
    Optional<GlobalPos> shopWorthVisiting,
    boolean takeTaskCoolingDown,
    boolean shopCoolingDown,
    Optional<GlobalPos> socialPartner,
    boolean playingAlong,
    boolean carryingGift,
    boolean eager,
    boolean attackCoolingDown,
    boolean hasArrows,
    boolean hasPlayableSelection,
    boolean playingMusic
) {
    public static IntentContext capture(AbstractPet pet, PetOwnership ownership) {
        Level level = pet.level();
        return new IntentContext(
            GlobalPos.of(level.dimension(), pet.blockPosition()),
            // A level without a day cycle, like the Nether, no longer has a time of day of its
            // own; a day's routine goes by the world's clock there, as villagers' schedules do.
            DayPhase.of(level.getDayTime()),
            ownership,
            PetPersonalities.of(pet.getType()),
            PetConstraints.anchorOf(pet, ownership),
            PerceivedTargets.capture(pet),
            pet.getTask(),
            offeringBoard(pet),
            shopWorthVisiting(pet),
            pet.getBrain().hasMemoryValue(InitMemory.TAKE_TASK_COOLDOWN.get()),
            pet.getBrain().hasMemoryValue(InitMemory.SHOP_COOLDOWN.get()),
            pet.getBrain().getMemory(InitMemory.INTERACTION_PLAN.get())
                .map(InteractionPlan::partner)
                .filter(AbstractPet::isAlive)
                .map(partner -> GlobalPos.of(partner.level().dimension(), partner.blockPosition())),
            pet.getBrain().hasMemoryValue(InitMemory.INTERACTION_RESERVATION.get()),
            !pet.getPendingGift().isEmpty(),
            pet.isEager(),
            pet.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_COOLING_DOWN),
            !Utils.getArrow(pet).isEmpty(),
            PlayMusicBehavior.playableSelection(pet).isPresent(),
            pet.getActivity() == PetActivity.PLAY_GUITAR
        );
    }

    /**
     * The shop the pet remembers, while there is a reason to go: something it likes, on
     * sale, and within what it is carrying. Asked every time the pet decides what to do,
     * so it only reads — the choosing of what to buy happens again at the counter.
     */
    private static Optional<GlobalPos> shopWorthVisiting(AbstractPet pet) {
        Level level = pet.level();
        return pet.getBrain().getMemory(InitMemory.NEAREST_SHOP.get())
            .filter(level::isLoaded)
            .flatMap(pos -> level.getBlockEntity(pos, InitBlockEntities.SHOP.get()))
            .filter(shop -> ShopBasket.wantsAnything(PetPersonalities.of(pet.getType()), shop.catalog(),
                Wallet.count(pet.getBackpack())))
            .map(shop -> GlobalPos.of(level.dimension(), shop.getBlockPos()));
    }

    private static Optional<GlobalPos> offeringBoard(AbstractPet pet) {
        Level level = pet.level();
        return pet.getBrain().getMemory(InitMemory.NEAREST_BOARD.get())
            .filter(level::isLoaded)
            .flatMap(pos -> level.getBlockEntity(pos, InitBlockEntities.LABOR_BOARD.get()))
            .filter(board -> board.offersSlipTo(pet))
            .map(board -> GlobalPos.of(level.dimension(), board.getBlockPos()));
    }
}
