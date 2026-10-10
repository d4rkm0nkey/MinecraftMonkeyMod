package me.teajay.cheeky.monkeys.registry;

import java.util.function.Supplier;
import me.teajay.cheeky.monkeys.entity.MonkeyEntity;
import me.teajay.cheeky.monkeys.platform.Platform;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ModEntities {
    public static final Supplier<EntityType<MonkeyEntity>> MONKEY = Platform.INSTANCE.register(
        Registries.ENTITY_TYPE,
        "monkey",
        key -> Platform.INSTANCE.mobBuilder(
                MonkeyEntity::new,
                MobCategory.CREATURE,
                MonkeyEntity::createAttributes,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                MonkeyEntity::checkMonkeySpawnRules
            )
            .sized(0.5F, 0.7F)
            .eyeHeight(0.55F)
            .clientTrackingRange(10)
            .build(key)
    );

    private ModEntities() {
    }

    public static void init() {
    }
}
