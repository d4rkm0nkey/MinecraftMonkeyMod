package me.teajay.cheeky.monkeys.entity.ai;

import java.util.Comparator;
import java.util.EnumSet;
import me.teajay.cheeky.monkeys.entity.MonkeyEntity;
import me.teajay.cheeky.monkeys.entity.PetMode;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import org.jspecify.annotations.Nullable;

/// In gather mode, a tame monkey picks up items lying around its owner (drops from mining, mobs, explosions)
/// for [MonkeyDeliverGoal] to bring over. It leaves alone whatever the owner has only just dropped.
public class MonkeyGatherGoal extends Goal {
    private static final double GATHER_RANGE = 10.0;
    private static final double SPEED = 1.3;
    private static final int CHECK_INTERVAL = 20;
    private static final int GIVE_UP_TICKS = 200;
    /// Items the owner dropped themselves are left alone for this long
    private static final int OWNER_DROP_GRACE_TICKS = 200;
    private final MonkeyEntity monkey;
    private @Nullable ItemEntity target;
    private int ticks;
    private int nextCheck;

    public MonkeyGatherGoal(MonkeyEntity monkey) {
        this.monkey = monkey;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.monkey.tickCount < this.nextCheck || !this.monkey.isTame() || this.monkey.getPetMode() != PetMode.GATHER
            || this.monkey.isScriptedMovement() || this.monkey.isInSittingPose() || !this.monkey.getMainHandItem().isEmpty()) {
            return false;
        }

        this.nextCheck = this.monkey.tickCount + CHECK_INTERVAL;
        if (!(this.monkey.getOwner() instanceof LivingEntity owner) || owner.level() != this.monkey.level() || this.monkey.distanceToSqr(owner) > 400.0) {
            return false;
        }

        this.target = this.monkey.level()
            .getEntitiesOfClass(ItemEntity.class, owner.getBoundingBox().inflate(GATHER_RANGE, 4.0, GATHER_RANGE), item -> this.isWanted(item, owner))
            .stream()
            .min(Comparator.comparingDouble(this.monkey::distanceToSqr))
            .orElse(null);
        return this.target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.target != null && this.target.isAlive() && !this.target.getItem().isEmpty() && this.ticks < GIVE_UP_TICKS
            && this.monkey.getMainHandItem().isEmpty() && this.monkey.getPetMode() == PetMode.GATHER;
    }

    @Override
    public void start() {
        this.ticks = 0;
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
        if (!this.monkey.getMainHandItem().isEmpty()) {
            return;
        }

        this.ticks++;
        this.monkey.getLookControl().setLookAt(this.target);
        double distanceSqr = this.monkey.distanceToSqr(this.target);
        if (distanceSqr < 2.0) {
            this.monkey.take(this.target, this.target.getItem().getCount());
            this.monkey.setItemSlot(EquipmentSlot.MAINHAND, this.target.getItem().copy());
            this.monkey.setCarryingForOwner(true);
            this.target.discard();
        } else if (distanceSqr < 9.0) {
            // Paths end up to a block short for small mobs; go the last bit straight
            this.monkey.getMoveControl().setWantedPosition(this.target.getX(), this.target.getY(), this.target.getZ(), SPEED);
        } else if (this.ticks % 10 == 0) {
            this.monkey.getNavigation().moveTo(this.target, SPEED);
        }
    }

    private boolean isWanted(ItemEntity item, LivingEntity owner) {
        return item.isAlive() && !item.getItem().isEmpty() && !(item.getOwner() == owner && item.getAge() < OWNER_DROP_GRACE_TICKS);
    }
}
