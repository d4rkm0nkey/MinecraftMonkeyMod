package me.teajay.cheeky.monkeys.entity.ai;

import java.util.EnumSet;
import java.util.List;
import me.teajay.cheeky.monkeys.entity.MonkeyEmote;
import me.teajay.cheeky.monkeys.entity.MonkeyEntity;
import me.teajay.cheeky.monkeys.entity.MonkeyState;
import me.teajay.cheeky.monkeys.entity.ai.TreeSpots.ClimbRoute;
import me.teajay.cheeky.monkeys.entity.ai.TreeSpots.Waypoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/// Monkeys spend their spare time up in the trees. They walk to a trunk and climb it, swing hand over hand along
/// under the canopy to its edge, climb up the outside of the leaves and sit on top. From there they leap across to
/// neighbouring trees, and eventually climb back down the way they came or jump down.
///
/// With `fleeing` set, the goal instead sends a monkey that just got hurt up the nearest tree, out of reach.
public class MonkeyTreeGoal extends Goal {
    private enum Phase { APPROACH, TRAVEL, PERCH, HANG, DONE }

    private static final int AVERAGE_START_INTERVAL = 600;
    private static final int APPROACH_TIMEOUT = 200;
    private static final int MAX_TREE_LEAPS = 3;
    /// Highest drop a monkey happily jumps down
    private static final int SAFE_DROP = 5;
    /// Highest a hanging monkey lets go from
    private static final int MAX_HANG_DROP = 6;
    private static final double FLEE_SPEED_FACTOR = 1.8;
    private static final MonkeyEmote[] PERCH_EMOTES = {MonkeyEmote.CHEST_POUND, MonkeyEmote.LOOK_AROUND, MonkeyEmote.SCRATCH_HEAD};

    private final MonkeyEntity monkey;
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

    public MonkeyTreeGoal(MonkeyEntity monkey, boolean fleeing) {
        this.monkey = monkey;
        this.fleeing = fleeing;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.monkey.isOrderedToSit() || this.monkey.isPassenger() || this.monkey.isLeashed() || this.monkey.isInWater()
            || this.monkey.isScriptedMovement() || !this.monkey.onGround()) {
            return false;
        }

        if (this.fleeing) {
            if (!this.wasHurtRecently()) {
                return false;
            }
        } else if (!this.monkey.isTreeClimbRequested()) {
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
            this.monkey.level(), this.monkey.blockPosition(), this.fleeing ? 8 : 12, this.monkey.getRandom(), this.monkey.getVisualScale()
        );
        return this.route != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.phase == Phase.DONE || !this.monkey.isAlive() || this.monkey.isPassenger() || this.monkey.isLeashed()) {
            return false;
        }

        // Sit right where you are, unless you're halfway up a trunk or mid-leap
        return !this.monkey.isOrderedToSit() || this.phase != Phase.APPROACH && this.phase != Phase.PERCH;
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
            this.perch(20 + this.monkey.getRandom().nextInt(60));
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
        this.cooldownUntil = this.monkey.tickCount + (this.fleeing ? 100 : 600 + this.monkey.getRandom().nextInt(1200));
    }

    @Override
    public void tick() {
        this.phaseTicks++;
        switch (this.phase) {
            case APPROACH -> this.tickApproach();
            case PERCH -> this.tickPerch();
            case HANG -> this.tickHang();
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

        if (level && distance < 0.4) {
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
        this.follow(climbRoute.path(), 0, climbRoute.perches() ? () -> this.perch(stay) : () -> this.hang(stay / 2));
    }

    /// Glides along the waypoints from `index` on, then runs `then`
    private void follow(List<Waypoint> path, int index, Runnable then) {
        if (index >= path.size()) {
            then.run();
            return;
        }

        Waypoint leg = path.get(index);
        double speed = this.fleeing ? leg.speed() * FLEE_SPEED_FACTOR : leg.speed();
        this.monkey.glideTo(leg.pos(), speed, leg.pose(), leg.yaw(), 0.0, () -> this.follow(path, index + 1, then));
    }

    private void perch(int duration) {
        this.monkey.releaseScriptedMovement();
        this.monkey.setPosture(MonkeyState.PERCHING);
        this.setPhase(Phase.PERCH);
        this.phaseDuration = duration;
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

        Player player = this.monkey.level().getNearestPlayer(this.monkey, 10.0);
        return player != null && (this.monkey.isFood(player.getMainHandItem()) || this.monkey.isFood(player.getOffhandItem()));
    }

    private boolean isUpInTree() {
        BlockPos pos = this.monkey.blockPosition();
        return this.monkey.onGround()
            && TreeSpots.isTree(this.monkey.level().getBlockState(pos.below()))
            && TreeSpots.heightAboveGround(this.monkey.level(), pos) >= 3;
    }
}
