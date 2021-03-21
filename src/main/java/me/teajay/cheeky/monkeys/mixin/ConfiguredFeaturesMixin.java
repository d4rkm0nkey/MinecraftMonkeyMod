package me.teajay.cheeky.monkeys.mixin;

import com.google.common.collect.ImmutableList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;

import me.teajay.cheeky.monkeys.common.world.gen.BananaTreeDecorator;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.gen.UniformIntDistribution;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.ConfiguredFeatures;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.TreeFeatureConfig;
import net.minecraft.world.gen.feature.size.TwoLayersFeatureSize;
import net.minecraft.world.gen.foliage.BlobFoliagePlacer;
import net.minecraft.world.gen.stateprovider.SimpleBlockStateProvider;
import net.minecraft.world.gen.tree.CocoaBeansTreeDecorator;
import net.minecraft.world.gen.tree.LeaveVineTreeDecorator;
import net.minecraft.world.gen.tree.TrunkVineTreeDecorator;
import net.minecraft.world.gen.trunk.StraightTrunkPlacer;

@Mixin(ConfiguredFeatures.class)
public class ConfiguredFeaturesMixin {
    @Accessor("JUNGLE_TREE")
    static ConfiguredFeature<TreeFeatureConfig, ?> getJungleTree() {
        throw new AssertionError();
    }


    private static <FC extends FeatureConfig> ConfiguredFeature<FC, ?> register(int rawId, String id, ConfiguredFeature<FC, ?> configuredFeature) {
        return Registry.register(BuiltinRegistries.CONFIGURED_FEATURE, rawId, id, configuredFeature);
    }

    private static int id = BuiltinRegistries.CONFIGURED_FEATURE.getRawId(getJungleTree());
    @Shadow
    private static final ConfiguredFeature<TreeFeatureConfig, ?> JUNGLE_TREE = register(
        id,
        "jungle_tree",
        Feature.TREE.configure(
            (new TreeFeatureConfig.Builder(
                new SimpleBlockStateProvider(ConfiguredFeaturesAccessor.getJungleLog()),
                new SimpleBlockStateProvider(ConfiguredFeaturesAccessor.getJungleLeaves()),
                new BlobFoliagePlacer(UniformIntDistribution.of(2), UniformIntDistribution.of(0), 3),
                new StraightTrunkPlacer(4, 8, 0),
                new TwoLayersFeatureSize(1, 0, 1))
            ).decorators(ImmutableList.of(
                new CocoaBeansTreeDecorator(0.2F),
                new BananaTreeDecorator(0.2F),
                TrunkVineTreeDecorator.INSTANCE,
                LeaveVineTreeDecorator.INSTANCE)
            ).ignoreVines().build()));
}
