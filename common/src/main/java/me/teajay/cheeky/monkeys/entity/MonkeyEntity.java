package me.teajay.cheeky.monkeys.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.util.GeckoLibUtil;
import me.teajay.cheeky.monkeys.entity.ai.MonkeyHopGoal;
import me.teajay.cheeky.monkeys.entity.ai.MonkeyIdleEmoteGoal;
import me.teajay.cheeky.monkeys.entity.ai.MonkeyTreeGoal;
import me.teajay.cheeky.monkeys.entity.ai.TreeSpots;
import me.teajay.cheeky.monkeys.registry.ModEntities;
import me.teajay.cheeky.monkeys.registry.ModSounds;
import me.teajay.cheeky.monkeys.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
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
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class MonkeyEntity extends TamableAnimal implements GeoEntity {
    public static final String BASE_CONTROLLER = "base";
    public static final String ACTION_CONTROLLER = "action";
    /// Render data captured by the renderer for the animation controllers
    public static final DataTicket<MonkeyState> STATE_TICKET = DataTicket.create("cheeky_monkeys_state", MonkeyState.class);
    public static final DataTicket<Float> MOVE_SPEED_TICKET = DataTicket.create("cheeky_monkeys_move_speed", Float.class);

    /// How big the model is drawn; the Blockbench model is human sized
    public static final float MODEL_SCALE = 0.6F;
    /// Babies are drawn this much smaller again. GeckoLib only applies the entity's scale attribute, not the baby age scale.
    public static final float BABY_SCALE = 0.6F;

    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(MonkeyEntity.class, EntityDataSerializers.BYTE);
    /// Walk animation speed above which the monkey switches to its running gallop
    private static final float RUN_THRESHOLD = 0.55F;
    /// Ticks in the air before a jump or fall counts as a leap, so small steps don't flicker the pose
    private static final int LEAP_AIR_TICKS = 4;
    private static final double JUKEBOX_RANGE = 3.46;
    private static final double LEAP_SPEED = 0.45;
    private static final int GAP_LEAP_COOLDOWN = 20;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int emoteTicks;
    private int eatingTicks;
    private int airTicks;
    private @Nullable BlockPos jukebox;
    private boolean dancing;
    /// Set while a goal moves the monkey along a scripted path (climbing, swinging, leaping) or holds it in place (hanging).
    /// Physics are off then: no gravity, no collisions, no pushing.
    private boolean scriptedMovement;
    /// Pose a goal wants shown instead of the one worked out from movement, like perching on a branch
    private @Nullable MonkeyState posture;
    private Vec3 glideFrom = Vec3.ZERO;
    private Vec3 glideTo = Vec3.ZERO;
    private double glideArc;
    private int glideTick;
    private int glideDuration;
    private float glideYaw;
    private @Nullable Runnable glideDone;
    private int gapLeapCooldown;
    private boolean treeClimbRequested;

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
        // Hurt monkeys flee up the nearest tree, or just run if there isn't one
        this.goalSelector.addGoal(1, new MonkeyTreeGoal(this, true));
        this.goalSelector.addGoal(2, new TamableAnimal.TamableAnimalPanicGoal(1.4));
        this.goalSelector.addGoal(3, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(4, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(5, new TemptGoal(this, 1.1, this::isFood, false));
        this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.2, 8.0F, 2.0F));
        this.goalSelector.addGoal(7, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(8, new MonkeyTreeGoal(this, false));
        this.goalSelector.addGoal(9, new MonkeyIdleEmoteGoal(this));
        this.goalSelector.addGoal(9, new MonkeyHopGoal(this));
        this.goalSelector.addGoal(10, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(12, new RandomLookAroundGoal(this));
    }

    public MonkeyState getState() {
        return MonkeyState.byId(this.entityData.get(DATA_STATE));
    }

    private void setState(MonkeyState state) {
        if (this.getState() != state) {
            this.entityData.set(DATA_STATE, (byte)state.ordinal());
        }
    }

    /// How big the monkey is drawn compared to the Blockbench model
    public float getVisualScale() {
        return this.isBaby() ? MODEL_SCALE * BABY_SCALE : MODEL_SCALE;
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

    // region Scripted movement

    public boolean isScriptedMovement() {
        return this.scriptedMovement;
    }

    public boolean isGliding() {
        return this.glideTick < this.glideDuration;
    }

    public void setPosture(@Nullable MonkeyState posture) {
        this.posture = posture;
    }

    /// Moves the monkey to `to` at `speed` blocks per tick, bulging `arc` blocks upwards on the way, facing `yaw` (or the
    /// way it's going, if NaN), then runs `then`. Physics stay off afterwards until [#releaseScriptedMovement()].
    public void glideTo(Vec3 to, double speed, MonkeyState pose, float yaw, double arc, @Nullable Runnable then) {
        this.scriptedMovement = true;
        this.posture = pose;
        this.navigation.stop();
        this.glideFrom = this.position();
        this.glideTo = to;
        this.glideArc = arc;
        this.glideYaw = yaw;
        this.glideTick = 0;
        this.glideDuration = Math.max(2, Mth.ceil(this.glideFrom.distanceTo(to) / speed));
        this.glideDone = then;
    }

    /// Leaps through the air to `to` and lands there with physics back on, then runs `then`
    public void leapTo(Vec3 to, @Nullable Runnable then) {
        this.playSound(ModSounds.MONKEY_AMBIENT.get(), 0.8F, this.getVoicePitch());
        this.glideTo(to, LEAP_SPEED, MonkeyState.LEAPING, Float.NaN, TreeSpots.leapArc(this.position(), to), () -> {
            this.releaseScriptedMovement();
            if (then != null) {
                then.run();
            }
        });
    }

    /// Holds the monkey still in the air in `pose`, like hanging from leaves
    public void holdPose(MonkeyState pose) {
        this.scriptedMovement = true;
        this.posture = pose;
        this.glideDuration = 0;
        this.glideDone = null;
    }

    /// Hands the monkey back to physics wherever it is
    public void releaseScriptedMovement() {
        this.scriptedMovement = false;
        this.posture = null;
        this.glideDuration = 0;
        this.glideDone = null;
        this.setDeltaMovement(Vec3.ZERO);
        this.resetFallDistance();
    }

    private void tickGlide() {
        if (!this.isGliding()) {
            return;
        }

        this.glideTick++;
        Vec3 pos = TreeSpots.arcPosition(this.glideFrom, this.glideTo, this.glideArc, this.glideTick / (double)this.glideDuration);
        Vec3 step = pos.subtract(this.position());
        float yaw = this.glideYaw;
        if (Float.isNaN(yaw)) {
            yaw = step.horizontalDistanceSqr() > 1.0E-4 ? (float)(Mth.atan2(step.z, step.x) * Mth.RAD_TO_DEG) - 90.0F : this.getYRot();
        }

        this.setPos(pos);
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);

        // Rustle through the leaves
        if (this.glideTick % 6 == 0) {
            BlockState inside = this.level().getBlockState(BlockPos.containing(pos.add(0.0, this.getBbHeight(), 0.0)));
            if (inside.is(BlockTags.LEAVES)) {
                this.playSound(inside.getSoundType().getStepSound(), 0.3F, 1.1F);
            }
        }

        if (this.glideTick >= this.glideDuration && this.glideDone != null) {
            Runnable done = this.glideDone;
            this.glideDone = null;
            done.run();
        }
    }

    @Override
    public void travel(Vec3 input) {
        if (this.scriptedMovement) {
            this.resetFallDistance();
            return;
        }

        super.travel(input);
    }

    @Override
    public boolean isPushable() {
        return !this.scriptedMovement && super.isPushable();
    }

    /// Leaps over a gap when the path the monkey is following ends short of where it wants to go
    private void tryGapLeap() {
        if (this.gapLeapCooldown > 0) {
            this.gapLeapCooldown--;
            return;
        }

        Path path = this.navigation.getPath();
        // Only once the monkey has got as close as walking gets it
        if (this.scriptedMovement || !this.onGround() || path == null || path.canReach() || !path.isDone()) {
            return;
        }

        this.gapLeapCooldown = GAP_LEAP_COOLDOWN;
        BlockPos landing = TreeSpots.findLeapToward(this.level(), this.position(), Vec3.atBottomCenterOf(path.getTarget()));
        if (landing != null) {
            this.leapTo(Vec3.atBottomCenterOf(landing), null);
        }
    }

    /// Asks the monkey to climb the nearest tree as soon as it can
    public void requestTreeClimb() {
        this.treeClimbRequested = true;
    }

    public boolean isTreeClimbRequested() {
        return this.treeClimbRequested;
    }

    public void clearTreeClimbRequest() {
        this.treeClimbRequested = false;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        this.tickGlide();
        this.tryGapLeap();
    }

    // endregion

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
        boolean airborne = !this.onGround() && !this.onClimbable() && !this.isInWater() && !this.isPassenger() && !this.isNoAi()
            && !this.scriptedMovement;
        this.airTicks = airborne ? this.airTicks + 1 : 0;

        if (this.isInSittingPose()) {
            this.setState(this.level().isDarkOutside() ? MonkeyState.SLEEPING : MonkeyState.SITTING);
        } else if (this.posture != null) {
            this.setState(this.posture);
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
            case SWINGING -> test.setAndContinue(MonkeyAnimations.SWING);
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
