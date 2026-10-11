package me.teajay.cheeky.monkeys.entity.ai;

import me.teajay.cheeky.monkeys.entity.MonkeyEntity;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;

/// Follows the owner only in the [pet modes][me.teajay.cheeky.monkeys.entity.PetMode] that should, and not while riding a shoulder
public class MonkeyFollowOwnerGoal extends FollowOwnerGoal {
    private final MonkeyEntity monkey;

    public MonkeyFollowOwnerGoal(MonkeyEntity monkey, double speed, float startDistance, float stopDistance) {
        super(monkey, speed, startDistance, stopDistance);
        this.monkey = monkey;
    }

    @Override
    public boolean canUse() {
        return this.monkey.getPetMode().followsOwner() && !this.monkey.isScriptedMovement() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return this.monkey.getPetMode().followsOwner() && !this.monkey.isScriptedMovement() && super.canContinueToUse();
    }
}
