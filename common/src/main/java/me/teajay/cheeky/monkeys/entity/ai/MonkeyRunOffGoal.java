package me.teajay.cheeky.monkeys.entity.ai;

import java.util.EnumSet;
import me.teajay.cheeky.monkeys.entity.MonkeyEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/// Runs away from whatever the monkey wants to get away from: the player it just robbed or grabbed a banana in front of,
/// or whatever made the troop raise the alarm. Running up a tree is better still, but that's [MonkeyTreeGoal]'s job.
public class MonkeyRunOffGoal extends Goal {
    private static final double SPEED = 1.6;
    private final MonkeyEntity monkey;
    private int repathTicks;

    public MonkeyRunOffGoal(MonkeyEntity monkey) {
        this.monkey = monkey;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return this.monkey.isRunningOff() && !this.monkey.isScriptedMovement() && !this.monkey.isOrderedToSit();
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        this.repathTicks = 0;
    }

    @Override
    public void stop() {
        this.monkey.getNavigation().stop();
    }

    @Override
    public void tick() {
        Entity threat = this.monkey.getRunOffFrom();
        if (threat == null || --this.repathTicks > 0 && !this.monkey.getNavigation().isDone()) {
            return;
        }

        this.repathTicks = 20;
        @Nullable Vec3 away = DefaultRandomPos.getPosAway(this.monkey, 12, 5, threat.position());
        if (away != null) {
            this.monkey.getNavigation().moveTo(away.x, away.y, away.z, SPEED);
        }
    }
}
