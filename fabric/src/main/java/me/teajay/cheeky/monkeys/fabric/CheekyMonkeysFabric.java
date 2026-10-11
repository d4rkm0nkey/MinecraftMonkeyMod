package me.teajay.cheeky.monkeys.fabric;

import me.teajay.cheeky.monkeys.CheekyMonkeys;
import me.teajay.cheeky.monkeys.registry.ModEntities;
import me.teajay.cheeky.monkeys.registry.ModItems;
import me.teajay.cheeky.monkeys.registry.ModTags;
import me.teajay.cheeky.monkeys.registry.ModWorldgen;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.GenerationStep;

public final class CheekyMonkeysFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        CheekyMonkeys.init();

        BiomeModifications.addSpawn(
            BiomeSelectors.tag(ModTags.HAS_MONKEYS),
            MobCategory.CREATURE,
            ModEntities.MONKEY.get(),
            ModWorldgen.MONKEY_SPAWN_WEIGHT,
            ModWorldgen.MONKEY_MIN_GROUP,
            ModWorldgen.MONKEY_MAX_GROUP
        );
        BiomeModifications.addSpawn(
            BiomeSelectors.tag(ModTags.HAS_SNOW_MONKEYS),
            MobCategory.CREATURE,
            ModEntities.MONKEY.get(),
            ModWorldgen.SNOW_MONKEY_SPAWN_WEIGHT,
            ModWorldgen.MONKEY_MIN_GROUP,
            ModWorldgen.MONKEY_MAX_GROUP
        );
        BiomeModifications.addFeature(BiomeSelectors.tag(ModTags.HAS_MONKEYS), GenerationStep.Decoration.VEGETAL_DECORATION, ModWorldgen.BANANA_BUNCHES);

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS)
            .register(output -> output.insertAfter(Items.SWEET_BERRIES, ModItems.BANANA.get()));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS)
            .register(output -> output.insertAfter(Items.COCOA_BEANS, ModItems.BANANA.get()));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
            .register(output -> output.insertAfter(Items.MOOSHROOM_SPAWN_EGG, ModItems.MONKEY_SPAWN_EGG.get()));
    }
}
