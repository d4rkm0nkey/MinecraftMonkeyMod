package me.teajay.cheeky.monkeys.entity.ai;

import java.util.EnumSet;
import me.teajay.cheeky.monkeys.entity.MonkeyEntity;
import me.teajay.cheeky.monkeys.entity.MonkeyState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/// Now and then an idle monkey takes a big playful leap forward
public class MonkeyHopGoal extends Goal {
    private static final int AVERAGE_INTERVAL_TICKS = 500;
    private final MonkeyEntity monkey;
    private BlockPos landing = BlockPos.ZERO;

    public MonkeyHopGoal(MonkeyEntity monkey) {
        this.monkey = monkey;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (!this.monkey.onGround() || this.monkey.isScriptedMovement() || this.monkey.isInSittingPose() || this.monkey.isEmoting()
            || this.monkey.getState() != MonkeyState.IDLE || this.monkey.getRandom().nextInt(reducedTickDelay(AVERAGE_INTERVAL_TICKS)) != 0) {
            return false;
        }

        BlockPos found = TreeSpots.findHop(this.monkey.level(), this.monkey.position(), this.monkey.getYRot(), this.monkey.getRandom());
        if (found == null) {
            return false;
        }

        this.landing = found;
        return true;
    }

    @Override
    public void start() {
        this.monkey.leapTo(Vec3.atBottomCenterOf(this.landing), null);
    }

    @Override
    public boolean canContinueToUse() {
        return this.monkey.isGliding();
    }

    @Override
    public boolean isInterruptable() {
        return false;
    }
}
