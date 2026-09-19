package com.dwinovo.chiikawa.block;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.init.InitBlockEntities;
import com.dwinovo.chiikawa.task.BoardSlips;
import com.dwinovo.chiikawa.task.BoardSlot;
import com.dwinovo.chiikawa.task.PetTask;
import com.dwinovo.chiikawa.task.PetTaskTypes;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Holds a labor board's slips for the current day. The day's slips are rolled the first
 * time the board is looked at on that day, so an unvisited board costs nothing; see
 * {@link BoardSlips} for the rules of rolling, reserving and taking them.
 */
public class LaborBoardBlockEntity extends BlockEntity {
    private static final Codec<List<BoardSlot>> SLOTS_CODEC = BoardSlot.CODEC.listOf();
    private static final long NOT_ROLLED = -1L;

    /** Day number the slots were rolled for. */
    private long day = NOT_ROLLED;
    private List<BoardSlot> slots = List.of();

    public LaborBoardBlockEntity(BlockPos pos, BlockState state) {
        super(InitBlockEntities.LABOR_BOARD.get(), pos, state);
    }

    /** @return whether the board has a slip {@code pet} would take right now */
    public boolean offersSlipTo(AbstractPet pet) {
        return find(pet).isPresent();
    }

    /**
     * Holds the slip {@code pet} would take while it walks over, so no other pet heads
     * for the same one.
     *
     * @return whether there was a slip to hold
     */
    public boolean reserve(AbstractPet pet) {
        OptionalInt index = find(pet);
        if (index.isEmpty()) {
            return false;
        }
        long until = level().getGameTime() + BoardSlips.RESERVATION_TICKS;
        update(index.getAsInt(), slot -> slot.reserve(pet.getUUID(), until));
        return true;
    }

    /** @return whether {@code pet} holds one of the board's slips */
    public boolean isReservedBy(AbstractPet pet) {
        long gameTime = level().getGameTime();
        return today().stream().anyMatch(slot -> slot.reservedBy(pet.getUUID(), gameTime));
    }

    /** @return the slip {@code pet} takes down, now gone from the board */
    public Optional<PetTask> claim(AbstractPet pet) {
        OptionalInt index = find(pet);
        if (index.isEmpty()) {
            return Optional.empty();
        }
        BoardSlot slot = slots.get(index.getAsInt());
        update(index.getAsInt(), BoardSlot::claim);
        return Optional.of(slot.slip());
    }

    /** Lets go of any slip {@code pet} holds. */
    public void release(AbstractPet pet) {
        long gameTime = level().getGameTime();
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).reservedBy(pet.getUUID(), gameTime)) {
                update(i, BoardSlot::release);
            }
        }
    }

    /** @return today's slips */
    public List<BoardSlot> today() {
        ServerLevel level = level();
        long today = level.getDayTime() / Level.TICKS_PER_DAY;
        if (today != day) {
            day = today;
            slots = BoardSlips.roll(BoardSlips.seed(level.getSeed(), today, worldPosition), PetTaskTypes.all());
            setChanged();
        }
        return slots;
    }

    private OptionalInt find(AbstractPet pet) {
        ServerLevel level = level();
        return BoardSlips.find(today(), pet.getCapabilityId(), pet.getUUID(),
            PetOwnership.of(pet) instanceof PetOwnership.Wild,
            level.getDayTime() % Level.TICKS_PER_DAY, level.getGameTime());
    }

    private void update(int index, UnaryOperator<BoardSlot> change) {
        List<BoardSlot> changed = new ArrayList<>(slots);
        changed.set(index, change.apply(changed.get(index)));
        slots = List.copyOf(changed);
        setChanged();
    }

    private ServerLevel level() {
        return (ServerLevel) Objects.requireNonNull(level, "labor board is not in a level");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("Day", day);
        tag.put("Slots", SLOTS_CODEC.encodeStart(NbtOps.INSTANCE, slots).getOrThrow());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        day = tag.contains("Day", Tag.TAG_LONG) ? tag.getLong("Day") : NOT_ROLLED;
        slots = tag.contains("Slots", Tag.TAG_LIST)
            ? SLOTS_CODEC.parse(NbtOps.INSTANCE, tag.get("Slots")).result().orElse(List.of())
            : List.of();
    }
}
