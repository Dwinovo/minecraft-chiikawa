package com.dwinovo.chiikawa.block;

import com.dwinovo.chiikawa.init.InitBlockEntities;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An exam desk's note of who is signed up at it, and the sheet that puts on it. One pet at a
 * time: the desk is taken from sign-up until that pet hands in or the exam closes, and the
 * note lapses by the clock once its results have been up, so a pet that never comes back
 * does not hold the desk for good. The chair has one too, as both halves of a bed do, to be
 * drawn by; it keeps nothing.
 */
public class ExamDeskBlockEntity extends BlockEntity {
    /** How often a desk looks at the clock: a second is soon enough for a sheet to change. */
    private static final int CLOCK_TICKS = 20;

    private Optional<DeskBooking> booking = Optional.empty();
    /**
     * The sheet on the desk, as {@link DeskBooking#sheet} reckons it: worked out here and sent
     * to the players nearby, who see only this.
     */
    private DeskSheet sheet = DeskSheet.NONE;

    public ExamDeskBlockEntity(BlockPos pos, BlockState state) {
        super(InitBlockEntities.EXAM_DESK.get(), pos, state);
    }

    /** @return the desk at {@code pos}, while it is in {@code level} and loaded */
    public static Optional<ExamDeskBlockEntity> at(Level level, GlobalPos pos) {
        return Optional.of(pos)
            .filter(at -> at.dimension().equals(level.dimension()) && level.isLoaded(at.pos()))
            .flatMap(at -> level.getBlockEntity(at.pos(), InitBlockEntities.EXAM_DESK.get()))
            .filter(found -> found.part() == DeskPart.DESK);
    }

    /** @return whether a pet with that place loaded would find no desk there any more */
    public static boolean isGone(Level level, GlobalPos pos) {
        return pos.dimension().equals(level.dimension()) && level.isLoaded(pos.pos()) && at(level, pos).isEmpty();
    }

    /** @return the way the desk faces: the way a pet sitting at it looks */
    public Direction facing() {
        return getBlockState().getValue(ExamDeskBlock.FACING);
    }

    /** @return which half of the desk this is */
    public DeskPart part() {
        return getBlockState().getValue(ExamDeskBlock.PART);
    }

    /** @return the chair its pet sits on */
    public BlockPos chair() {
        return ExamDeskBlock.chair(worldPosition, facing());
    }

    /** @return who is signed up here, while the desk has anything to show for it */
    public Optional<DeskBooking> booking() {
        return booking;
    }

    /** Whether a pet may be signed up here now: nobody is at the desk with their exam still on. */
    public boolean isFree() {
        return booking.filter(held -> held.holdsDesk(level().getDayTime())).isEmpty();
    }

    /** {@code pet} is signed up here. */
    public void book(DeskBooking signedUp) {
        booking = Optional.of(signedUp);
        changed();
    }

    /** {@code pet} handed its paper in here. */
    public void handIn(UUID pet, boolean passed) {
        booking = booking.map(held -> held.pet().equals(pet) ? held.handedIn(passed) : held);
        changed();
    }

    /** @return the sheet on the desk */
    public DeskSheet sheet() {
        return sheet;
    }

    /** Lets a booking with nothing more to show lapse, and puts the sheet on the desk that the booking calls for. */
    private void refresh() {
        long dayTime = level().getDayTime();
        if (booking.isPresent() && !booking.get().current(dayTime)) {
            booking = Optional.empty();
            setChanged();
        }
        DeskSheet now = booking.map(held -> held.sheet(dayTime)).orElse(DeskSheet.NONE);
        if (now != sheet) {
            sheet = now;
            level().sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    private void changed() {
        setChanged();
        refresh();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ExamDeskBlockEntity desk) {
        if (level.getGameTime() % CLOCK_TICKS == 0) {
            desk.refresh();
        }
    }

    /** What a player's game is told of the desk: only the sheet on it. Only read here, as the labor board's plates are. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putByte("Sheet", (byte) sheet.ordinal());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private Level level() {
        return Objects.requireNonNull(level, "exam desk is not in a level");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        booking.flatMap(held -> DeskBooking.CODEC.encodeStart(NbtOps.INSTANCE, held).result())
            .ifPresent(encoded -> tag.put("Booking", encoded));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        booking = tag.contains("Booking", Tag.TAG_COMPOUND)
            ? DeskBooking.CODEC.parse(NbtOps.INSTANCE, tag.get("Booking")).result()
            : Optional.empty();
        // A save holds the booking and the sheet follows from it within a second; a player's
        // game is sent the sheet alone.
        sheet = DeskSheet.byOrdinal(tag.getByte("Sheet"));
    }
}
