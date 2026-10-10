package me.teajay.cheeky.monkeys.entity.ai;

import java.util.EnumSet;
import me.teajay.cheeky.monkeys.entity.MonkeyEmote;
import me.teajay.cheeky.monkeys.entity.MonkeyEntity;
import me.teajay.cheeky.monkeys.entity.MonkeyState;
import net.minecraft.world.entity.ai.goal.Goal;

/// Every now and then an idle monkey stops to pound its chest, scratch its head or look around
public class MonkeyIdleEmoteGoal extends Goal {
    private static final int AVERAGE_INTERVAL_TICKS = 400;
    private final MonkeyEntity monkey;

    public MonkeyIdleEmoteGoal(MonkeyEntity monkey) {
        this.monkey = monkey;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return this.monkey.onGround()
            && !this.monkey.isEmoting()
            && this.monkey.getState() == MonkeyState.IDLE
            && this.monkey.getRandom().nextInt(reducedTickDelay(AVERAGE_INTERVAL_TICKS)) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.monkey.isEmoting();
    }

    @Override
    public void start() {
        this.monkey.getNavigation().stop();
        MonkeyEmote[] emotes = MonkeyEmote.IDLE_EMOTES;
        this.monkey.playEmote(emotes[this.monkey.getRandom().nextInt(emotes.length)]);
    }
}
