package me.teajay.cheeky.monkeys.common.world.gen;

import com.mojang.serialization.Codec;

import me.teajay.cheeky.monkeys.common.CheekyMonkeys;
import me.teajay.cheeky.monkeys.common.block.BananaBlock;
import me.teajay.cheeky.monkeys.common.world.gen.tree.BananaTreeDecoratorType;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.tree.TreeDecorator;
import net.minecraft.world.gen.tree.TreeDecoratorType;

public class BananaTreeDecorator extends TreeDecorator {
   public static final Codec<BananaTreeDecorator> CODEC = Codec.floatRange(0.0F, 1.0F).fieldOf("probability").xmap(BananaTreeDecorator::new, (treeDecorator) -> {
      return treeDecorator.probability;
   }).codec();
   private final float probability;

   public BananaTreeDecorator(float probability) {
      this.probability = probability;
   }

   protected TreeDecoratorType<?> getType() {
      return BananaTreeDecoratorType.BANANA;
   }

   public void generate(StructureWorldAccess world, Random random, List<BlockPos> logPositions, List<BlockPos> leavesPositions, Set<BlockPos> placedStates, BlockBox box) {
      System.out.println("Banana!");
    if (!(random.nextFloat() >= this.probability)) {
         int i = ((BlockPos)logPositions.get(0)).getY();
         logPositions.stream().filter((pos) -> {
            return pos.getY() - i <= 2;
         }).forEach((pos) -> {
            Iterator<Direction> var6 = Direction.Type.HORIZONTAL.iterator();

            while(var6.hasNext()) {
               Direction direction = (Direction)var6.next();
               if (random.nextFloat() <= 0.25F) {
                  Direction direction2 = direction.getOpposite();
                  BlockPos blockPos = pos.add(direction2.getOffsetX(), 0, direction2.getOffsetZ());
                  if (Feature.isAir(world, blockPos)) {
                     BlockState blockState = (BlockState)((BlockState)CheekyMonkeys.BANANA_BLOCK.getDefaultState().with(BananaBlock.AGE, random.nextInt(3))).with(BananaBlock.FACING, direction);
                     this.setBlockStateAndEncompassPosition(world, blockPos, blockState, placedStates, box);
                  }
               }
            }
         });
      }
   }
}
