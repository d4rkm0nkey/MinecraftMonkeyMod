package me.teajay.cheeky.monkeys.registry;

import java.util.function.Supplier;
import me.teajay.cheeky.monkeys.platform.Platform;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

public final class ModItems {
    public static final FoodProperties BANANA_FOOD = new FoodProperties.Builder().nutrition(4).saturationModifier(0.3F).build();

    /// Eaten like any fruit, and planted on the side of a jungle log like cocoa beans.
    public static final Supplier<Item> BANANA = Platform.INSTANCE.register(
        Registries.ITEM,
        "banana",
        key -> new BlockItem(ModBlocks.BANANA_BUNCH.get(), new Item.Properties().setId(key).food(BANANA_FOOD).useItemDescriptionPrefix())
    );
    public static final Supplier<Item> MONKEY_SPAWN_EGG = Platform.INSTANCE.register(
        Registries.ITEM,
        "monkey_spawn_egg",
        key -> new SpawnEggItem(new Item.Properties().setId(key).spawnEgg(ModEntities.MONKEY.get()))
    );

    private ModItems() {
    }

    public static void init() {
    }
}
