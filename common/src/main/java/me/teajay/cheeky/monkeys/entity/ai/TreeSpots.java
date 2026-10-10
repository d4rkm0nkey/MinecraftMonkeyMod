package me.teajay.cheeky.monkeys.entity.ai;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.teajay.cheeky.monkeys.entity.MonkeyState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Util;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/// Finds the ways a monkey can get up, around and down trees, and where it can leap to.
public final class TreeSpots {
    /// Highest a monkey will climb up a single trunk
    public static final int MAX_CLIMB = 24;
    public static final double CLIMB_SPEED = 0.15;
    public static final double SWING_SPEED = 0.1;
    private static final double STEP_SPEED = 0.1;
    /// A perch needs at least this much space below it to be worth climbing to
    private static final int MIN_PERCH_HEIGHT = 3;
    /// Lowest leaves a monkey will hang from
    private static final int MIN_HANG_HEIGHT = 2;
    /// How far a monkey swings along under the canopy looking for its edge
    private static final int MAX_SWING = 8;
    /// How far a monkey climbs up the outside of the leaves to get on top
    private static final int MAX_EDGE_CLIMB = 5;
    private static final int MAX_DESCENT = 40;
    /// At full size the raised hand of a hanging monkey is this far above its feet, and its outstretched arms reach this far forward
    private static final double HANG_REACH = 2.3;
    private static final double ARM_REACH = 0.55;

    private TreeSpots() {
    }

    /// One leg of a route: glide to `pos` in `pose`, facing `yaw` (or the way it's going, if NaN), at `speed` blocks per tick
    public record Waypoint(Vec3 pos, MonkeyState pose, float yaw, double speed) {
    }

    /// A way up a tree from `start` at the trunk's foot, ending on top of the canopy if `perches`, or hanging below it otherwise
    public record ClimbRoute(BlockPos start, List<Waypoint> path, boolean perches) {
        /// The same way back down: each leg reversed, keeping the pose and speed it was climbed with
        public List<Waypoint> reversed() {
            List<Waypoint> back = new ArrayList<>();
            for (int i = this.path.size() - 1; i > 0; i--) {
                Waypoint leg = this.path.get(i);
                back.add(new Waypoint(this.path.get(i - 1).pos(), leg.pose(), leg.yaw(), leg.speed()));
            }

            return back;
        }
    }

    public static boolean isTree(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES);
    }

    /// Air or something you can walk through, like vines or flowers
    public static boolean isOpen(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getFluidState().isEmpty() && state.getCollisionShape(level, pos).isEmpty();
    }

    /// Open space or leaves; a monkey in a hurry squeezes through both
    public static boolean isPassable(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getFluidState().isEmpty() && (state.is(BlockTags.LEAVES) || state.getCollisionShape(level, pos).isEmpty());
    }

    /// Somewhere to sit up in a tree: open space on top of leaves or a log
    public static boolean isPerch(BlockGetter level, BlockPos pos) {
        return isOpen(level, pos) && isTree(level.getBlockState(pos.below()));
    }

    /// Solid ground that isn't part of a tree
    public static boolean isGround(BlockGetter level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        return isOpen(level, pos) && !isTree(belowState) && belowState.isFaceSturdy(level, below, Direction.UP);
    }

    /// Open space right under leaves or a log, with room for a dangling monkey
    private static boolean isHangSpot(BlockGetter level, BlockPos pos) {
        return isOpen(level, pos) && isOpen(level, pos.below()) && isTree(level.getBlockState(pos.above()));
    }

    /// How far it is from `pos` down through air and trees to the first solid block that isn't part of a tree,
    /// or `MAX_DESCENT` if there's nothing
    public static int heightAboveGround(BlockGetter level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int depth = 0; depth < MAX_DESCENT; depth++) {
            cursor.move(Direction.DOWN);
            if (!isPassable(level, cursor) && !isTree(level.getBlockState(cursor))) {
                return depth;
            }
        }

        return MAX_DESCENT;
    }

    /// Looks for trunks within `radius` that the monkey at `origin` could walk to and climb.
    /// `scale` is how big the monkey is drawn, which decides how far below the leaves it hangs.
    /// With a `target`, picks the way up that ends closest to it.
    public static @Nullable ClimbRoute findClimb(
        BlockGetter level, BlockPos origin, int radius, RandomSource random, float scale, @Nullable Vec3 target
    ) {
        List<ClimbRoute> routes = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -3; dy <= 3; dy++) {
                    cursor.setWithOffset(origin, dx, dy, dz);
                    // Only look at the bottom log of each trunk
                    if (!level.getBlockState(cursor).is(BlockTags.LOGS) || level.getBlockState(cursor.below()).is(BlockTags.LOGS)) {
                        continue;
                    }

                    for (Direction side : Direction.Plane.HORIZONTAL) {
                        ClimbRoute route = scanTrunk(level, cursor.relative(side), side.getOpposite(), random, scale);
                        if (route != null) {
                            routes.add(route);
                        }
                    }
                }
            }
        }

        if (routes.isEmpty()) {
            return null;
        }

        if (target != null) {
            return routes.stream()
                .filter(ClimbRoute::perches)
                .min(Comparator.comparingDouble(route -> route.path().getLast().pos().distanceToSqr(target)))
                .orElse(null);
        }

        // Sitting on top of the tree is the goal; hanging around underneath is the fallback
        List<ClimbRoute> perching = routes.stream().filter(ClimbRoute::perches).toList();
        if (!perching.isEmpty() && random.nextInt(4) != 0) {
            routes = new ArrayList<>(perching);
        }

        // Prefer one of the nearest trunks, but don't always pick the same one
        routes.sort(Comparator.comparingDouble(route -> route.start().distSqr(origin)));
        return routes.get(random.nextInt(Math.min(routes.size(), 4)));
    }

    /// Climbs the column at `start` next to the trunk in direction `trunkSide` up to the underside of the canopy,
    /// then swings along under it to its edge and climbs up the outside of the leaves to sit on top
    private static @Nullable ClimbRoute scanTrunk(BlockGetter level, BlockPos start, Direction trunkSide, RandomSource random, float scale) {
        if (!isGround(level, start)) {
            return null;
        }

        int top = -1;
        for (int height = 1; height <= MAX_CLIMB; height++) {
            BlockPos cell = start.above(height);
            if (height <= 2 && !level.getBlockState(cell.relative(trunkSide)).is(BlockTags.LOGS)) {
                // Not a real trunk, just a stump or a fallen log
                return null;
            }

            if (isOpen(level, cell)) {
                continue;
            }

            if (isTree(level.getBlockState(cell)) && height - 1 >= MIN_HANG_HEIGHT) {
                top = height - 1;
            }

            break;
        }

        if (top < 0) {
            return null;
        }

        BlockPos underCanopy = start.above(top);
        double hangDrop = 1.0 - HANG_REACH * scale;
        double hug = 0.5 - ARM_REACH * scale;
        float trunkYaw = trunkSide.toYRot();

        List<Waypoint> path = new ArrayList<>();
        path.add(new Waypoint(hugging(start, trunkSide, hug, 0.0), MonkeyState.CLIMBING, trunkYaw, STEP_SPEED));
        path.add(new Waypoint(hugging(underCanopy, trunkSide, hug, hangDrop), MonkeyState.CLIMBING, trunkYaw, CLIMB_SPEED));

        // Swing along under the canopy, breadth first, until there's a way up its side
        Map<BlockPos, BlockPos> cameFrom = new HashMap<>();
        cameFrom.put(underCanopy, underCanopy);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(underCanopy);
        List<Direction> directions = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());

        while (!queue.isEmpty()) {
            BlockPos cell = queue.poll();
            Util.shuffle(directions, random);

            for (Direction direction : directions) {
                BlockPos next = cell.relative(direction);
                if (cameFrom.containsKey(next)) {
                    continue;
                }

                if (isHangSpot(level, next)) {
                    if (next.distManhattan(underCanopy) <= MAX_SWING) {
                        cameFrom.put(next, cell);
                        queue.add(next);
                    }

                    continue;
                }

                BlockPos perch = findEdgeClimb(level, cell, next);
                if (perch != null) {
                    addSwing(path, cameFrom, cell, hangDrop);
                    float leavesYaw = direction.getOpposite().toYRot();
                    // Out from under the leaves, up their side and over the top
                    path.add(new Waypoint(hugging(next, direction.getOpposite(), hug, 0.2), MonkeyState.CLIMBING, leavesYaw, STEP_SPEED));
                    path.add(new Waypoint(hugging(next, direction.getOpposite(), hug, perch.getY() - next.getY()), MonkeyState.CLIMBING, leavesYaw, CLIMB_SPEED));
                    path.add(new Waypoint(Vec3.atBottomCenterOf(perch), MonkeyState.CLIMBING, leavesYaw, STEP_SPEED));
                    return new ClimbRoute(start, path, true);
                }
            }
        }

        // No way on top; just hang out somewhere under the canopy
        List<BlockPos> hangSpots = new ArrayList<>(cameFrom.keySet());
        addSwing(path, cameFrom, hangSpots.get(random.nextInt(hangSpots.size())), hangDrop);
        return new ClimbRoute(start, path, false);
    }

    /// From hanging under `cell`, checks whether the monkey can swing out to the open column at `outside`
    /// and climb up it along the side of the leaves until it can step onto them
    private static @Nullable BlockPos findEdgeClimb(BlockGetter level, BlockPos cell, BlockPos outside) {
        if (!isOpen(level, outside)) {
            return null;
        }

        for (int height = 1; height <= MAX_EDGE_CLIMB; height++) {
            if (!isOpen(level, outside.above(height))) {
                return null;
            }

            BlockPos perch = cell.above(height);
            if (height >= 2 && isPerch(level, perch) && heightAboveGround(level, perch) >= MIN_PERCH_HEIGHT) {
                return perch;
            }
        }

        return null;
    }

    /// Adds the swing from just under the canopy to `end`, one cell at a time
    private static void addSwing(List<Waypoint> path, Map<BlockPos, BlockPos> cameFrom, BlockPos end, double hangDrop) {
        List<BlockPos> cells = new ArrayList<>();
        for (BlockPos cell = end; ; cell = cameFrom.get(cell)) {
            cells.add(cell);
            if (cameFrom.get(cell).equals(cell)) {
                break;
            }
        }

        Collections.reverse(cells);
        for (BlockPos cell : cells) {
            path.add(new Waypoint(Vec3.atBottomCenterOf(cell).add(0.0, hangDrop, 0.0), MonkeyState.SWINGING, Float.NaN, SWING_SPEED));
        }
    }

    /// The point in `cell`, `lift` above its floor, where a monkey holding on to the block on `side` is
    private static Vec3 hugging(BlockPos cell, Direction side, double hug, double lift) {
        return Vec3.atBottomCenterOf(cell).add(side.getStepX() * hug, lift, side.getStepZ() * hug);
    }

    /// The way down from a perch at `from` along the outside of the tree: over the edge and straight down an open column.
    /// Returns null when every side is blocked.
    public static @Nullable List<Waypoint> findClimbDownOutside(BlockGetter level, BlockPos from, float scale) {
        double hug = 0.5 - ARM_REACH * scale;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos outside = from.relative(direction);
            if (!isOpen(level, outside) || isTree(level.getBlockState(outside.below()))) {
                continue;
            }

            BlockPos.MutableBlockPos cursor = outside.mutable();
            for (int depth = 0; depth < MAX_DESCENT; depth++) {
                if (isGround(level, cursor)) {
                    if (depth < 2) {
                        break;
                    }

                    float yaw = direction.getOpposite().toYRot();
                    return List.of(
                        new Waypoint(hugging(outside, direction.getOpposite(), hug, 0.0), MonkeyState.CLIMBING, yaw, STEP_SPEED),
                        new Waypoint(hugging(cursor, direction.getOpposite(), hug, 0.0), MonkeyState.CLIMBING, yaw, CLIMB_SPEED),
                        new Waypoint(Vec3.atBottomCenterOf(cursor), MonkeyState.CLIMBING, yaw, STEP_SPEED)
                    );
                }

                if (!isOpen(level, cursor.move(Direction.DOWN))) {
                    break;
                }
            }
        }

        return null;
    }

    /// Finds the ground straight below `from` if the monkey can squeeze down to it through nothing but air and leaves
    public static @Nullable BlockPos findClimbDownThrough(BlockGetter level, BlockPos from) {
        BlockPos.MutableBlockPos cursor = from.mutable();
        for (int depth = 0; depth < MAX_DESCENT; depth++) {
            cursor.move(Direction.DOWN);
            if (isPassable(level, cursor)) {
                continue;
            }

            BlockPos landing = cursor.above();
            return isGround(level, landing) ? landing.immutable() : null;
        }

        return null;
    }

    /// Finds a perch in a neighbouring tree that the monkey at `from` can leap to without hitting logs on the way
    public static @Nullable BlockPos findTreeLeap(BlockGetter level, BlockPos from, RandomSource random) {
        List<BlockPos> targets = new ArrayList<>();
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                int distSqr = dx * dx + dz * dz;
                if (distSqr < 9 || distSqr > 49) {
                    continue;
                }

                for (int dy = -3; dy <= 2; dy++) {
                    BlockPos target = from.offset(dx, dy, dz);
                    if (isPerch(level, target) && heightAboveGround(level, target) >= MIN_PERCH_HEIGHT && canLeap(level, from, target)) {
                        targets.add(target);
                    }
                }
            }
        }

        return targets.isEmpty() ? null : targets.get(random.nextInt(targets.size()));
    }

    /// Finds open ground the monkey at `from` can jump down to, at most `maxDrop` blocks lower
    public static @Nullable BlockPos findJumpDown(BlockGetter level, BlockPos from, int maxDrop, RandomSource random) {
        List<BlockPos> targets = new ArrayList<>();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                for (int dy = -maxDrop; dy <= 0; dy++) {
                    BlockPos target = from.offset(dx, dy, dz);
                    if (isGround(level, target) && canLeap(level, from, target)) {
                        targets.add(target);
                    }
                }
            }
        }

        return targets.isEmpty() ? null : targets.get(random.nextInt(targets.size()));
    }

    /// Finds ground, or a spot on top of a tree, to leap to from `from` that gets the monkey closer to `target`, between 2 and
    /// 5 blocks ahead. Used to jump over gaps and ditches and between treetops where the pathfinder can't get across.
    public static @Nullable BlockPos findLeapToward(BlockGetter level, Vec3 from, Vec3 target) {
        Vec3 toTarget = target.subtract(from).multiply(1.0, 0.0, 1.0);
        double distance = toTarget.length();
        if (distance < 2.0) {
            return null;
        }

        Vec3 ahead = toTarget.scale(1.0 / distance);
        Vec3 side = new Vec3(-ahead.z, 0.0, ahead.x);
        BlockPos fromPos = BlockPos.containing(from);
        // Furthest first, so the monkey covers as much of the gap as it can
        for (double reach = Math.min(5.0, distance + 0.5); reach >= 2.0; reach -= 0.5) {
            for (double offset : new double[]{0.0, -1.0, 1.0}) {
                Vec3 point = from.add(ahead.scale(reach)).add(side.scale(offset));
                for (int dy = 1; dy >= -3; dy--) {
                    BlockPos landing = BlockPos.containing(point.x, from.y + dy, point.z);
                    if ((isGround(level, landing) || isPerch(level, landing))
                        && Vec3.atBottomCenterOf(landing).distanceToSqr(target) < from.distanceToSqr(target) - 1.0
                        && canLeap(level, fromPos, landing)) {
                        return landing;
                    }
                }
            }
        }

        return null;
    }

    /// Finds ground to leap to roughly in direction `yaw`, for a playful hop
    public static @Nullable BlockPos findHop(BlockGetter level, Vec3 from, float yaw, RandomSource random) {
        float angle = (yaw + (random.nextFloat() - 0.5F) * 60.0F) * ((float)Math.PI / 180.0F);
        Vec3 ahead = new Vec3(-Math.sin(angle), 0.0, Math.cos(angle));
        double reach = 2.0 + random.nextDouble() * 2.5;
        Vec3 point = from.add(ahead.scale(reach));
        BlockPos fromPos = BlockPos.containing(from);
        for (int dy = 1; dy >= -2; dy--) {
            BlockPos landing = BlockPos.containing(point.x, from.y + dy, point.z);
            if (isGround(level, landing) && canLeap(level, fromPos, landing)) {
                return landing;
            }
        }

        return null;
    }

    /// Finds a log next to the column at `pos` to face while climbing, if there is one
    public static @Nullable Direction findTrunkSide(BlockGetter level, BlockPos pos) {
        for (int depth = 0; depth < 3; depth++) {
            for (Direction side : Direction.Plane.HORIZONTAL) {
                if (level.getBlockState(pos.below(depth).relative(side)).is(BlockTags.LOGS)) {
                    return side;
                }
            }
        }

        return null;
    }

    /// How far a leap between two points bulges above the straight line between them
    public static double leapArc(Vec3 from, Vec3 to) {
        double distance = from.subtract(to).horizontalDistance();
        return 0.6 + distance * 0.15 + Math.max(0.0, to.y - from.y) * 0.5;
    }

    /// Position along a glide, `progress` going from 0 to 1, on a parabola through both ends that bulges `arc` blocks above the middle
    public static Vec3 arcPosition(Vec3 from, Vec3 to, double arc, double progress) {
        return from.lerp(to, progress).add(0.0, 4.0 * arc * progress * (1.0 - progress), 0.0);
    }

    private static boolean canLeap(BlockGetter level, BlockPos from, BlockPos to) {
        Vec3 start = Vec3.atBottomCenterOf(from);
        Vec3 end = Vec3.atBottomCenterOf(to);
        double arc = leapArc(start, end);
        int samples = 10;
        for (int i = 1; i < samples; i++) {
            Vec3 point = arcPosition(start, end, arc, i / (double)samples);
            if (!isPassable(level, BlockPos.containing(point)) || !isPassable(level, BlockPos.containing(point.add(0.0, 0.6, 0.0)))) {
                return false;
            }
        }

        return true;
    }
}
