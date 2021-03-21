package me.teajay.cheeky.monkeys.common.world.gen.tree;

import com.google.common.collect.ImmutableList;

import me.teajay.cheeky.monkeys.common.CheekyMonkeys;
import me.teajay.cheeky.monkeys.common.world.gen.BananaTreeDecorator;
import me.teajay.cheeky.monkeys.mixin.ConfiguredFeaturesAccessor;
import me.teajay.cheeky.monkeys.mixin.ConfiguredFeaturesStatesAccessor;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.gen.UniformIntDistribution;
import net.minecraft.world.gen.decorator.CountExtraDecoratorConfig;
import net.minecraft.world.gen.decorator.Decorator;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.ConfiguredFeatures;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.RandomFeatureConfig;
import net.minecraft.world.gen.feature.TreeFeatureConfig;
import net.minecraft.world.gen.feature.size.TwoLayersFeatureSize;
import net.minecraft.world.gen.foliage.BlobFoliagePlacer;
import net.minecraft.world.gen.stateprovider.SimpleBlockStateProvider;
import net.minecraft.world.gen.tree.LeaveVineTreeDecorator;
import net.minecraft.world.gen.tree.TrunkVineTreeDecorator;
import net.minecraft.world.gen.trunk.StraightTrunkPlacer;

public class BananaTreeRegister {

    public static ConfiguredFeature<TreeFeatureConfig, ?> registerBananaTree() {
        return register(
			"banana_tree",
			Feature.TREE.configure(
				(new TreeFeatureConfig.Builder(
					new SimpleBlockStateProvider(ConfiguredFeaturesStatesAccessor.getJungleLog()),
					new SimpleBlockStateProvider(ConfiguredFeaturesStatesAccessor.getJungleLeaves()),
					new BlobFoliagePlacer(UniformIntDistribution.of(2), UniformIntDistribution.of(0), 3),
					new StraightTrunkPlacer(4, 8, 0),
					new TwoLayersFeatureSize(1, 0, 1))
				).decorators(ImmutableList.of(
					new BananaTreeDecorator(0.2F),
					TrunkVineTreeDecorator.INSTANCE,
					LeaveVineTreeDecorator.INSTANCE)
				).ignoreVines().build()));
    }

    public static final ConfiguredFeature<?, ?> registerBananaTrees() {
        return register(
            "trees_jungle_banana", 
            Feature.RANDOM_SELECTOR.configure(
                new RandomFeatureConfig(
                    ImmutableList.of(
                        ConfiguredFeaturesAccessor.getFancyOak().withChance(0.1F),
                        ConfiguredFeaturesAccessor.getJungleBush().withChance(0.5F),
                        ConfiguredFeaturesAccessor.getMegaJungleTree().withChance(0.33333334F)),
                        CheekyMonkeys.BANANA_TREE)
                    ).decorate(ConfiguredFeatures.Decorators.SQUARE_HEIGHTMAP)
                    .decorate(Decorator.COUNT_EXTRA.configure(new CountExtraDecoratorConfig(50, 0.1F, 1))));
    }

    private static <FC extends FeatureConfig> ConfiguredFeature<FC, ?> register(String id, ConfiguredFeature<FC, ?> configuredFeature) {
        return Registry.register(BuiltinRegistries.CONFIGURED_FEATURE, id, configuredFeature);
    }
}
