package com.dwinovo.chiikawa.block;

import com.dwinovo.chiikawa.init.InitBlockEntities;
import com.dwinovo.chiikawa.network.ExamDeskServerPacketHandler;
import com.mojang.serialization.MapCodec;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The exam desk (試験机): a school desk with its chair behind it, two blocks placed and
 * broken together as a bed is. An owner signs a pet up at it for a licence exam, one pet at
 * a time; the pet sits on the chair facing the way the desk faces and writes on the desk.
 * Who is signed up lives in the desk half's {@link ExamDeskBlockEntity}; signing up is
 * {@link com.dwinovo.chiikawa.qualification.ExamEnrollment}'s business.
 */
public class ExamDeskBlock extends BaseEntityBlock implements PropAtRest {
    public static final MapCodec<ExamDeskBlock> CODEC = simpleCodec(ExamDeskBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DeskPart> PART = EnumProperty.create("part", DeskPart.class);
    /** How high the chair's seat is, in pixels; its model is built to it. */
    public static final int SEAT_HEIGHT = 4;
    /** How high the desk's top is, in pixels: a pet on the chair has its chin at it. */
    public static final int DESK_HEIGHT = 7;
    /** How far from the desk a pet sits on the chair, in pixels: close enough to write on it. */
    public static final int SEAT_INSET = 3;
    /**
     * Where on the chair its pet sits, in pixels from the middle of the desk's floor, the
     * desk facing {@code -Z} as its model does: on the seat, up against the desk.
     */
    public static final Vec3 SEAT = new Vec3(0, SEAT_HEIGHT, 8 + SEAT_INSET);

    // Every box is given for the desk facing north: the desk to the north, its chair to the south.
    /** The desk's top and legs, out to the edge its pet sits at. */
    private static final Map<Direction, VoxelShape> DESK_OUTLINE = byFacing(1, 0, 4, 15, DESK_HEIGHT, 16);
    /** What of the desk stands in the way: short of its pet's edge, so a pet sitting up close is not inside it. */
    private static final Map<Direction, VoxelShape> DESK_SOLID = byFacing(1, 0, 4, 15, DESK_HEIGHT, 14);
    /** The chair's seat, out to the desk, and its back. */
    private static final Map<Direction, VoxelShape> CHAIR_OUTLINE = union(
        byFacing(2, 0, 1, 14, SEAT_HEIGHT, 13), byFacing(2, SEAT_HEIGHT, 12, 14, 11, 13));
    /** Only the seat stands in the way, so a pet can step up onto it from any side. */
    private static final Map<Direction, VoxelShape> CHAIR_SOLID = byFacing(2, 0, 1, 14, SEAT_HEIGHT, 13);

    public ExamDeskBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, DeskPart.DESK));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /** The chair of a desk at {@code desk} facing {@code facing}: the block behind it. */
    public static BlockPos chair(BlockPos desk, Direction facing) {
        return desk.relative(DeskPart.DESK.toOther(facing));
    }

    /** Where on the chair its pet sits, in the world; see {@link #SEAT}. */
    public static Vec3 seatPoint(BlockPos desk, Direction facing) {
        double out = SEAT.z / 16.0;
        return Vec3.atBottomCenterOf(desk).add(-facing.getStepX() * out, SEAT.y / 16.0, -facing.getStepZ() * out);
    }

    /** A desk with nobody signed up at it has no sheet on it. */
    @Override
    public boolean shownAtRest(String bone) {
        return !DeskSheet.isSheet(bone);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(PART) == DeskPart.DESK ? DESK_OUTLINE : CHAIR_OUTLINE).get(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(PART) == DeskPart.DESK ? DESK_SOLID : CHAIR_SOLID).get(state.getValue(FACING));
    }

    /** Pets walk round the desk, as they do round a bed, and up onto the chair. */
    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return state.getValue(PART) == DeskPart.CHAIR && super.isPathfindable(state, type);
    }

    /** The desk faces whoever places it, its chair on the far side: the pet sits looking at them. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos chair = chair(context.getClickedPos(), facing);
        Level level = context.getLevel();
        return level.getBlockState(chair).canBeReplaced(context) && level.getWorldBorder().isWithinBounds(chair)
            ? defaultBlockState().setValue(FACING, facing)
            : null;
    }

    /** Puts the chair behind the desk, as a bed puts its head beyond its foot. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            level.setBlock(chair(pos, state.getValue(FACING)), state.setValue(PART, DeskPart.CHAIR), Block.UPDATE_ALL);
            level.updateNeighborsAt(pos, Blocks.AIR);
            state.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
        }
    }

    /** Either half goes when the other does, as a bed's do. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        if (direction == state.getValue(PART).toOther(state.getValue(FACING))) {
            return neighbour.is(this) && neighbour.getValue(PART) != state.getValue(PART)
                ? state
                : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbour, random);
    }

    /**
     * The desk is what drops. A player in creative breaking the chair takes the desk with it
     * without dropping anything, as one breaking the foot of a bed takes the head.
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && state.getValue(PART) == DeskPart.CHAIR) {
            BlockPos desk = pos.relative(DeskPart.CHAIR.toOther(state.getValue(FACING)));
            BlockState deskState = level.getBlockState(desk);
            if (deskState.is(this) && deskState.getValue(PART) == DeskPart.DESK) {
                level.setBlock(desk, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                level.levelEvent(player, 2001, desk, Block.getId(deskState));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Opens the sign-up screen, from either half: the owner's pets nearby, and the odds of each. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockPos desk = state.getValue(PART) == DeskPart.DESK ? pos : pos.relative(DeskPart.CHAIR.toOther(state.getValue(FACING)));
        if (player instanceof ServerPlayer serverPlayer) {
            level.getBlockEntity(desk, InitBlockEntities.EXAM_DESK.get())
                .ifPresent(found -> ExamDeskServerPacketHandler.show(found, serverPlayer));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    /** Both halves have one, as both halves of a bed do, to be drawn by; only the desk's keeps anything. */
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ExamDeskBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || state.getValue(PART) != DeskPart.DESK ? null
            : createTickerHelper(type, InitBlockEntities.EXAM_DESK.get(), ExamDeskBlockEntity::serverTick);
    }

    /** A box given facing north, turned to face each way about the middle of the block. */
    private static Map<Direction, VoxelShape> byFacing(double x1, double y1, double z1, double x2, double y2, double z2) {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        shapes.put(Direction.NORTH, Block.box(x1, y1, z1, x2, y2, z2));
        shapes.put(Direction.SOUTH, Block.box(16 - x2, y1, 16 - z2, 16 - x1, y2, 16 - z1));
        shapes.put(Direction.EAST, Block.box(16 - z2, y1, x1, 16 - z1, y2, x2));
        shapes.put(Direction.WEST, Block.box(z1, y1, 16 - x2, z2, y2, 16 - x1));
        return shapes;
    }

    private static Map<Direction, VoxelShape> union(Map<Direction, VoxelShape> a, Map<Direction, VoxelShape> b) {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        a.forEach((facing, shape) -> shapes.put(facing, Shapes.or(shape, b.get(facing))));
        return shapes;
    }
}
