package me.teajay.cheeky.monkeys.entity.ai;

import java.util.EnumSet;
import me.teajay.cheeky.monkeys.entity.MonkeyEntity;
import me.teajay.cheeky.monkeys.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/// A tame monkey brings whatever it fetched or snatched to its owner and puts it in their inventory,
/// or at their feet if it doesn't fit
public class MonkeyDeliverGoal extends Goal {
    private static final double SPEED = 1.3;
    private static final int GIVE_UP_TICKS = 400;
    private final MonkeyEntity monkey;
    private @Nullable LivingEntity owner;
    private int ticks;

    public MonkeyDeliverGoal(MonkeyEntity monkey) {
        this.monkey = monkey;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.monkey.isTame() || this.monkey.isScriptedMovement() || this.monkey.isInSittingPose() || this.monkey.isEating()
            || !this.monkey.isCarryingForOwner() && !this.monkey.hasLoot()) {
            return false;
        }

        this.owner = this.monkey.getOwner() instanceof LivingEntity living && living.level() == this.monkey.level()
            && this.monkey.distanceToSqr(living) < 32.0 * 32.0 ? living : null;
        return this.owner != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.owner != null && this.owner.isAlive() && !this.monkey.getMainHandItem().isEmpty() && this.ticks < GIVE_UP_TICKS
            && !this.monkey.isInSittingPose();
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.monkey.getNavigation().moveTo(this.owner, SPEED);
    }

    @Override
    public void stop() {
        this.owner = null;
        this.monkey.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.monkey.getMainHandItem().isEmpty()) {
            return;
        }

        this.ticks++;
        this.monkey.getLookControl().setLookAt(this.owner);
        double distanceSqr = this.monkey.distanceToSqr(this.owner);
        if (distanceSqr < 4.5) {
            this.handOver();
        } else if (distanceSqr < 9.0) {
            this.monkey.getMoveControl().setWantedPosition(this.owner.getX(), this.owner.getY(), this.owner.getZ(), SPEED);
        } else if (this.ticks % 10 == 0) {
            this.monkey.getNavigation().moveTo(this.owner, SPEED);
        }
    }

    private void handOver() {
        ItemStack item = this.monkey.getMainHandItem().copy();
        this.monkey.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        this.monkey.setCarryingForOwner(false);
        if (!(this.owner instanceof Player player) || !player.getInventory().add(item)) {
            this.monkey.spawnAtLocation((ServerLevel)this.monkey.level(), item);
        }

        this.monkey.playSound(ModSounds.MONKEY_HAPPY.get(), 0.8F, this.monkey.getVoicePitch());
        this.monkey.level().playSound(null, this.owner.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.3F, 1.4F);
    }
}
