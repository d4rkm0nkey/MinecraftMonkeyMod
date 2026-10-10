package me.teajay.cheeky.monkeys.registry;

import me.teajay.cheeky.monkeys.CheekyMonkeys;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class ModWorldgen {
    /// Banana bunches hung on existing jungle trunks, defined in `data/cheeky_monkeys/worldgen`
    public static final ResourceKey<PlacedFeature> BANANA_BUNCHES = ResourceKey.create(Registries.PLACED_FEATURE, CheekyMonkeys.id("banana_bunches"));

    /// Natural spawn settings, shared by the Fabric code and mirrored in the NeoForge biome modifier JSON
    public static final int MONKEY_SPAWN_WEIGHT = 20;
    public static final int MONKEY_MIN_GROUP = 2;
    public static final int MONKEY_MAX_GROUP = 4;

    private ModWorldgen() {
    }
}
