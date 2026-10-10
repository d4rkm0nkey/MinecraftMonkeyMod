package me.teajay.cheeky.monkeys.world;

import com.mojang.serialization.MapCodec;
import me.teajay.cheeky.monkeys.block.BananaBunchBlock;
import me.teajay.cheeky.monkeys.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/// Hangs a banana bunch on the side of an existing jungle trunk, right below the canopy.
///
/// Placed at the top of the canopy, it scans down a few nearby columns for the first open spot next to a trunk.
/// Runs after the jungle's trees have grown, so it works for every jungle tree shape, mega trees included,
/// and it may replace the vines that cover most jungle trunks.
public record BananaBunchesFeature() implements Feature {
    public static final MapCodec<BananaBunchesFeature> CODEC = MapCodec.unit(BananaBunchesFeature::new);
    private static final int COLUMN_ATTEMPTS = 8;
    private static final int SEARCH_RADIUS = 3;
    private static final int MAX_DEPTH = 24;
    /// How far above the bunch the canopy may start
    private static final int MAX_CANOPY_DISTANCE = 4;

    @Override
    public MapCodec<BananaBunchesFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int attempt = 0; attempt < COLUMN_ATTEMPTS; attempt++) {
            int x = origin.getX() + random.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
            int z = origin.getZ() + random.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;

            for (int y = origin.getY(); y > origin.getY() - MAX_DEPTH; y--) {
                cursor.set(x, y, z);
                if (isOpen(level, cursor) && isBelowCanopy(level, cursor) && tryPlaceBunch(level, random, cursor)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean tryPlaceBunch(WorldGenLevel level, RandomSource random, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL.shuffledCopy(random)) {
            BlockState bunch = ModBlocks.BANANA_BUNCH.get()
                .defaultBlockState()
                .setValue(BananaBunchBlock.FACING, direction)
                .setValue(BananaBunchBlock.AGE, random.nextInt(BananaBunchBlock.MAX_AGE + 1));

            if (bunch.canSurvive(level, pos)) {
                level.setBlock(pos, bunch, 2);
                return true;
            }
        }

        return false;
    }

    private static boolean isOpen(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.is(BlockTags.REPLACEABLE) && state.getFluidState().isEmpty();
    }

    private static boolean isBelowCanopy(WorldGenLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int i = 0; i < MAX_CANOPY_DISTANCE; i++) {
            if (level.getBlockState(cursor.move(Direction.UP)).is(BlockTags.LEAVES)) {
                return true;
            }
        }

        return false;
    }
}
