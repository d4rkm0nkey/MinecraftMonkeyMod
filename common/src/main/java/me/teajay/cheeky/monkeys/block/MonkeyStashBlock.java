package me.teajay.cheeky.monkeys.block;

import me.teajay.cheeky.monkeys.block.entity.MonkeyStashBlockEntity;
import me.teajay.cheeky.monkeys.registry.ModAdvancements;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/// A messy nest of twigs and leaves up in a tree, where a troop of monkeys hides what it steals.
/// Anyone who finds one can open it and take their things back.
public class MonkeyStashBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(
        Block.box(1.0, 0.0, 1.0, 15.0, 2.0, 15.0),
        Block.box(0.0, 2.0, 0.0, 16.0, 6.0, 2.0),
        Block.box(0.0, 2.0, 14.0, 16.0, 6.0, 16.0),
        Block.box(0.0, 2.0, 2.0, 2.0, 6.0, 14.0),
        Block.box(14.0, 2.0, 2.0, 16.0, 6.0, 14.0)
    );

    public MonkeyStashBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MonkeyStashBlockEntity stash) {
            player.openMenu(stash);
            if (player instanceof ServerPlayer serverPlayer) {
                ModAdvancements.award(serverPlayer, ModAdvancements.FINDERS_KEEPERS);
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MonkeyStashBlockEntity(pos, state);
    }
}
