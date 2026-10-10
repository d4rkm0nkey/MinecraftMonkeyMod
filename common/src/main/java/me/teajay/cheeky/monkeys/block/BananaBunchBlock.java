package me.teajay.cheeky.monkeys.block;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/// A bunch of bananas hanging from the side of a jungle log. Grows through three stages like cocoa:
/// blossom, young (green) and grown (yellow).
public class BananaBunchBlock extends CocoaBlock {
    private static final List<Map<Direction, VoxelShape>> SHAPES = List.of(
        Shapes.rotateHorizontal(Block.box(7.0, 8.0, 2.5, 9.0, 11.0, 4.5)),
        Shapes.rotateHorizontal(Block.box(5.5, 5.0, 1.0, 10.5, 11.0, 6.0)),
        Shapes.rotateHorizontal(Block.box(5.9, 3.0, 0.0, 10.1, 12.0, 5.6))
    );

    public BananaBunchBlock(Properties properties) {
        super(properties);
    }

    public static boolean isRipe(BlockState state) {
        return state.getValue(AGE) >= MAX_AGE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(AGE)).get(state.getValue(FACING));
    }
}
