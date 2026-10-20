package com.dwinovo.chiikawa.whistle;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitMemory;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * A whistle a wild pet heard, held in a brain memory until it has lost interest. The
 * memory's expiry is the curiosity running out; {@code from} is the moment the pet sets
 * off for it. Written by the pet's own mind when it is told of the whistle, see {@code PetMind}, and
 * not saved: a pet that is unloaded has lost interest by the time it is back.
 *
 * @param where the blower, as they stood when they blew
 * @param from the game time the pet starts for it
 */
public record WhistleHeard(GlobalPos where, long from) {
    /** @return the memory type the whistle is held in */
    public static MemoryModuleType<WhistleHeard> type() {
        return InitMemory.WHISTLE_HEARD.get();
    }

    /**
     * @param pet a pet
     * @return where a whistle was blown that this pet has set off for, once it has had time to react, while it is in the
     *         world the whistle was blown in; the one definition of it, for the pet's view and for the steps that go there
     */
    public static Optional<GlobalPos> by(AbstractPet pet) {
        return pet.getBrain().getMemory(type())
            .filter(heard -> heard.from() <= pet.level().getGameTime())
            .map(WhistleHeard::where)
            .filter(where -> where.dimension().equals(pet.level().dimension()));
    }
}
