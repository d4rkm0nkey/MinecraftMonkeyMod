package me.teajay.cheeky.monkeys.common.entity;

import java.util.Random;
import java.util.UUID;

import me.teajay.cheeky.monkeys.common.CheekyMonkeys;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SitGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
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
	private static final TrackedData<Boolean> SITTING = DataTracker.registerData(MonkeyEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private AnimationFactory factory = new AnimationFactory(this);

	public MonkeyEntity(EntityType<? extends TameableEntity> entityType, World world) {
		super(entityType, world);
		this.ignoreCameraFrustum = false;
	}

	@Override
	public AnimationFactory getFactory() {
		return this.factory;
	}

	private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
		if (this.isSitting()) {
			event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.monkey.sit", true));
			return PlayState.CONTINUE;
		} else if (event.isMoving()) {
			event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.monkey.walk", true));
			return PlayState.CONTINUE;
		}
		// not working!
		if (this.goalSelector.getRunningGoals().filter((goal) -> {
			return goal.getGoal() instanceof LookAtEntityGoal;
		}).count() > 0) {
			event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.monkey.stand", true));
		}
		event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.monkey.idle", true));
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

	public static boolean canSpawn(EntityType<MonkeyEntity> entityType, ServerWorldAccess serverWorldAccess,
			SpawnReason spawnReason, BlockPos blockPos, Random random) {

		if (spawnReason == SpawnReason.BREEDING 
			|| spawnReason == SpawnReason.COMMAND
			|| spawnReason == SpawnReason.MOB_SUMMONED 
			|| spawnReason == SpawnReason.TRIGGERED
			|| spawnReason == SpawnReason.SPAWN_EGG
		) return true;

		ServerWorld world = serverWorldAccess.toServerWorld();
		if (world.getBiome(blockPos).getCategory() == Biome.Category.JUNGLE) {
			return true;
		}
		return false;
	}

	public static DefaultAttributeContainer.Builder createEntityAttributes() {
		return MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 8.0D)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25D).add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32);
	}

	protected void initDataTracker() {
		super.initDataTracker();
		this.dataTracker.startTracking(SITTING, false);
	}

	protected void initGoals() {
		this.goalSelector.add(1, new SwimGoal(this));
		this.goalSelector.add(2, new SitGoal(this));
		this.goalSelector.add(3, new WanderAroundFarGoal(this, 1.0D));
		this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
		this.goalSelector.add(5, new LookAroundGoal(this));
	}

	@Override
	public boolean isBreedingItem(ItemStack stack) {
		return stack.getItem() == CheekyMonkeys.BANANA_ITEM;
	}

	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		ItemStack itemStack = player.getStackInHand(hand);
		Item item = itemStack.getItem();
		if (this.world.isClient) {
			boolean bl = this.isOwner(player) || this.isTamed() || item == CheekyMonkeys.BANANA_ITEM && !this.isTamed();
			return bl ? ActionResult.CONSUME : ActionResult.PASS;
		} else {
			if (this.isTamed()) {
				if (this.isBreedingItem(itemStack) && this.getHealth() < this.getMaxHealth()) {
					if (!player.abilities.creativeMode) {
						itemStack.decrement(1);
					}

					this.heal((float) item.getFoodComponent().getHunger());
					return ActionResult.SUCCESS;
				}
				if(!item.isFood()) {
					ActionResult actionResult = super.interactMob(player, hand);
					if ((!actionResult.isAccepted() || this.isBaby()) && this.isOwner(player)) {
					   this.setSitting(!this.isSitting());
					   this.jumping = false;
					   this.navigation.stop();
					   return ActionResult.SUCCESS;
					}
	 
					return actionResult;
				}
			} else if (item == CheekyMonkeys.BANANA_ITEM) {
				if (!player.abilities.creativeMode) {
					itemStack.decrement(1);
				}

				if (this.random.nextInt(3) == 0) {
					this.setOwner(player);
					this.navigation.stop();
					this.setSitting(true);
					this.world.sendEntityStatus(this, (byte) 7);
				} else {
					this.world.sendEntityStatus(this, (byte) 6);
				}

				return ActionResult.SUCCESS;
			}

			return super.interactMob(player, hand);
		}
	}

	@Override
    public boolean isSitting() {
        return this.dataTracker.get(SITTING);
    }

	@Override
    public void setSitting(boolean sitting) {
        this.dataTracker.set(SITTING, sitting);
    }
}
