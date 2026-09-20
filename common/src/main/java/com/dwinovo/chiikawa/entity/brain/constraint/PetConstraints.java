package com.dwinovo.chiikawa.entity.brain.constraint;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.brain.intent.IntentCategory;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * Turns the owner's {@link PetDirective} into what the AI may do: an anchor (where)
 * and a permission table (what kind of intent).
 *
 * <p>This is the only AI class that reads {@link PetDirective}. Every rule that used
 * to be a mode check inside a behavior or sensor lives in {@link #anchorOf} or
 * {@link #allows}.
 */
public final class PetConstraints {
    private PetConstraints() {
    }

    /**
     * @param pet the pet
     * @param ownership the pet's ownership
     * @return the pet's current anchor
     */
    public static PetAnchor anchorOf(AbstractPet pet, PetOwnership ownership) {
        return anchorOf(
            pet.getPetDirective(),
            ownership,
            GlobalPos.of(pet.level().dimension(), pet.blockPosition()),
            followableOwnerPos(pet),
            pet.getBrain().getMemory(MemoryModuleType.HOME),
            pet.isLeashed() || pet.isPassenger()
        );
    }

    /**
     * Pure anchor table.
     *
     * <p>A leashed or riding pet is moved by something else, so its anchor never
     * follows the owner or pulls it (0.0.9 skipped following and returning home in that
     * case too). Wild pets ignore the directive and roam around {@code HOME}, where they
     * spawned.
     *
     * @param directive the owner's directive
     * @param ownership the pet's ownership
     * @param petPos the pet's block position
     * @param ownerPos the owner's position while the owner is online, alive, not
     *                 spectating and in the pet's level
     * @param home the {@code HOME} memory
     * @param tethered whether the pet is leashed or riding
     * @return the anchor
     */
    static PetAnchor anchorOf(PetDirective directive, PetOwnership ownership, GlobalPos petPos,
            Optional<GlobalPos> ownerPos, Optional<GlobalPos> home, boolean tethered) {
        if (ownership instanceof PetOwnership.Wild) {
            return homeAnchor(petPos, home, AnchorDistances.WILD_REACH, AnchorDistances.WILD_LEASH, tethered);
        }
        return switch (directive) {
            case FOLLOW -> ownerPos
                .filter(owner -> !tethered && owner.dimension().equals(petPos.dimension()))
                .map(owner -> new PetAnchor(owner, AnchorDistances.FOLLOW_REACH, AnchorDistances.FOLLOW_LEASH, true, true))
                .orElseGet(() -> new PetAnchor(petPos, 0.0, PetAnchor.UNLEASHED, false, true));
            case STAY -> new PetAnchor(petPos, 0.0, PetAnchor.UNLEASHED, false, false);
            case FREE -> homeAnchor(petPos, home, AnchorDistances.FREE_REACH, AnchorDistances.FREE_LEASH, tethered);
        };
    }

    /**
     * Roaming around {@code HOME}, or around the pet itself when it has no home in its
     * level.
     */
    private static PetAnchor homeAnchor(GlobalPos petPos, Optional<GlobalPos> home, double reach, double leash,
            boolean tethered) {
        GlobalPos center = home.filter(pos -> pos.dimension().equals(petPos.dimension())).orElse(petPos);
        return new PetAnchor(center, reach, tethered ? PetAnchor.UNLEASHED : leash, false, true);
    }

    /**
     * @param pet the pet
     * @param ownership the pet's ownership
     * @param category an intent category
     * @return whether the pet's directive permits intents of {@code category}
     */
    public static boolean allows(AbstractPet pet, PetOwnership ownership, IntentCategory category) {
        return allows(pet.getPetDirective(), ownership, category);
    }

    /**
     * Pure permission table. Wild pets ignore the directive.
     */
    static boolean allows(PetDirective directive, PetOwnership ownership, IntentCategory category) {
        boolean wild = ownership instanceof PetOwnership.Wild;
        return switch (category) {
            case FOLLOW_OWNER -> !wild && directive == PetDirective.FOLLOW;
            case STAY -> !wild && directive == PetDirective.STAY;
            case WANDER, TAKE_TASK -> wild || directive != PetDirective.STAY;
            // Giving goes wherever the pet may walk: a pet at heel hands its present over
            // on the way, one left to itself brings it back. A pet told to sit sits, and
            // gives it to whoever comes to collect it.
            case GIFT -> !wild && directive != PetDirective.STAY;
            // Shopping goes with work: it is what an owner's pet does with its own day
            // once it has been let loose in it. A wild pet has no wages to spend, and one
            // at heel is out with its owner rather than out on its own errand.
            case WORK, PICK_UP, SHOP -> !wild && directive == PetDirective.FREE;
            case FORAGE -> wild || directive == PetDirective.FREE;
            // Fighting is the one kind of work a pet at heel still does: it is walking
            // beside its owner, and something is attacking them. Only a pet told to sit
            // sits through that.
            case COMBAT -> wild || directive != PetDirective.STAY;
        };
    }

    private static Optional<GlobalPos> followableOwnerPos(AbstractPet pet) {
        // getOwner only resolves players in the pet's own level.
        LivingEntity owner = pet.getOwner();
        if (owner == null || !owner.isAlive() || owner.isSpectator()) {
            return Optional.empty();
        }
        return Optional.of(GlobalPos.of(owner.level().dimension(), owner.blockPosition()));
    }
}
