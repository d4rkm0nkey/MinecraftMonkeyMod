package me.teajay.cheeky.monkeys.entity.ai;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import me.teajay.cheeky.monkeys.entity.MonkeyEmote;
import me.teajay.cheeky.monkeys.entity.MonkeyEntity;
import me.teajay.cheeky.monkeys.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/// Wild monkeys sneak up behind players and steal something from their pockets, then run for it.
/// If the player turns around in time the monkey pretends nothing is going on.
///
/// To get the item back, hit the monkey, offer it a banana, or, as a last resort, kill it: it always drops what it stole.
public class MonkeyPickpocketGoal extends Goal {
    private static final double NOTICE_RANGE = 12.0;
    private static final double SNEAK_SPEED = 0.9;
    private static final double STEAL_RANGE_SQR = 3.24;
    private static final int CHECK_INTERVAL = 40;
    private static final int GIVE_UP_TICKS = 400;
    /// Up to this many items are taken from a stack
    private static final int MAX_STOLEN = 4;
    private final MonkeyEntity monkey;
    private @Nullable Player victim;
    private int ticks;
    private int nextCheck;

    public MonkeyPickpocketGoal(MonkeyEntity monkey) {
        this.monkey = monkey;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.monkey.tickCount < this.nextCheck || this.monkey.isTame() || this.monkey.isBaby() || !this.monkey.canPickpocket()
            || this.monkey.isScriptedMovement() || !this.monkey.getMainHandItem().isEmpty() || this.monkey.isRunningOff()) {
            return false;
        }

        this.nextCheck = this.monkey.tickCount + CHECK_INTERVAL;
        Player player = this.monkey.level().getNearestPlayer(this.monkey, NOTICE_RANGE);
        if (!this.isMark(player) || this.isWatchedBy(player)) {
            return false;
        }

        this.victim = player;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return this.isMark(this.victim) && this.ticks < GIVE_UP_TICKS && this.monkey.getMainHandItem().isEmpty()
            && this.monkey.distanceToSqr(this.victim) < NOTICE_RANGE * NOTICE_RANGE * 2;
    }

    @Override
    public void start() {
        this.ticks = 0;
    }

    @Override
    public void stop() {
        this.victim = null;
        this.monkey.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        Player player = this.victim;
        if (player == null || !this.monkey.getMainHandItem().isEmpty()) {
            return;
        }

        this.ticks++;

        if (this.ticks > 10 && this.isWatchedBy(player) && this.monkey.distanceToSqr(player) < 36.0) {
            // Caught in the act: stop dead and look innocent
            this.monkey.getNavigation().stop();
            this.monkey.playEmote(this.monkey.getRandom().nextBoolean() ? MonkeyEmote.LOOK_AROUND : MonkeyEmote.SCRATCH_HEAD);
            this.monkey.setTheftCooldown(600 + this.monkey.getRandom().nextInt(600));
            this.victim = null;
            return;
        }

        this.monkey.getLookControl().setLookAt(player);
        if (this.monkey.distanceToSqr(player) < STEAL_RANGE_SQR) {
            this.steal(player);
        } else {
            // Creep up on a spot right behind the player
            Vec3 behind = player.position().subtract(player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale(1.0));
            if (this.monkey.distanceToSqr(behind) < 9.0) {
                // Paths end up to a block short for small mobs; sneak the last bit straight in
                this.monkey.getMoveControl().setWantedPosition(behind.x, behind.y, behind.z, SNEAK_SPEED);
            } else if (this.ticks % 5 == 0) {
                this.monkey.getNavigation().moveTo(behind.x, behind.y, behind.z, SNEAK_SPEED);
            }
        }
    }

    private void steal(Player player) {
        List<ItemStack> pockets = new ArrayList<>();
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (!stack.isEmpty()) {
                pockets.add(stack);
            }
        }

        if (pockets.isEmpty()) {
            this.victim = null;
            return;
        }

        ItemStack pocket = pockets.get(this.monkey.getRandom().nextInt(pockets.size()));
        ItemStack stolen = pocket.split(1 + this.monkey.getRandom().nextInt(Math.min(pocket.getCount(), MAX_STOLEN)));
        player.getInventory().setChanged();
        this.monkey.takeLoot(stolen);
        this.monkey.resetTheftCooldown();
        this.monkey.playSound(ModSounds.MONKEY_HAPPY.get(), 1.0F, this.monkey.getVoicePitch() * 1.2F);
        player.sendOverlayMessage(Component.translatable("message.cheeky_monkeys.stolen", stolen.getHoverName()));
        this.monkey.runOffFrom(player, 200 + this.monkey.getRandom().nextInt(100));
    }

    /// A player worth robbing: one who could notice and has something in their pockets
    private boolean isMark(@Nullable Player player) {
        if (player == null || !player.isAlive() || player.isSpectator() || player.isCreative()) {
            return false;
        }

        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (!stack.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    /// The player is looking roughly towards the monkey
    private boolean isWatchedBy(Player player) {
        Vec3 toMonkey = this.monkey.getEyePosition().subtract(player.getEyePosition()).normalize();
        return player.getLookAngle().dot(toMonkey) > 0.5 && player.hasLineOfSight(this.monkey);
    }
}
