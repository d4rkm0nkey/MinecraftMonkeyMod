package me.lumpy.monkey.cheeky.monkeys.common;

import me.lumpy.monkey.cheeky.monkeys.common.entity.MonkeyEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.world.Heightmap;
import software.bernie.geckolib3.GeckoLib;
import net.minecraft.util.registry.Registry;

public class CheekyMonkeys implements ModInitializer {
    public static final String MODID = "cheeky-monkeys";
	public static EntityType<MonkeyEntity> MONKEY;
	public static Item MONKEY_SPAWN_EGG;

	@Override
	public void onInitialize() {
        GeckoLib.initialize();

		// REGISTER MONKEY
		MONKEY = registryEntity(
			"monkey",
			FabricEntityTypeBuilder
				.createMob()
				.entityFactory(MonkeyEntity::new)
				.spawnGroup(SpawnGroup.AMBIENT)
				.dimensions(EntityDimensions.changing(0.8F, 0.4F))
				.trackRangeBlocks(8)
				.spawnRestriction(
					SpawnRestriction.Location.ON_GROUND, 
					Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, 
					MonkeyEntity::canSpawn
				).build()
		);
		FabricDefaultAttributeRegistry.register(MONKEY, MonkeyEntity.createEntityAttributes());
		// REGISTER MONKEY SPAWN EGG
		MONKEY_SPAWN_EGG = registerItem(
			new SpawnEggItem(
				MONKEY, 
				0x583D1E,
				0xECC892,
				(new Item.Settings())
					.group(ItemGroup.MISC)),
				"monkey_spawn_egg"
			);
	}

	private EntityType<MonkeyEntity> registryEntity(String string, EntityType<MonkeyEntity> entityType) {
		return Registry.register(Registry.ENTITY_TYPE, MODID + ":" + string, entityType);
	}

	public static Item registerItem(Item item, String name) {
        Registry.register(Registry.ITEM, MODID + ":" + name, item);
        return item;
    }
}