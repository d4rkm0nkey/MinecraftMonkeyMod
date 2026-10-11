package me.teajay.cheeky.monkeys.entity.ai;

import java.util.Comparator;
import java.util.EnumSet;
import me.teajay.cheeky.monkeys.CheekyMonkeysConfig;
import me.teajay.cheeky.monkeys.entity.MonkeyEntity;
import me.teajay.cheeky.monkeys.entity.PetMode;
import me.teajay.cheeky.monkeys.registry.ModSounds;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/// A tame monkey darts in on monsters going for its owner and snatches the weapon out of their hands,
/// so skeletons lose their bows and zombies their swords. [MonkeyDeliverGoal] then brings the prize over.
public class MonkeyDisarmGoal extends Goal {
    private static final double SPEED = 1.6;
    private static final double THREAT_RANGE = 12.0;
    private static final int CHECK_INTERVAL = 20;
    private static final int COOLDOWN_TICKS = 300;
    private static final int GIVE_UP_TICKS = 200;
    private final MonkeyEntity monkey;
    private @Nullable Monster target;
    private int ticks;
    private int nextCheck;

    public MonkeyDisarmGoal(MonkeyEntity monkey) {
        this.monkey = monkey;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!CheekyMonkeysConfig.get().petDisarming || this.monkey.tickCount < this.nextCheck || !this.monkey.isTame() || this.monkey.getPetMode() == PetMode.STAY || this.monkey.isBaby()
            || this.monkey.isScriptedMovement() || this.monkey.isInSittingPose() || !this.monkey.getMainHandItem().isEmpty()) {
            return false;
        }

        this.nextCheck = this.monkey.tickCount + CHECK_INTERVAL;
        if (!(this.monkey.getOwner() instanceof LivingEntity owner) || owner.level() != this.monkey.level()) {
            return false;
        }

        this.target = this.monkey.level()
            .getEntitiesOfClass(Monster.class, owner.getBoundingBox().inflate(THREAT_RANGE), monster -> this.isArmedThreat(monster, owner))
            .stream()
            .min(Comparator.comparingDouble(this.monkey::distanceToSqr))
            .orElse(null);
        return this.target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.target != null && this.target.isAlive() && !this.target.getMainHandItem().isEmpty()
            && this.monkey.getMainHandItem().isEmpty() && this.ticks < GIVE_UP_TICKS;
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
        if (distanceSqr < 3.0) {
            ItemStack weapon = this.target.getMainHandItem().copy();
            this.target.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            this.monkey.setItemSlot(EquipmentSlot.MAINHAND, weapon);
            this.monkey.setCarryingForOwner(true);
            this.monkey.playSound(ModSounds.MONKEY_HAPPY.get(), 1.0F, this.monkey.getVoicePitch() * 1.2F);
            this.nextCheck = this.monkey.tickCount + COOLDOWN_TICKS;
        } else if (distanceSqr < 9.0) {
            this.monkey.getMoveControl().setWantedPosition(this.target.getX(), this.target.getY(), this.target.getZ(), SPEED);
        } else if (this.ticks % 10 == 0) {
            this.monkey.getNavigation().moveTo(this.target, SPEED);
        }
    }

    private boolean isArmedThreat(Monster monster, LivingEntity owner) {
        return monster.isAlive() && !monster.getMainHandItem().isEmpty()
            && (monster.getTarget() == owner || monster.distanceToSqr(owner) < 64.0);
    }
}
