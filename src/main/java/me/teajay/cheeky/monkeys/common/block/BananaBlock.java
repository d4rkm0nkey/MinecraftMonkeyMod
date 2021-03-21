package me.teajay.cheeky.monkeys.common.block;

import java.util.Random;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class BananaBlock extends HorizontalFacingBlock implements Fertilizable  {
    public static final IntProperty AGE;
    protected static final VoxelShape[] AGE_TO_EAST_SHAPE;
    protected static final VoxelShape[] AGE_TO_WEST_SHAPE;
    protected static final VoxelShape[] AGE_TO_NORTH_SHAPE;
    protected static final VoxelShape[] AGE_TO_SOUTH_SHAPE;
    
    public BananaBlock(Settings settings) {
        super(settings);
        this.setDefaultState((BlockState)((BlockState)((BlockState)this.stateManager.getDefaultState()).with(FACING, Direction.NORTH)).with(AGE, 0));
    }

    public boolean hasRandomTicks(BlockState state) {
        return (Integer)state.get(AGE) < 2;
    }

    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (world.random.nextInt(10) == 0) {
           int i = (Integer)state.get(AGE);
           if (i < 2) {
              world.setBlockState(pos, (BlockState)state.with(AGE, i + 1), 2);
           }
        }
    }

    @Nullable
    public BlockState getPlacementState(ItemPlacementContext ctx) {
       BlockState blockState = this.getDefaultState();
       WorldView worldView = ctx.getWorld();
       BlockPos blockPos = ctx.getBlockPos();
       Direction[] directions = ctx.getPlacementDirections();
       int numDirs = directions.length;
 
       for(int i = 0; i < numDirs; ++i) {
          Direction direction = directions[i];
          if (direction.getAxis().isHorizontal()) {
             blockState = (BlockState)blockState.with(FACING, direction);
             if (blockState.canPlaceAt(worldView, blockPos)) {
                return blockState;
             }
          }
       }
       return null;
    }

    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        Block block = world.getBlockState(pos.offset((Direction)state.get(FACING))).getBlock();
        return block.isIn(BlockTags.JUNGLE_LOGS);
    }

    @Override
    public boolean isFertilizable(BlockView world, BlockPos pos, BlockState state, boolean isClient) {
        return (Integer)state.get(AGE) < 2;
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        world.setBlockState(pos, (BlockState)state.with(AGE, (Integer)state.get(AGE) + 1), 2);
    }
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return false;
    }

    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        int age = (Integer)state.get(AGE);
        switch((Direction)state.get(FACING)) {
        case SOUTH:
           return AGE_TO_SOUTH_SHAPE[age];
        case NORTH:
        default:
           return AGE_TO_NORTH_SHAPE[age];
        case WEST:
           return AGE_TO_WEST_SHAPE[age];
        case EAST:
           return AGE_TO_EAST_SHAPE[age];
        }
     }

    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState newState, WorldAccess world, BlockPos pos, BlockPos posFrom) {
        return direction == state.get(FACING) && !state.canPlaceAt(world, pos) ? Blocks.AIR.getDefaultState() : super.getStateForNeighborUpdate(state, direction, newState, world, pos, posFrom);
    }

    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, AGE);
    }

    static {
        AGE = Properties.AGE_2;
        AGE_TO_NORTH_SHAPE = new VoxelShape[] {
            VoxelShapes.union(
                Block.createCuboidShape(7.5, 10, 3, 8.5, 11, 4), 
                Block.createCuboidShape(7, 8, 2.5, 9, 10, 4.5)
            ),
            VoxelShapes.union(
                Block.createCuboidShape(7.5, 5.5, 2, 8.5, 6.5, 5),
                Block.createCuboidShape(6.5, 5.5, 3, 9.5, 6.5, 4),
                Block.createCuboidShape(7.5, 6.5, 1, 8.5, 7.5, 6),
                Block.createCuboidShape(7.5, 7.5, 2, 8.5, 8.5, 5),
                Block.createCuboidShape(6.5, 7.5, 3, 9.5, 8.5, 4),
                Block.createCuboidShape(6.5, 8.5, 3, 9.5, 9.5, 4),
                Block.createCuboidShape(7.5, 8.5, 2, 8.5, 9.5, 5),
                Block.createCuboidShape(5.5, 6.5, 3, 10.5, 7.5, 4),
                Block.createCuboidShape(7.5, 5, 3, 8.5, 11, 4)
            ),
            VoxelShapes.union(
                Block.createCuboidShape(7.5, 8, 3.2, 8.5, 9, 5.2),
                Block.createCuboidShape(7.5, 9, 3.6, 8.5, 10, 5.6),
                Block.createCuboidShape(7.7, 8, 3, 9.7, 9, 4),
                Block.createCuboidShape(8.1, 9, 3, 10.1, 10, 4),
                Block.createCuboidShape(7.5, 9, 1.4, 8.5, 10, 3.4),
                Block.createCuboidShape(7.5, 8, 1.8, 8.5, 9, 3.8),
                Block.createCuboidShape(6.3, 8, 3, 8.3, 9, 4),
                Block.createCuboidShape(5.9, 9, 3, 7.9, 10, 4),
                Block.createCuboidShape(8.1, 7, 3, 10.1, 8, 4),
                Block.createCuboidShape(7.7, 6, 3, 9.7, 7, 4),
                Block.createCuboidShape(7.9, 4, 3, 8.9, 5, 4),
                Block.createCuboidShape(8.3, 5, 3, 9.3, 6, 4),
                Block.createCuboidShape(7.5, 7, 1.4, 8.5, 8, 3.4),
                Block.createCuboidShape(7.5, 6, 1.8, 8.5, 7, 3.8),
                Block.createCuboidShape(7.5, 4, 2.6, 8.5, 5, 3.6),
                Block.createCuboidShape(7.5, 5, 2.2, 8.5, 6, 3.2),
                Block.createCuboidShape(6.7, 5, 3, 7.7, 6, 4),
                Block.createCuboidShape(7.1, 4, 3, 8.1, 5, 4),
                Block.createCuboidShape(6.3, 6, 3, 8.3, 7, 4),
                Block.createCuboidShape(5.9, 7, 3, 7.9, 8, 4),
                Block.createCuboidShape(7.5, 5, 3.8, 8.5, 6, 4.8),
                Block.createCuboidShape(7.5, 4, 3.4, 8.5, 5, 4.4),
                Block.createCuboidShape(7.5, 6, 3.2, 8.5, 7, 5.2),
                Block.createCuboidShape(7.5, 7, 3.6, 8.5, 8, 5.6),
                Block.createCuboidShape(7.5, 3, 3, 8.5, 9, 4),
                Block.createCuboidShape(8, 9, 0, 8, 12, 3),
                Block.createCuboidShape(7, 9, 2.5, 9, 10, 4.5),
                Block.createCuboidShape(7.5, 10, 3, 8.5, 11, 4)
            )
        };
        AGE_TO_EAST_SHAPE = new VoxelShape[] {
            VoxelShapes.union(
                Block.createCuboidShape(12, 10, 7.5, 13, 11, 8.5),
                Block.createCuboidShape(11.5, 8, 7, 13.5, 10, 9)
            ),
            VoxelShapes.union(
                Block.createCuboidShape(11, 5.5, 7.5, 14, 6.5, 8.5),
                Block.createCuboidShape(12, 5.5, 6.5, 13, 6.5, 9.5),
                Block.createCuboidShape(10, 6.5, 7.5, 15, 7.5, 8.5),
                Block.createCuboidShape(11, 7.5, 7.5, 14, 8.5, 8.5),
                Block.createCuboidShape(12, 7.5, 6.5, 13, 8.5, 9.5),
                Block.createCuboidShape(12, 8.5, 6.5, 13, 9.5, 9.5),
                Block.createCuboidShape(11, 8.5, 7.5, 14, 9.5, 8.5),
                Block.createCuboidShape(12, 6.5, 5.5, 13, 7.5, 10.5),
                Block.createCuboidShape(12, 5, 7.5, 13, 11, 8.5)
            ),
            VoxelShapes.union(
                Block.createCuboidShape(10.8, 8, 7.5, 12.8, 9, 8.5),
                Block.createCuboidShape(10.4, 9, 7.5, 12.4, 10, 8.5),
                Block.createCuboidShape(12, 8, 7.7, 13, 9, 9.7),
                Block.createCuboidShape(12, 9, 8.1, 13, 10, 10.1),
                Block.createCuboidShape(12.6, 9, 7.5, 14.6, 10, 8.5),
                Block.createCuboidShape(12.2, 8, 7.5, 14.2, 9, 8.5),
                Block.createCuboidShape(12, 8, 6.3, 13, 9, 8.3),
                Block.createCuboidShape(12, 9, 5.9, 13, 10, 7.9),
                Block.createCuboidShape(12, 7, 8.1, 13, 8, 10.1),
                Block.createCuboidShape(12, 6, 7.7, 13, 7, 9.7),
                Block.createCuboidShape(12, 4, 7.9, 13, 5, 8.9),
                Block.createCuboidShape(12, 5, 8.3, 13, 6, 9.3),
                Block.createCuboidShape(12.6, 7, 7.5, 14.6, 8, 8.5),
                Block.createCuboidShape(12.2, 6, 7.5, 14.2, 7, 8.5),
                Block.createCuboidShape(12.4, 4, 7.5, 13.4, 5, 8.5),
                Block.createCuboidShape(12.8, 5, 7.5, 13.8, 6, 8.5),
                Block.createCuboidShape(12, 5, 6.7, 13, 6, 7.7),
                Block.createCuboidShape(12, 4, 7.1, 13, 5, 8.1),
                Block.createCuboidShape(12, 6, 6.3, 13, 7, 8.3),
                Block.createCuboidShape(12, 7, 5.9, 13, 8, 7.9),
                Block.createCuboidShape(11.2, 5, 7.5, 12.2, 6, 8.5),
                Block.createCuboidShape(11.6, 4, 7.5, 12.6, 5, 8.5),
                Block.createCuboidShape(10.8, 6, 7.5, 12.8, 7, 8.5),
                Block.createCuboidShape(10.4, 7, 7.5, 12.4, 8, 8.5),
                Block.createCuboidShape(12, 3, 7.5, 13, 9, 8.5),
                Block.createCuboidShape(13, 9, 8, 16, 12, 8),
                Block.createCuboidShape(11.5, 9, 7, 13.5, 10, 9),
                Block.createCuboidShape(12, 10, 7.5, 13, 11, 8.5)
            )
        };
        AGE_TO_WEST_SHAPE = new VoxelShape[] {
            VoxelShapes.union(
                Block.createCuboidShape(3, 10, 7.5, 4, 11, 8.5),
                Block.createCuboidShape(2.5, 8, 7, 4.5, 10, 9)
            ),
            VoxelShapes.union(
                Block.createCuboidShape(2, 5.5, 7.5, 5, 6.5, 8.5),
                Block.createCuboidShape(3, 5.5, 6.5, 4, 6.5, 9.5),
                Block.createCuboidShape(1, 6.5, 7.5, 6, 7.5, 8.5),
                Block.createCuboidShape(2, 7.5, 7.5, 5, 8.5, 8.5),
                Block.createCuboidShape(3, 7.5, 6.5, 4, 8.5, 9.5),
                Block.createCuboidShape(3, 8.5, 6.5, 4, 9.5, 9.5),
                Block.createCuboidShape(2, 8.5, 7.5, 5, 9.5, 8.5),
                Block.createCuboidShape(3, 6.5, 5.5, 4, 7.5, 10.5),
                Block.createCuboidShape(3, 5, 7.5, 4, 11, 8.5)
            ),
            VoxelShapes.union(
                Block.createCuboidShape(3.2, 8, 7.5, 5.2, 9, 8.5),
                Block.createCuboidShape(3.6, 9, 7.5, 5.6, 10, 8.5),
                Block.createCuboidShape(3, 8, 6.3, 4, 9, 8.3),
                Block.createCuboidShape(3, 9, 5.9, 4, 10, 7.9),
                Block.createCuboidShape(1.4, 9, 7.5, 3.4, 10, 8.5),
                Block.createCuboidShape(1.8, 8, 7.5, 3.8, 9, 8.5),
                Block.createCuboidShape(3, 8, 7.7, 4, 9, 9.7),
                Block.createCuboidShape(3, 9, 8.1, 4, 10, 10.1),
                Block.createCuboidShape(3, 7, 5.9, 4, 8, 7.9),
                Block.createCuboidShape(3, 6, 6.3, 4, 7, 8.3),
                Block.createCuboidShape(3, 4, 7.1, 4, 5, 8.1),
                Block.createCuboidShape(3, 5, 6.7, 4, 6, 7.7),
                Block.createCuboidShape(1.4, 7, 7.5, 3.4, 8, 8.5),
                Block.createCuboidShape(1.8, 6, 7.5, 3.8, 7, 8.5),
                Block.createCuboidShape(2.6, 4, 7.5, 3.6, 5, 8.5),
                Block.createCuboidShape(2.2, 5, 7.5, 3.2, 6, 8.5),
                Block.createCuboidShape(3, 5, 8.3, 4, 6, 9.3),
                Block.createCuboidShape(3, 4, 7.9, 4, 5, 8.9),
                Block.createCuboidShape(3, 6, 7.7, 4, 7, 9.7),
                Block.createCuboidShape(3, 7, 8.1, 4, 8, 10.1),
                Block.createCuboidShape(3.8, 5, 7.5, 4.8, 6, 8.5),
                Block.createCuboidShape(3.4, 4, 7.5, 4.4, 5, 8.5),
                Block.createCuboidShape(3.2, 6, 7.5, 5.2, 7, 8.5),
                Block.createCuboidShape(3.6, 7, 7.5, 5.6, 8, 8.5),
                Block.createCuboidShape(3, 3, 7.5, 4, 9, 8.5),
                Block.createCuboidShape(0, 9, 8, 3, 12, 8),
                Block.createCuboidShape(2.5, 9, 7, 4.5, 10, 9),
                Block.createCuboidShape(3, 10, 7.5, 4, 11, 8.5)
            )
        };
        AGE_TO_SOUTH_SHAPE = new VoxelShape[] {
            VoxelShapes.union(
                Block.createCuboidShape(7.5, 10, 12, 8.5, 11, 13),
                Block.createCuboidShape(7, 8, 11.5, 9, 10, 13.5)
            ),
            VoxelShapes.union(
                Block.createCuboidShape(7.5, 5.5, 11, 8.5, 6.5, 14),
                Block.createCuboidShape(6.5, 5.5, 12, 9.5, 6.5, 13),
                Block.createCuboidShape(7.5, 6.5, 10, 8.5, 7.5, 15),
                Block.createCuboidShape(7.5, 7.5, 11, 8.5, 8.5, 14),
                Block.createCuboidShape(6.5, 7.5, 12, 9.5, 8.5, 13),
                Block.createCuboidShape(6.5, 8.5, 12, 9.5, 9.5, 13),
                Block.createCuboidShape(7.5, 8.5, 11, 8.5, 9.5, 14),
                Block.createCuboidShape(5.5, 6.5, 12, 10.5, 7.5, 13),
                Block.createCuboidShape(7.5, 5, 12, 8.5, 11, 13)
            ),
            VoxelShapes.union(
                Block.createCuboidShape(7.5, 8, 10.8, 8.5, 9, 12.8),
                Block.createCuboidShape(7.5, 9, 10.4, 8.5, 10, 12.4),
                Block.createCuboidShape(6.3, 8, 12, 8.3, 9, 13),
                Block.createCuboidShape(5.9, 9, 12, 7.9, 10, 13),
                Block.createCuboidShape(7.5, 9, 12.6, 8.5, 10, 14.6),
                Block.createCuboidShape(7.5, 8, 12.2, 8.5, 9, 14.2),
                Block.createCuboidShape(7.7, 8, 12, 9.7, 9, 13),
                Block.createCuboidShape(8.1, 9, 12, 10.1, 10, 13),
                Block.createCuboidShape(5.9, 7, 12, 7.9, 8, 13),
                Block.createCuboidShape(6.3, 6, 12, 8.3, 7, 13),
                Block.createCuboidShape(7.1, 4, 12, 8.1, 5, 13),
                Block.createCuboidShape(6.7, 5, 12, 7.7, 6, 13),
                Block.createCuboidShape(7.5, 7, 12.6, 8.5, 8, 14.6),
                Block.createCuboidShape(7.5, 6, 12.2, 8.5, 7, 14.2),
                Block.createCuboidShape(7.5, 4, 12.4, 8.5, 5, 13.4),
                Block.createCuboidShape(7.5, 5, 12.8, 8.5, 6, 13.8),
                Block.createCuboidShape(8.3, 5, 12, 9.3, 6, 13),
                Block.createCuboidShape(7.9, 4, 12, 8.9, 5, 13),
                Block.createCuboidShape(7.7, 6, 12, 9.7, 7, 13),
                Block.createCuboidShape(8.1, 7, 12, 10.1, 8, 13),
                Block.createCuboidShape(7.5, 5, 11.2, 8.5, 6, 12.2),
                Block.createCuboidShape(7.5, 4, 11.6, 8.5, 5, 12.6),
                Block.createCuboidShape(7.5, 6, 10.8, 8.5, 7, 12.8),
                Block.createCuboidShape(7.5, 7, 10.4, 8.5, 8, 12.4),
                Block.createCuboidShape(7.5, 3, 12, 8.5, 9, 13),
                Block.createCuboidShape(8, 9, 13, 8, 12, 16),
                Block.createCuboidShape(7, 9, 11.5, 9, 10, 13.5),
                Block.createCuboidShape(7.5, 10, 12, 8.5, 11, 13)
            )
        };
    }
}