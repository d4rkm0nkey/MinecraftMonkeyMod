package me.teajay.cheeky.monkeys.fabric.gametest;

import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import me.teajay.cheeky.monkeys.entity.MonkeyEmote;
import me.teajay.cheeky.monkeys.entity.MonkeyEntity;
import me.teajay.cheeky.monkeys.entity.MonkeyState;
import me.teajay.cheeky.monkeys.registry.ModEntities;
import me.teajay.cheeky.monkeys.registry.ModItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/// Lines monkeys up in front of the camera in their different states and emotes and takes screenshots,
/// so animations and rendering can be checked by eye. Run with `./gradlew :fabric:runClientGameTest`.
public class MonkeyShowcaseClientGameTest implements FabricClientGameTest {
    private static final int[] SLOTS = {-6, -3, 0, 3, 6};

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            TestServerContext server = singleplayer.getServer();
            singleplayer.getConnection().waitForChunksRender();

            server.runCommand("gamerule advance_time false");
            server.runCommand("time set noon");
            int ground = server.computeOnServer(s -> s.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0));
            server.runCommand("tp @p 0 " + ground + " 7 180 18");
            context.waitTicks(20);

            // Idle, sitting, baby, dancing, eating
            spawnLineup(server, (monkeys, player) -> {
                sit(monkeys.get(1), player);
                monkeys.get(2).setAge(-24000);
                BlockPos jukebox = monkeys.get(3).blockPosition().north(2);
                monkeys.get(3).setRecordPlayingNearby(jukebox, true);
                monkeys.get(4).setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.BANANA.get()));
                sit(monkeys.get(4), player);
            });
            server.runCommand("setblock 3 " + ground + " -2 minecraft:jukebox");
            context.waitTicks(30);
            server.runOnServer(s -> monkeys(s).get(4).playEmote(MonkeyEmote.EAT));
            context.waitTicks(12);
            context.takeScreenshot("monkeys_lineup");

            // All idle emotes plus the happy cheer, mid-animation
            spawnLineup(server, (monkeys, player) -> {
                MonkeyEmote[] emotes = {MonkeyEmote.CHEST_POUND, MonkeyEmote.SCRATCH_HEAD, MonkeyEmote.CHEST_RUB, MonkeyEmote.LOOK_AROUND, MonkeyEmote.HAPPY};
                for (int i = 0; i < emotes.length; i++) {
                    monkeys.get(i).playEmote(emotes[i]);
                }
            });
            context.waitTicks(16);
            context.takeScreenshot("monkeys_emotes_early");
            context.waitTicks(10);
            context.takeScreenshot("monkeys_emotes_late");

            // Climbing a vine wall and sleeping
            server.runCommand("fill -7 " + ground + " -1 -5 " + (ground + 5) + " -1 minecraft:oak_planks");
            server.runCommand("fill -7 " + ground + " 0 -5 " + (ground + 5) + " 0 minecraft:vine[north=true]");
            spawnLineup(server, (monkeys, player) -> {
                for (int i = 1; i < monkeys.size(); i++) {
                    sit(monkeys.get(i), player);
                }
            });
            server.runCommand("effect give @p minecraft:night_vision infinite 0 true");
            server.runCommand("time set midnight");
            context.waitTicks(40);
            server.runOnServer(s -> {
                List<MonkeyEntity> monkeys = monkeys(s);
                if (monkeys.get(0).getState() != MonkeyState.CLIMBING || monkeys.get(1).getState() != MonkeyState.SLEEPING) {
                    throw new AssertionError("Expected climbing and sleeping monkeys, got " + monkeys.stream().map(MonkeyEntity::getState).toList());
                }
            });
            context.takeScreenshot("monkeys_climb_sleep");

            // Walking, running and leaping need real AI and physics
            server.runCommand("time set noon");
            server.runCommand("fill -7 " + ground + " -1 -5 " + (ground + 5) + " 0 minecraft:air");
            spawnLineup(server, (monkeys, player) -> {
                monkeys.forEach(monkey -> monkey.setNoAi(false));
                monkeys.get(0).getNavigation().moveTo(-6.5, ground, -12.5, 1.0);
                monkeys.get(1).getNavigation().moveTo(-3.5, ground, -12.5, 2.2);
                monkeys.get(2).snapTo(0.5, ground + 6, 0.5, 0, 0);
                monkeys.get(2).setDeltaMovement(0, 0.6, -0.1);
                monkeys.subList(3, 5).forEach(monkey -> sit(monkey, player));
            });
            context.waitTicks(9);
            context.takeScreenshot("monkeys_moving");
        }
    }

    private static void spawnLineup(TestServerContext server, BiConsumer<List<MonkeyEntity>, ServerPlayer> setup) {
        server.runOnServer(s -> {
            ServerLevel level = s.overworld();
            monkeys(s).forEach(MonkeyEntity::discard);
            for (int x : SLOTS) {
                MonkeyEntity monkey = ModEntities.MONKEY.get().create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                monkey.setNoAi(true);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, 0);
                monkey.snapTo(x + 0.5, y, 0.5, 0, 0);
                monkey.setYHeadRot(0);
                monkey.setYBodyRot(0);
                level.addFreshEntity(monkey);
            }
            setup.accept(monkeys(s), s.getPlayerList().getPlayers().getFirst());
        });
    }

    private static void sit(MonkeyEntity monkey, ServerPlayer owner) {
        monkey.tame(owner);
        monkey.setOrderedToSit(true);
        monkey.setInSittingPose(true);
    }

    private static List<MonkeyEntity> monkeys(MinecraftServer server) {
        return server.overworld()
            .getEntitiesOfClass(MonkeyEntity.class, new AABB(-20, -70, -20, 20, -40, 20))
            .stream()
            .sorted(Comparator.comparingDouble(MonkeyEntity::getX))
            .toList();
    }
}
