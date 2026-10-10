package me.teajay.cheeky.monkeys.fabric;

import java.util.function.Function;
import java.util.function.Supplier;
import me.teajay.cheeky.monkeys.CheekyMonkeys;
import me.teajay.cheeky.monkeys.platform.Platform;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;

public final class FabricPlatform implements Platform {
    @Override
    public String loaderName() {
        return "Fabric";
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R, T extends R> Supplier<T> register(ResourceKey<? extends Registry<R>> registryKey, String name, Function<ResourceKey<R>, T> factory) {
        Registry<R> registry = (Registry<R>)BuiltInRegistries.REGISTRY.getValue(registryKey.identifier());
        if (registry == null) {
            throw new IllegalArgumentException("Unknown registry " + registryKey.identifier());
        }

        ResourceKey<R> key = ResourceKey.create(registryKey, CheekyMonkeys.id(name));
        T value = Registry.register(registry, key, factory.apply(key));
        if (value instanceof BlockItem blockItem) {
            blockItem.registerBlocks(Item.BY_BLOCK, blockItem);
        }

        return () -> value;
    }

    @Override
    public <T extends Mob> EntityType.Builder<T> mobBuilder(
        EntityType.EntityFactory<T> factory,
        MobCategory category,
        Supplier<AttributeSupplier.Builder> attributes,
        SpawnPlacementType placementType,
        Heightmap.Types heightmap,
        SpawnPlacements.SpawnPredicate<T> spawnPredicate
    ) {
        return FabricEntityType.Builder.createMob(
            factory,
            category,
            builder -> builder.defaultAttributes(attributes).spawnPlacement(placementType, heightmap, spawnPredicate)
        );
    }
}
