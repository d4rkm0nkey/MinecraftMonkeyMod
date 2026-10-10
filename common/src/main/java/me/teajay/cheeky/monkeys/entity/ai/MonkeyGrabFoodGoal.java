package me.teajay.cheeky.monkeys.entity.ai;

import java.util.Comparator;
import java.util.EnumSet;
import me.teajay.cheeky.monkeys.entity.MonkeyEntity;
import me.teajay.cheeky.monkeys.registry.ModSounds;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/// Monkeys can't resist a banana lying around. They race for it, snatch one and run off with it to eat in peace.
/// Tame monkeys just eat it on the spot.
public class MonkeyGrabFoodGoal extends Goal {
    private static final double SEARCH_RANGE = 14.0;
    private static final double SPEED = 1.5;
    private static final int CHECK_INTERVAL = 10;
    private static final int GIVE_UP_TICKS = 300;
    private final MonkeyEntity monkey;
    private @Nullable ItemEntity target;
    private int ticks;
    private int nextCheck;

    public MonkeyGrabFoodGoal(MonkeyEntity monkey) {
        this.monkey = monkey;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.monkey.tickCount < this.nextCheck || this.monkey.isOrderedToSit() || this.monkey.isPassenger()
            || this.monkey.isScriptedMovement() || !this.monkey.getMainHandItem().isEmpty()) {
            return false;
        }

        this.nextCheck = this.monkey.tickCount + CHECK_INTERVAL;
        this.target = this.monkey.level()
            .getEntitiesOfClass(ItemEntity.class, this.monkey.getBoundingBox().inflate(SEARCH_RANGE, 6.0, SEARCH_RANGE), this::isFood)
            .stream()
            .min(Comparator.comparingDouble(this.monkey::distanceToSqr))
            .orElse(null);
        return this.target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.target != null && this.isFood(this.target) && this.ticks < GIVE_UP_TICKS
            && this.monkey.getMainHandItem().isEmpty() && !this.monkey.isOrderedToSit();
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.monkey.playSound(ModSounds.MONKEY_AMBIENT.get(), 1.0F, this.monkey.getVoicePitch() * 1.2F);
        this.monkey.getNavigation().moveTo(this.target, SPEED);
    }

    @Override
    public void stop() {
        this.target = null;
        this.monkey.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        // The goal can tick once more after grabbing before it gets stopped
        if (!this.monkey.getMainHandItem().isEmpty()) {
            return;
        }

        this.ticks++;
        this.monkey.getLookControl().setLookAt(this.target);
        double distanceSqr = this.monkey.distanceToSqr(this.target);
        if (distanceSqr < 2.0) {
            this.grab(this.target);
        } else if (distanceSqr < 9.0) {
            // Paths end up to a block short for small mobs; run the last bit straight at it
            this.monkey.getMoveControl().setWantedPosition(this.target.getX(), this.target.getY(), this.target.getZ(), SPEED);
        } else if (this.ticks % 10 == 0) {
            this.monkey.getNavigation().moveTo(this.target, SPEED);
        }
    }

    private void grab(ItemEntity item) {
        // Work on a copy: clients shrink their own stack when they see the pickup, and in singleplayer that can be this one
        ItemStack rest = item.getItem().copy();
        this.monkey.take(item, 1);
        this.monkey.setItemSlot(EquipmentSlot.MAINHAND, rest.split(1));
        if (rest.isEmpty()) {
            item.discard();
        } else {
            item.setItem(rest);
        }

        this.monkey.playSound(ModSounds.MONKEY_HAPPY.get(), 1.0F, this.monkey.getVoicePitch());
        if (!this.monkey.isTame()) {
            // Mine now! Get away from whoever might want it back
            Entity thrower = item.getOwner();
            Player nearest = this.monkey.level().getNearestPlayer(this.monkey, 16.0);
            this.monkey.runOffFrom(thrower != null ? thrower : nearest, 60 + this.monkey.getRandom().nextInt(60));
        }
    }

    private boolean isFood(ItemEntity item) {
        return item.isAlive() && !item.getItem().isEmpty() && this.monkey.isFood(item.getItem());
    }
}
