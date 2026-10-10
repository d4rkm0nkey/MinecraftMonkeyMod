package me.teajay.cheeky.monkeys.registry;

import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import me.teajay.cheeky.monkeys.platform.Platform;
import me.teajay.cheeky.monkeys.world.BananaBunchesFeature;
import net.minecraft.core.registries.Registries;

public final class ModFeatures {
    public static final Supplier<MapCodec<BananaBunchesFeature>> BANANA_BUNCHES = Platform.INSTANCE.register(
        Registries.FEATURE_TYPE,
        "banana_bunches",
        key -> BananaBunchesFeature.CODEC
    );

    private ModFeatures() {
    }

    public static void init() {
    }
}
