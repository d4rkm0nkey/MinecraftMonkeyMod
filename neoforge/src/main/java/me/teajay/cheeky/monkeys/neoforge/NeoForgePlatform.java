package me.teajay.cheeky.monkeys.neoforge;

import java.nio.file.Path;
import net.neoforged.fml.loading.FMLPaths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import me.teajay.cheeky.monkeys.CheekyMonkeys;
import me.teajay.cheeky.monkeys.platform.Platform;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jspecify.annotations.Nullable;

public final class NeoForgePlatform implements Platform {
    private final Map<ResourceKey<? extends Registry<?>>, DeferredRegister<?>> registers = new LinkedHashMap<>();
    private final List<MobSetup<?>> mobSetups = new ArrayList<>();
    /// The entity type currently being built, so `mobBuilder` knows which type its settings belong to
    private @Nullable ResourceKey<?> buildingKey;

    @Override
    public String loaderName() {
        return "NeoForge";
    }

    @Override
    public Path configDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R, T extends R> Supplier<T> register(ResourceKey<? extends Registry<R>> registryKey, String name, Function<ResourceKey<R>, T> factory) {
        DeferredRegister<R> register = (DeferredRegister<R>)this.registers.computeIfAbsent(
            registryKey,
            key -> DeferredRegister.create((ResourceKey<Registry<R>>)key, CheekyMonkeys.MOD_ID)
        );

        return register.register(name, id -> {
            ResourceKey<R> key = ResourceKey.create(registryKey, id);
            this.buildingKey = key;
            try {
                return factory.apply(key);
            } finally {
                this.buildingKey = null;
            }
        });
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
        if (this.buildingKey == null) {
            throw new IllegalStateException("mobBuilder must be called from an entity type registration");
        }

        this.mobSetups.add(new MobSetup<>(this.buildingKey.identifier(), attributes, placementType, heightmap, spawnPredicate));
        return EntityType.Builder.of(factory, category);
    }

    void registerAll(IEventBus modBus) {
        this.registers.values().forEach(register -> register.register(modBus));
        modBus.addListener(this::onAttributeCreation);
        modBus.addListener(this::onRegisterSpawnPlacements);
    }

    private void onAttributeCreation(EntityAttributeCreationEvent event) {
        for (MobSetup<?> setup : this.mobSetups) {
            event.put(setup.type(), setup.attributes().get().build());
        }
    }

    private void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        for (MobSetup<?> setup : this.mobSetups) {
            setup.registerPlacement(event);
        }
    }

    private record MobSetup<T extends Mob>(
        Identifier id,
        Supplier<AttributeSupplier.Builder> attributes,
        SpawnPlacementType placementType,
        Heightmap.Types heightmap,
        SpawnPlacements.SpawnPredicate<T> spawnPredicate
    ) {
        @SuppressWarnings("unchecked")
        EntityType<T> type() {
            return (EntityType<T>)BuiltInRegistries.ENTITY_TYPE.getValue(this.id);
        }

        void registerPlacement(RegisterSpawnPlacementsEvent event) {
            event.register(this.type(), this.placementType, this.heightmap, this.spawnPredicate, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
    }
}
