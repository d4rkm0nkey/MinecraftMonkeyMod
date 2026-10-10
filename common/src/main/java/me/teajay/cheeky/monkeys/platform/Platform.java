package me.teajay.cheeky.monkeys.platform;

import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.levelgen.Heightmap;

/// Loader-specific hooks. Each loader ships one implementation, found through `META-INF/services`.
public interface Platform {
    Platform INSTANCE = ServiceLoader.load(Platform.class, Platform.class.getClassLoader())
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No Cheeky Monkeys platform implementation found"));

    String loaderName();

    /// Registers an object under this mod's namespace. The returned supplier is safe to call
    /// once the registry has been populated (immediately on Fabric, after the RegisterEvent on NeoForge).
    <R, T extends R> Supplier<T> register(ResourceKey<? extends Registry<R>> registry, String name, Function<ResourceKey<R>, T> factory);

    /// Creates an entity type builder that also wires up default attributes and spawn placement.
    <T extends Mob> EntityType.Builder<T> mobBuilder(
        EntityType.EntityFactory<T> factory,
        MobCategory category,
        Supplier<AttributeSupplier.Builder> attributes,
        SpawnPlacementType placementType,
        Heightmap.Types heightmap,
        SpawnPlacements.SpawnPredicate<T> spawnPredicate
    );
}
