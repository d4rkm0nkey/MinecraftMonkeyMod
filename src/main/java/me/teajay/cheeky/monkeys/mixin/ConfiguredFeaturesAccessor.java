package me.teajay.cheeky.monkeys.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.ConfiguredFeatures;
import net.minecraft.world.gen.feature.TreeFeatureConfig;

@Mixin(ConfiguredFeatures.class)
public interface ConfiguredFeaturesAccessor {
    @Accessor("FANCY_OAK")
    static ConfiguredFeature<TreeFeatureConfig, ?> getFancyOak() {
        throw new AssertionError();
    }

    @Accessor("JUNGLE_BUSH")
    static ConfiguredFeature<TreeFeatureConfig, ?> getJungleBush() {
        throw new AssertionError();
    }

    @Accessor("MEGA_JUNGLE_TREE")
    static ConfiguredFeature<TreeFeatureConfig, ?> getMegaJungleTree() {
        throw new AssertionError();
    }
}
