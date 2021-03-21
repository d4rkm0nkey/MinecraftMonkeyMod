package me.teajay.cheeky.monkeys.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.block.BlockState;
import net.minecraft.world.gen.feature.ConfiguredFeatures;

@Mixin(ConfiguredFeatures.States.class)
public interface ConfiguredFeaturesStatesAccessor {
    @Accessor("JUNGLE_LOG")
    static BlockState getJungleLog() {
        throw new AssertionError();
    }

    @Accessor("JUNGLE_LEAVES")
    static BlockState getJungleLeaves() {
        throw new AssertionError();
    }
}