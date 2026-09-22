package com.dwinovo.chiikawa.block;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.init.InitBlockEntities;
import com.dwinovo.chiikawa.network.BoardPayloads;
import com.dwinovo.chiikawa.task.BoardLevels;
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
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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
    /** How often a board looks at the clock: a second is soon enough for a plate to go up. */
    private static final int CLOCK_TICKS = 20;

    /** Day number the slots were rolled for. */
    private long day = NOT_ROLLED;
    private List<BoardSlot> slots = List.of();
    /**
     * How far the owner has paid the board up, as saved; see {@link BoardLevels}. Spelt out
     * rather than called a level, because a block entity already has a level: the world.
     */
    private int boardLevel = BoardLevels.FIRST_LEVEL;
    /**
     * Which of the day's plates still hang on the board, as {@link BoardSlips#hanging}
     * reckons it: worked out here and sent to the players nearby, who see only this.
     */
    private int hanging;

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
        update(index.getAsInt(), open -> open.claim(claimOf(pet)));
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

    /** @return which of the day's plates still hang, a bit for each place */
    public int hanging() {
        return hanging;
    }

    /** @return how far the board has been paid up, within the levels boards have today */
    public int boardLevel() {
        return BoardLevels.current().clamp(boardLevel);
    }

    /**
     * Takes the board up a level. The day's slips are not re-rolled: what the new level
     * buys goes up beside them, so nobody's pet loses the slip it is out working on.
     *
     * @return whether there was a level left to buy
     */
    public boolean upgrade() {
        BoardLevels levels = BoardLevels.current();
        int at = levels.clamp(boardLevel);
        if (at >= levels.top()) {
            return false;
        }
        boardLevel = at + 1;
        setChanged();
        return true;
    }

    /** @return today's slips as the board screen shows them */
    public List<BoardPayloads.SlipView> slipViews() {
        return today().stream()
            .map(slot -> new BoardPayloads.SlipView(slot.slip().type(), slot.slip().icon(), slot.slip().capability(),
                slot.slip().target(), slot.claim().map(BoardSlot.Claim::describe).orElse("")))
            .toList();
    }

    /** @return today's slips */
    public List<BoardSlot> today() {
        ServerLevel world = level();
        long today = world.getDayTime() / Level.TICKS_PER_DAY;
        long seed = BoardSlips.seed(world.getSeed(), today, worldPosition);
        if (today != day) {
            day = today;
            slots = BoardSlips.roll(seed, PetTaskTypes.all(), BoardLevels.current(), boardLevel);
            markUpdated();
            return slots;
        }
        // A board upgraded partway through the day puts the slip it just bought up now.
        List<BoardSlot> grown = BoardSlips.topUp(slots, seed, PetTaskTypes.all(), BoardLevels.current(), boardLevel);
        if (grown != slots) {
            slots = grown;
            markUpdated();
        }
        return slots;
    }

    private static BoardSlot.Claim claimOf(AbstractPet pet) {
        LivingEntity owner = pet.getOwner();
        return new BoardSlot.Claim(pet.getDisplayName().getString(),
            owner == null ? "" : owner.getDisplayName().getString());
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
        markUpdated();
    }

    /**
     * The slips changed: saved with the chunk, and the plates the board shows told to the
     * players who can see it, so a plate comes down the moment a pet takes it. The way the
     * campfire tells them what is on its grill.
     */
    private void markUpdated() {
        hanging = BoardSlips.hanging(slots);
        setChanged();
        level().sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    /**
     * Keeps the plates up to date by itself: puts the day's slips up the first second of a
     * new day, and the first second after the board is placed, with nobody having to look
     * at it first.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, LaborBoardBlockEntity board) {
        if (level.getGameTime() % CLOCK_TICKS == 0) {
            board.today();
        }
    }

    /**
     * What a player's game is told of the board: only which plates hang, nothing of whose
     * they are.
     *
     * <p>Only read here, never worked out: the game asks for this while it is sending the
     * chunk's changed blocks out, and a board that changed itself then — putting its day's
     * slips up and telling the chunk about it — would be telling it in the middle of that
     * send. The chunk would lose track of that part of itself, and no block broken there
     * would ever reach a player again.
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Hanging", hanging);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private ServerLevel level() {
        return (ServerLevel) Objects.requireNonNull(level, "labor board is not in a level");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("Day", day);
        tag.putInt("Level", boardLevel);
        tag.put("Slots", SLOTS_CODEC.encodeStart(NbtOps.INSTANCE, slots).getOrThrow());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        day = tag.contains("Day", Tag.TAG_LONG) ? tag.getLong("Day") : NOT_ROLLED;
        // Kept as saved and read within the levels there are when it is used: a pack that
        // takes levels away for a while does not cost the board the ones it paid for.
        boardLevel = tag.contains("Level", Tag.TAG_INT)
            ? Math.max(BoardLevels.FIRST_LEVEL, tag.getInt("Level"))
            : BoardLevels.FIRST_LEVEL;
        slots = tag.contains("Slots", Tag.TAG_LIST)
            ? SLOTS_CODEC.parse(NbtOps.INSTANCE, tag.get("Slots")).result().orElse(List.of())
            : List.of();
        // A save holds the slips and the plates follow from them; a player's game is sent
        // the plates alone.
        hanging = tag.contains("Hanging", Tag.TAG_INT) ? tag.getInt("Hanging") : BoardSlips.hanging(slots);
    }
}
