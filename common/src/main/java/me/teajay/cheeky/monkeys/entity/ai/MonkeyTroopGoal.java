package me.teajay.cheeky.monkeys.entity.ai;

import java.util.EnumSet;
import java.util.List;
import me.teajay.cheeky.monkeys.entity.MonkeyEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/// Wild monkeys live in troops: one that wanders too far from the others heads back to them
public class MonkeyTroopGoal extends Goal {
    private static final double TROOP_RANGE = 24.0;
    private static final double MAX_SPREAD = 8.0;
    private static final double CLOSE_ENOUGH = 4.0;
    private static final int CHECK_INTERVAL = 40;
    private final MonkeyEntity monkey;
    private Vec3 troopCentre = Vec3.ZERO;
    private int nextCheck;

    public MonkeyTroopGoal(MonkeyEntity monkey) {
        this.monkey = monkey;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.monkey.tickCount < this.nextCheck || this.monkey.isTame() || this.monkey.isScriptedMovement() || !this.monkey.onGround()) {
            return false;
        }

        this.nextCheck = this.monkey.tickCount + CHECK_INTERVAL + this.monkey.getRandom().nextInt(CHECK_INTERVAL);
        List<MonkeyEntity> troop = this.monkey.level().getEntitiesOfClass(
            MonkeyEntity.class, this.monkey.getBoundingBox().inflate(TROOP_RANGE), other -> other != this.monkey && !other.isTame()
        );
        if (troop.isEmpty()) {
            return false;
        }

        Vec3 sum = Vec3.ZERO;
        for (MonkeyEntity other : troop) {
            sum = sum.add(other.position());
        }

        this.troopCentre = sum.scale(1.0 / troop.size());
        return this.monkey.position().distanceTo(this.troopCentre) > MAX_SPREAD;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.monkey.getNavigation().isDone() && this.monkey.position().distanceTo(this.troopCentre) > CLOSE_ENOUGH;
    }

    @Override
    public void start() {
        this.monkey.getNavigation().moveTo(this.troopCentre.x, this.troopCentre.y, this.troopCentre.z, 1.1);
    }

    @Override
    public void stop() {
        this.monkey.getNavigation().stop();
    }
}
