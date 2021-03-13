package me.lumpy.monkey.cheeky.monkeys.common.entity;

import java.util.Random;
import java.util.UUID;

import me.lumpy.monkey.cheeky.monkeys.common.CheekyMonkeys;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

public class MonkeyEntity extends TameableEntity implements IAnimatable {
	private AnimationFactory factory = new AnimationFactory(this);

	public MonkeyEntity(EntityType<? extends TameableEntity> entityType, World world) {
		super(entityType, world);
		this.ignoreCameraFrustum = false;
	}

	@Override
	public AnimationFactory getFactory() {
		return this.factory;
	}

	private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event)
    {
        event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.monkey.walk", true));
        return PlayState.CONTINUE;
    }

	@Override
	public void registerControllers(AnimationData data) {
		data.addAnimationController(new AnimationController<MonkeyEntity>(this, "controller", 0, this::predicate));	
	}

	@Override
	public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
		MonkeyEntity monkeyEntity = CheekyMonkeys.MONKEY.create(world);
		UUID ownerUuid = this.getOwnerUuid();
        if (ownerUuid != null) {
            monkeyEntity.setOwnerUuid(ownerUuid);
            monkeyEntity.setTamed(true);
        }
		return monkeyEntity;
	}

	public static boolean canSpawn(EntityType<MonkeyEntity> entityType, ServerWorldAccess serverWorldAccess, SpawnReason spawnReason, BlockPos blockPos, Random random) {
        ServerWorld world = serverWorldAccess.toServerWorld();
		if(world.getBiome(blockPos).getCategory() == Biome.Category.JUNGLE) {
			return true;
		};
        return true; // ToDo should be false
    }

	public static DefaultAttributeContainer.Builder createEntityAttributes() {
        return MobEntity.createMobAttributes()
			.add(EntityAttributes.GENERIC_MAX_HEALTH, 8.0D)
			.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.5D)
			.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1)
			.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32);
    }
}
