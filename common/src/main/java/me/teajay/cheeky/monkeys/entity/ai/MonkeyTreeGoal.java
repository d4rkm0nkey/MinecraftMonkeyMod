package me.teajay.cheeky.monkeys.entity.ai;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import me.teajay.cheeky.monkeys.CheekyMonkeysConfig;
import me.teajay.cheeky.monkeys.block.entity.MonkeyStashBlockEntity;
import me.teajay.cheeky.monkeys.entity.MonkeyEmote;
import me.teajay.cheeky.monkeys.entity.MonkeyEntity;
import me.teajay.cheeky.monkeys.entity.MonkeyState;
import me.teajay.cheeky.monkeys.entity.ai.TreeSpots.ClimbRoute;
import me.teajay.cheeky.monkeys.entity.ai.TreeSpots.Waypoint;
import me.teajay.cheeky.monkeys.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/// Monkeys spend their spare time up in the trees. They walk to a trunk and climb it, swing hand over hand along
/// under the canopy to its edge, climb up the outside of the leaves and sit on top. From there they leap across to
/// neighbouring trees, and eventually climb back down the way they came or jump down.
///
/// The other [modes][Mode] send a monkey that just got hurt up the nearest tree, out of reach, or up the tree a player
/// holding a banana is standing on, to come and get it.
public class MonkeyTreeGoal extends Goal {
    public enum Mode {
        /// Up into the trees just because
        LEISURE,
        /// Up the nearest tree after getting hurt
        FLEE,
        /// Up to a player standing in a tree with a banana
        TEMPTED
    }

    private enum Phase { APPROACH, TRAVEL, PERCH, HANG, FOLLOW, DONE }

    private static final int AVERAGE_START_INTERVAL = 600;
    private static final int APPROACH_TIMEOUT = 200;
    private static final int MAX_TREE_LEAPS = 3;
    /// Highest drop a monkey happily jumps down
    private static final int SAFE_DROP = 5;
    /// Highest a hanging monkey lets go from
    private static final int MAX_HANG_DROP = 6;
    private static final double FLEE_SPEED_FACTOR = 1.8;
    private static final double TEMPT_RANGE = 16.0;
    private static final int STASH_SEARCH_RANGE = 8;
    private static final int STASH_GIVE_UP_TICKS = 300;
    private static final int TEMPT_CHECK_INTERVAL = 20;
    private static final MonkeyEmote[] PERCH_EMOTES = {MonkeyEmote.CHEST_POUND, MonkeyEmote.LOOK_AROUND, MonkeyEmote.SCRATCH_HEAD};

    private final MonkeyEntity monkey;
    private final Mode mode;
    private final boolean fleeing;
    private Phase phase = Phase.DONE;
    private int phaseTicks;
    private int phaseDuration;
    private @Nullable ClimbRoute route;
    /// The way back down the tree the monkey climbed; gone once it leaps to another tree
    private @Nullable List<Waypoint> wayBack;
    private int treeLeapsLeft;
    private int cooldownUntil;
    private @Nullable Vec3 lookTarget;
    private @Nullable Player tempter;
    /// Gave up on stashing the loot here, because there's no room for a nest or the nest is full
    private boolean stashFailed;
    private int nextTemptCheck;

    public MonkeyTreeGoal(MonkeyEntity monkey, Mode mode) {
        this.monkey = monkey;
        this.mode = mode;
        this.fleeing = mode == Mode.FLEE;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.monkey.isOrderedToSit() || this.monkey.isPassenger() || this.monkey.isLeashed() || this.monkey.isInWater()
            || this.monkey.isScriptedMovement() || !this.monkey.onGround()) {
            return false;
        }

        if (this.mode == Mode.TEMPTED) {
            return this.canClimbToTempter();
        }

        if (this.fleeing) {
            if (!this.wasHurtRecently() && !this.monkey.isRunningOff()) {
                return false;
            }
        } else if (!this.monkey.isTreeClimbRequested() && !this.isUpInTree()) {
            if (this.monkey.tickCount < this.cooldownUntil || this.wantsToComeDown()
                || this.monkey.getRandom().nextInt(reducedTickDelay(AVERAGE_START_INTERVAL)) != 0) {
                return false;
            }
        }

        if (this.isUpInTree()) {
            this.route = null;
            return true;
        }

        this.route = TreeSpots.findClimb(
            this.monkey.level(), this.monkey.blockPosition(), this.fleeing ? 8 : 12, this.monkey.getRandom(), this.monkey.getVisualScale(), null
        );
        return this.route != null;
    }

    /// A player up in a tree is holding a banana and there's a way up to them
    private boolean canClimbToTempter() {
        if (this.monkey.tickCount < this.nextTemptCheck) {
            return false;
        }

        this.nextTemptCheck = this.monkey.tickCount + TEMPT_CHECK_INTERVAL;
        Player player = this.monkey.level().getNearestPlayer(this.monkey, TEMPT_RANGE);
        if (!this.isTempting(player) || !this.isPlayerUpInTree(player) || this.monkey.distanceToSqr(player) < 6.0) {
            return false;
        }

        this.tempter = player;
        if (this.isUpInTree()) {
            this.route = null;
            return true;
        }

        this.route = TreeSpots.findClimb(
            this.monkey.level(), this.monkey.blockPosition(), (int)TEMPT_RANGE, this.monkey.getRandom(), this.monkey.getVisualScale(), player.position()
        );
        return this.route != null && this.route.perches();
    }

    private boolean isTempting(@Nullable Player player) {
        return player != null && player.isAlive() && !player.isSpectator()
            && (this.monkey.isFood(player.getMainHandItem()) || this.monkey.isFood(player.getOffhandItem()));
    }

    @Override
    public boolean canContinueToUse() {
        if (this.phase == Phase.DONE || !this.monkey.isAlive() || this.monkey.isPassenger() || this.monkey.isLeashed()) {
            return false;
        }

        if (this.mode == Mode.TEMPTED && (this.phase == Phase.APPROACH || this.phase == Phase.FOLLOW) && !this.isStillTempted()) {
            return false;
        }

        // Sit right where you are, unless you're halfway up a trunk or mid-leap
        return !this.monkey.isOrderedToSit() || this.phase != Phase.APPROACH && this.phase != Phase.PERCH && this.phase != Phase.FOLLOW;
    }

    @Override
    public boolean isInterruptable() {
        return this.phase == Phase.APPROACH;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.monkey.clearTreeClimbRequest();
        this.treeLeapsLeft = MAX_TREE_LEAPS;
        this.wayBack = null;

        if (this.route == null) {
            // Already up a tree
            if (this.mode == Mode.TEMPTED) {
                this.followTempter();
            } else {
                this.perch(20 + this.monkey.getRandom().nextInt(60));
            }

            return;
        }

        this.setPhase(Phase.APPROACH);
        this.walkToTrunk();
    }

    @Override
    public void stop() {
        if (this.monkey.isScriptedMovement()) {
            this.monkey.releaseScriptedMovement();
        }

        this.monkey.setPosture(null);
        this.monkey.getNavigation().stop();
        this.phase = Phase.DONE;
        this.route = null;
        this.wayBack = null;
        this.lookTarget = null;
        this.tempter = null;
        this.cooldownUntil = this.monkey.tickCount + switch (this.mode) {
            case LEISURE -> 600 + this.monkey.getRandom().nextInt(1200);
            case FLEE -> 100;
            case TEMPTED -> 0;
        };
    }

    @Override
    public void tick() {
        this.phaseTicks++;
        switch (this.phase) {
            case APPROACH -> this.tickApproach();
            case PERCH -> this.tickPerch();
            case HANG -> this.tickHang();
            case FOLLOW -> this.tickFollow();
            case TRAVEL, DONE -> {
            }
        }
    }

    private void setPhase(Phase phase) {
        this.phase = phase;
        this.phaseTicks = 0;
    }

    private void walkToTrunk() {
        BlockPos start = this.route.start();
        this.monkey.getNavigation().moveTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5, this.fleeing ? 1.5 : 1.0);
    }

    private void tickApproach() {
        Vec3 start = Vec3.atBottomCenterOf(this.route.start());
        double distance = this.monkey.position().subtract(start).horizontalDistance();
        boolean level = Math.abs(this.monkey.getY() - start.y) < 0.6;

        // Jump onto the trunk from a little way off, as long as the trunk isn't in the way
        Vec3 towardsTrunk = Vec3.atLowerCornerOf(this.route.trunkSide().getUnitVec3i());
        boolean inFront = this.monkey.position().subtract(start).dot(towardsTrunk) < 0.2;
        if (level && (distance < 0.4 || distance < 1.8 && inFront)) {
            this.climb();
        } else if (this.phaseTicks > APPROACH_TIMEOUT) {
            if (level && distance < 1.5) {
                this.climb();
            } else {
                this.phase = Phase.DONE;
            }
        } else if (this.monkey.getNavigation().isDone()) {
            if (level && distance < 3.0) {
                // Paths end up to a block short for small mobs; walk the last bit straight to the trunk
                this.monkey.getMoveControl().setWantedPosition(start.x, start.y, start.z, this.fleeing ? 1.5 : 1.0);
            } else {
                this.walkToTrunk();
            }
        }
    }

    private void climb() {
        ClimbRoute climbRoute = this.route;
        this.monkey.getNavigation().stop();
        this.wayBack = climbRoute.reversed();
        this.setPhase(Phase.TRAVEL);
        int stay = this.fleeing ? 400 + this.monkey.getRandom().nextInt(400) : 200 + this.monkey.getRandom().nextInt(400);
        if (this.mode == Mode.TEMPTED) {
            this.follow(climbRoute.path(), 0, this::followTempter);
        } else {
            this.follow(climbRoute.path(), 0, climbRoute.perches() ? () -> this.perch(stay) : () -> this.hang(stay / 2));
        }
    }

    /// Glides along the waypoints from `index` on, then runs `then`
    private void follow(List<Waypoint> path, int index, Runnable then) {
        if (index >= path.size()) {
            then.run();
            return;
        }

        Waypoint leg = path.get(index);
        double speed = this.fleeing ? leg.speed() * FLEE_SPEED_FACTOR : leg.speed();
        this.monkey.glideTo(leg.pos(), speed, leg.pose(), leg.yaw(), leg.arc(), () -> this.follow(path, index + 1, then));
    }

    private void perch(int duration) {
        this.monkey.releaseScriptedMovement();
        this.monkey.setPosture(MonkeyState.PERCHING);
        this.setPhase(Phase.PERCH);
        this.phaseDuration = duration;
        this.stashFailed = false;
    }

    /// Up in the tree with the player; walk over the canopy to them, leaping across gaps on the way
    private void followTempter() {
        this.monkey.releaseScriptedMovement();
        this.monkey.setPosture(null);
        this.setPhase(Phase.FOLLOW);
    }

    private void tickFollow() {
        Player player = this.tempter;
        if (player == null || this.monkey.isGliding()) {
            return;
        }

        this.monkey.getLookControl().setLookAt(player, 30.0F, this.monkey.getMaxHeadXRot());
        if (this.monkey.distanceToSqr(player) < 2.5) {
            this.monkey.getNavigation().stop();
            this.monkey.setPosture(this.isUpInTree() ? MonkeyState.PERCHING : null);
        } else if (this.phaseTicks % 10 == 0) {
            this.monkey.setPosture(null);
            this.monkey.getNavigation().moveTo(player, 1.1);
        }
    }

    private boolean isStillTempted() {
        return this.isTempting(this.tempter) && this.monkey.distanceToSqr(this.tempter) < TEMPT_RANGE * TEMPT_RANGE * 1.5;
    }

    private void hang(int duration) {
        this.monkey.holdPose(MonkeyState.HANGING);
        this.setPhase(Phase.HANG);
        this.phaseDuration = duration;
    }

    private void finish() {
        this.monkey.releaseScriptedMovement();
        this.phase = Phase.DONE;
    }

    private void tickPerch() {
        if (this.monkey.isGliding()) {
            // Something else is moving the monkey, like a leap
            return;
        }

        if (this.phaseTicks > 5 && !this.isUpInTree()) {
            // Fell or got pushed off
            this.phase = Phase.DONE;
            return;
        }

        // A thief up in the trees hides its loot before doing anything else
        if (!this.monkey.isTame() && this.monkey.hasLoot() && !this.stashFailed && CheekyMonkeysConfig.get().stashNests) {
            this.tickStash();
            return;
        }

        this.lookAround();
        if (!this.fleeing && !this.monkey.isEmoting() && this.monkey.getRandom().nextInt(200) == 0) {
            this.monkey.playEmote(PERCH_EMOTES[this.monkey.getRandom().nextInt(PERCH_EMOTES.length)]);
        }

        if (this.phaseTicks >= this.phaseDuration) {
            this.moveOn();
        } else if (!this.fleeing && this.wantsToComeDown()) {
            this.comeDown();
        }
    }

    /// Heads for the troop's nest nearby, or builds one right here, and puts the loot in it
    private void tickStash() {
        if (this.phaseTicks > STASH_GIVE_UP_TICKS) {
            this.stashFailed = true;
            this.monkey.getNavigation().stop();
            this.monkey.setPosture(MonkeyState.PERCHING);
            return;
        }

        if (this.phaseTicks % 10 != 0) {
            return;
        }

        Level level = this.monkey.level();
        BlockPos here = this.monkey.blockPosition();
        BlockPos stash = BlockPos.betweenClosedStream(here.offset(-STASH_SEARCH_RANGE, -3, -STASH_SEARCH_RANGE), here.offset(STASH_SEARCH_RANGE, 3, STASH_SEARCH_RANGE))
            .filter(pos -> level.getBlockState(pos).is(ModBlocks.MONKEY_STASH.get()))
            .min(Comparator.comparingDouble(pos -> pos.distSqr(here)))
            .map(BlockPos::immutable)
            .orElseGet(() -> this.buildStash(level, here));

        if (stash == null) {
            this.stashFailed = true;
            return;
        }

        if (Vec3.atBottomCenterOf(stash).distanceToSqr(this.monkey.position()) > 4.5) {
            this.monkey.setPosture(null);
            this.monkey.getNavigation().moveTo(stash.getX() + 0.5, stash.getY(), stash.getZ() + 0.5, 1.0);
            return;
        }

        this.monkey.getNavigation().stop();
        this.monkey.setPosture(MonkeyState.PERCHING);
        this.monkey.getLookControl().setLookAt(Vec3.atCenterOf(stash));
        if (level.getBlockEntity(stash) instanceof MonkeyStashBlockEntity nest) {
            ItemStack rest = nest.stash(this.monkey.getMainHandItem());
            this.monkey.setItemSlot(EquipmentSlot.MAINHAND, rest);
            if (rest.isEmpty()) {
                this.monkey.playEmote(MonkeyEmote.HAPPY);
            } else {
                this.stashFailed = true;
            }
        }
    }

    /// Weaves a new nest on the leaves right next to the monkey
    private @Nullable BlockPos buildStash(Level level, BlockPos here) {
        for (BlockPos pos : BlockPos.betweenClosed(here.offset(-1, 0, -1), here.offset(1, 0, 1))) {
            if (!pos.equals(here) && TreeSpots.isPerch(level, pos) && level.getBlockState(pos).canBeReplaced()) {
                BlockPos nest = pos.immutable();
                level.setBlock(nest, ModBlocks.MONKEY_STASH.get().defaultBlockState(), Block.UPDATE_ALL);
                level.playSound(null, nest, SoundEvents.GRASS_PLACE, SoundSource.NEUTRAL, 1.0F, 0.8F);
                return nest;
            }
        }

        return null;
    }

    private void tickHang() {
        this.lookAround();
        if (this.phaseTicks >= this.phaseDuration || this.monkey.isOrderedToSit() || !this.fleeing && this.wantsToComeDown()) {
            if (TreeSpots.heightAboveGround(this.monkey.level(), this.monkey.blockPosition()) <= MAX_HANG_DROP
                && (this.monkey.isOrderedToSit() || this.monkey.getRandom().nextBoolean())) {
                // Let go and drop
                this.finish();
            } else {
                this.comeDown();
            }
        }
    }

    /// Done sitting here: leap over to another tree, or head back down
    private void moveOn() {
        if (this.treeLeapsLeft > 0 && this.monkey.getRandom().nextBoolean()) {
            BlockPos target = TreeSpots.findTreeLeap(this.monkey.level(), this.monkey.blockPosition(), this.monkey.getRandom());
            if (target != null) {
                this.treeLeapsLeft--;
                this.wayBack = null;
                this.setPhase(Phase.TRAVEL);
                this.monkey.leapTo(Vec3.atBottomCenterOf(target), () -> this.perch(80 + this.monkey.getRandom().nextInt(200)));
                return;
            }
        }

        this.comeDown();
    }

    private void comeDown() {
        this.setPhase(Phase.TRAVEL);
        if (this.wayBack != null) {
            this.monkey.setPosture(null);
            this.follow(this.wayBack, 0, this::finish);
            return;
        }

        BlockPos here = this.monkey.blockPosition();
        int height = TreeSpots.heightAboveGround(this.monkey.level(), here);
        if (height <= SAFE_DROP) {
            BlockPos landing = TreeSpots.findJumpDown(this.monkey.level(), here, SAFE_DROP, this.monkey.getRandom());
            if (landing != null) {
                this.monkey.leapTo(Vec3.atBottomCenterOf(landing), this::finish);
                return;
            }
        }

        List<Waypoint> outside = TreeSpots.findClimbDownOutside(this.monkey.level(), here, this.monkey.getVisualScale());
        if (outside != null) {
            this.follow(outside, 0, this::finish);
            return;
        }

        BlockPos ground = TreeSpots.findClimbDownThrough(this.monkey.level(), here);
        if (ground != null) {
            Direction trunk = TreeSpots.findTrunkSide(this.monkey.level(), here);
            float yaw = trunk != null ? trunk.toYRot() : this.monkey.getYRot();
            this.follow(List.of(new Waypoint(Vec3.atBottomCenterOf(ground), MonkeyState.CLIMBING, yaw, TreeSpots.CLIMB_SPEED)), 0, this::finish);
            return;
        }

        // Stuck up here for now; try again later
        this.phase = Phase.DONE;
    }

    private void lookAround() {
        if (this.lookTarget == null || this.phaseTicks % 60 == 0) {
            Player player = this.monkey.level().getNearestPlayer(this.monkey, 12.0);
            if (player != null && this.monkey.getRandom().nextBoolean()) {
                this.lookTarget = player.getEyePosition();
            } else {
                float angle = this.monkey.getRandom().nextFloat() * (float)(Math.PI * 2);
                this.lookTarget = this.monkey.getEyePosition().add(Math.cos(angle) * 4.0, this.monkey.getRandom().nextFloat() * 2.0 - 1.5, Math.sin(angle) * 4.0);
            }
        }

        this.monkey.getLookControl().setLookAt(this.lookTarget);
    }

    private boolean wasHurtRecently() {
        return this.monkey.getLastHurtByMob() != null && this.monkey.tickCount - this.monkey.getLastHurtByMobTimestamp() < 40;
    }

    /// Something down on the ground matters more than sitting in a tree
    private boolean wantsToComeDown() {
        if (this.monkey.isInLove()) {
            return true;
        }

        LivingEntity owner = this.monkey.getOwner();
        if (owner != null && this.monkey.distanceToSqr(owner) > 144.0) {
            return true;
        }

        // A banana down on the ground; one up in the trees is a reason to stay
        Player player = this.monkey.level().getNearestPlayer(this.monkey, 10.0);
        return this.isTempting(player) && !this.isPlayerUpInTree(player);
    }

    private boolean isUpInTree() {
        return this.isUpInTree(this.monkey);
    }

    /// Standing, or hopping about, on top of leaves or logs well above the ground
    private boolean isPlayerUpInTree(Player player) {
        BlockPos pos = player.blockPosition();
        return (TreeSpots.isTree(player.level().getBlockState(pos.below())) || TreeSpots.isTree(player.level().getBlockState(pos.below(2))))
            && TreeSpots.heightAboveGround(player.level(), pos) >= 3;
    }

    /// Standing on top of leaves or logs well above the ground
    private boolean isUpInTree(LivingEntity entity) {
        BlockPos pos = entity.blockPosition();
        return entity.onGround()
            && TreeSpots.isTree(entity.level().getBlockState(pos.below()))
            && TreeSpots.heightAboveGround(entity.level(), pos) >= 3;
    }
}
