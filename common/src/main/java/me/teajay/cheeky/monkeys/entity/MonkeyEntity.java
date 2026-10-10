package me.teajay.cheeky.monkeys.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.util.GeckoLibUtil;
import me.teajay.cheeky.monkeys.entity.ai.MonkeyIdleEmoteGoal;
import me.teajay.cheeky.monkeys.registry.ModEntities;
import me.teajay.cheeky.monkeys.registry.ModSounds;
import me.teajay.cheeky.monkeys.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MonkeyEntity extends TamableAnimal implements GeoEntity {
    public static final String BASE_CONTROLLER = "base";
    public static final String ACTION_CONTROLLER = "action";
    /// Render data captured by the renderer for the animation controllers
    public static final DataTicket<MonkeyState> STATE_TICKET = DataTicket.create("cheeky_monkeys_state", MonkeyState.class);
    public static final DataTicket<Float> MOVE_SPEED_TICKET = DataTicket.create("cheeky_monkeys_move_speed", Float.class);

    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(MonkeyEntity.class, EntityDataSerializers.BYTE);
    /// Walk animation speed above which the monkey switches to its running gallop
    private static final float RUN_THRESHOLD = 0.55F;
    /// Ticks in the air before a jump or fall counts as a leap, so small steps don't flicker the pose
    private static final int LEAP_AIR_TICKS = 4;
    private static final double JUKEBOX_RANGE = 3.46;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int emoteTicks;
    private int eatingTicks;
    private int airTicks;
    private @Nullable BlockPos jukebox;
    private boolean dancing;

    public MonkeyEntity(EntityType<? extends MonkeyEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
            .add(Attributes.MAX_HEALTH, 10.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.ATTACK_DAMAGE, 2.0)
            .add(Attributes.FOLLOW_RANGE, 24.0)
            // Monkeys are built for falling out of trees
            .add(Attributes.SAFE_FALL_DISTANCE, 6.0)
            .add(Attributes.FALL_DAMAGE_MULTIPLIER, 0.5);
    }

    public static boolean checkMonkeySpawnRules(
        EntityType<MonkeyEntity> type, LevelAccessor level, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random
    ) {
        return level.getBlockState(pos.below()).is(ModTags.MONKEYS_SPAWNABLE_ON) && isBrightEnoughToSpawn(level, pos);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_STATE, (byte)MonkeyState.IDLE.ordinal());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TamableAnimal.TamableAnimalPanicGoal(1.4));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.1, this::isFood, false));
        this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.2, 8.0F, 2.0F));
        this.goalSelector.addGoal(6, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(7, new MonkeyIdleEmoteGoal(this));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
    }

    public MonkeyState getState() {
        return MonkeyState.byId(this.entityData.get(DATA_STATE));
    }

    private void setState(MonkeyState state) {
        if (this.getState() != state) {
            this.entityData.set(DATA_STATE, (byte)state.ordinal());
        }
    }

    public boolean isEmoting() {
        return this.emoteTicks > 0;
    }

    /// Plays a one-off animation for everyone tracking this monkey. Server side only.
    public void playEmote(MonkeyEmote emote) {
        this.emoteTicks = emote.durationTicks();
        this.triggerAnim(ACTION_CONTROLLER, emote.animationName());

        SoundEvent sound = emote.sound();
        if (sound != null) {
            this.playSound(sound, 1.0F, this.getVoicePitch());
        }
    }

    /// Holds a piece of food in hand and munches on it
    private void startEating(ItemStack food) {
        this.setItemSlot(EquipmentSlot.MAINHAND, food.copyWithCount(1));
        this.eatingTicks = MonkeyEmote.EAT.durationTicks();
        this.playEmote(MonkeyEmote.EAT);
    }

    @Override
    public void aiStep() {
        if (this.jukebox == null || !this.jukebox.closerToCenterThan(this.position(), JUKEBOX_RANGE)
            || !this.level().getBlockState(this.jukebox).is(Blocks.JUKEBOX)) {
            this.dancing = false;
            this.jukebox = null;
        }

        super.aiStep();

        if (!this.level().isClientSide()) {
            this.tickEmotes();
            this.updateState();
        }
    }

    private void tickEmotes() {
        if (this.emoteTicks > 0) {
            this.emoteTicks--;
        }

        if (this.eatingTicks > 0 && --this.eatingTicks == 0) {
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }

    private void updateState() {
        // Monkeys without AI have no physics, so they never touch the ground; don't leave them mid-leap forever
        boolean airborne = !this.onGround() && !this.onClimbable() && !this.isInWater() && !this.isPassenger() && !this.isNoAi();
        this.airTicks = airborne ? this.airTicks + 1 : 0;

        if (this.isInSittingPose()) {
            this.setState(this.level().isDarkOutside() ? MonkeyState.SLEEPING : MonkeyState.SITTING);
        } else if (this.onClimbable()) {
            this.setState(MonkeyState.CLIMBING);
        } else if (this.airTicks > LEAP_AIR_TICKS) {
            this.setState(MonkeyState.LEAPING);
        } else if (this.dancing) {
            this.setState(MonkeyState.DANCING);
        } else {
            this.setState(MonkeyState.IDLE);
        }
    }

    @Override
    public void setRecordPlayingNearby(BlockPos jukebox, boolean isPlaying) {
        this.jukebox = jukebox;
        this.dancing = isPlaying;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (this.isTame()) {
            if (this.isOwnedBy(player)) {
                if (this.isFood(itemStack) && this.getHealth() < this.getMaxHealth()) {
                    if (!this.level().isClientSide()) {
                        this.startEating(itemStack);
                        this.feed(player, hand, itemStack, 1.0F, 2.0F);
                    }

                    return InteractionResult.SUCCESS;
                }

                InteractionResult parentInteraction = super.mobInteract(player, hand);
                if (!parentInteraction.consumesAction()) {
                    this.setOrderedToSit(!this.isOrderedToSit());
                    this.jumping = false;
                    this.navigation.stop();
                    this.setTarget(null);
                    return InteractionResult.SUCCESS;
                }

                return parentInteraction;
            }
        } else if (this.isFood(itemStack)) {
            if (!this.level().isClientSide()) {
                this.startEating(itemStack);
                this.usePlayerItem(player, hand, itemStack);
                this.tryToTame(player);
                this.setPersistenceRequired();
            }

            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    private void tryToTame(Player player) {
        if (this.random.nextInt(3) == 0) {
            this.tame(player);
            this.navigation.stop();
            this.setTarget(null);
            this.setOrderedToSit(true);
            this.playEmote(MonkeyEmote.HAPPY);
            this.level().broadcastEntityEvent(this, (byte)7);
        } else {
            this.level().broadcastEntityEvent(this, (byte)6);
            // Bananas still put a wild monkey in the mood, so wild troops can be bred too
            if (this.getAge() == 0 && this.canFallInLove()) {
                this.setInLove(player);
            }
        }
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(ModTags.MONKEY_FOOD);
    }

    @Override
    public boolean canMate(Animal partner) {
        if (!(partner instanceof MonkeyEntity monkey) || this.isInSittingPose() || monkey.isInSittingPose()) {
            return false;
        }

        return super.canMate(partner);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        MonkeyEntity baby = ModEntities.MONKEY.get().create(level, EntitySpawnReason.BREEDING);
        if (baby != null && this.isTame()) {
            baby.setOwnerReference(this.getOwnerReference());
            baby.setTame(true, true);
        }

        return baby;
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return !this.isTame() && this.tickCount > 2400;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.getState() == MonkeyState.SLEEPING ? null : ModSounds.MONKEY_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.MONKEY_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.MONKEY_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockState) {
        this.playSound(blockState.getSoundType().getStepSound(), 0.1F, 1.2F);
    }

    // region GeckoLib

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<MonkeyEntity> actions = new AnimationController<>(ACTION_CONTROLLER, 4, test -> PlayState.STOP);
        for (MonkeyEmote emote : MonkeyEmote.values()) {
            actions.triggerableAnim(emote.animationName(), emote.animation());
        }

        controllers.add(new AnimationController<>(BASE_CONTROLLER, 5, this::handleBaseAnimation), actions);
    }

    private PlayState handleBaseAnimation(AnimationTest<MonkeyEntity> test) {
        test.setControllerSpeed(1.0F);

        return switch (test.getDataOrDefault(STATE_TICKET, MonkeyState.IDLE)) {
            case SITTING -> test.setAndContinue(MonkeyAnimations.SIT);
            case SLEEPING -> test.setAndContinue(MonkeyAnimations.SLEEP);
            case CLIMBING -> test.setAndContinue(MonkeyAnimations.CLIMB);
            case HANGING -> test.setAndContinue(MonkeyAnimations.HANG);
            case PERCHING -> test.setAndContinue(MonkeyAnimations.PERCH);
            case LEAPING -> test.setAndContinue(MonkeyAnimations.LEAP);
            case DANCING -> test.setAndContinue(MonkeyAnimations.DANCE);
            case IDLE -> {
                if (!test.isMoving()) {
                    yield test.setAndContinue(MonkeyAnimations.IDLE);
                }

                float speed = test.getDataOrDefault(MOVE_SPEED_TICKET, 0.0F);
                if (speed > RUN_THRESHOLD) {
                    test.setControllerSpeed(Math.clamp(speed * 1.4F, 0.8F, 1.6F));
                    yield test.setAndContinue(MonkeyAnimations.RUN);
                }

                test.setControllerSpeed(Math.clamp(speed * 2.5F, 0.5F, 1.5F));
                yield test.setAndContinue(MonkeyAnimations.WALK);
            }
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    // endregion
}
