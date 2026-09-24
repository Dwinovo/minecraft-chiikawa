package com.dwinovo.chiikawa.block;

import com.dwinovo.chiikawa.network.ShopPayloads.PriceView;
import com.dwinovo.chiikawa.network.ShopPayloads.ShopPricesPayload;
import com.dwinovo.chiikawa.platform.Services;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The shop: a counter a pet walks up to and spends its wages at, and where an owner trades
 * what the farm brought in. A market stall under a pink-and-cream awning, with bread, jam
 * and a cake out on the counter, drawn from its own model by its block entity's renderer.
 *
 * <p>Solid, unlike the labor board — a counter is a thing you stand at, and a pet that
 * could walk through it would be serving itself from the wrong side.
 */
public class ShopBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** The stall up to its awning, the counter's top and the awning overhanging the customer's side. */
    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
        Direction.NORTH, Block.box(0, 0, 0, 16, 16, 12.5),
        Direction.SOUTH, Block.box(0, 0, 3.5, 16, 16, 16),
        Direction.EAST, Block.box(3.5, 0, 0, 16, 16, 16),
        Direction.WEST, Block.box(0, 0, 0, 12.5, 16, 16)
    );

    public ShopBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    /** The counter faces whoever puts it down, so they end up behind it. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShopBlockEntity(pos, state);
    }

    /** Shows the price list: what is on the shelf, and what the shop will take off you. */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof ShopBlockEntity shop) {
            List<PriceView> prices = shop.catalog().entries().stream()
                .map(entry -> new PriceView(BuiltInRegistries.ITEM.getKey(entry.item()), entry.buy(), entry.sell()))
                .toList();
            Services.NETWORK.sendToClient(serverPlayer, new ShopPricesPayload(pos, prices));
        }
        return InteractionResult.CONSUME;
    }
}
