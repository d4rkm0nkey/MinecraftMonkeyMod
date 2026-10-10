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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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

            context.getInput().pressKey(options -> options.keyToggleGui);
            treeClimbing(context, server, ground);
            climbToPlayer(context, server, ground);
            server.runCommand("gamemode survival @p");
            gapLeap(context, server, ground);
            thrownBanana(context, server, ground);
            pickpocket(context, server, ground);
        }
    }

    /// Up the trunk, swinging along under the canopy, up the side of the leaves, sitting on top and leaping to the next tree
    private static void treeClimbing(ClientGameTestContext context, TestServerContext server, int ground) {
        server.runCommand("fill -8 " + ground + " -10 12 " + (ground + 8) + " 0 minecraft:air");
        for (int x : new int[]{0, 7}) {
            server.runCommand("fill " + x + " " + ground + " -4 " + x + " " + (ground + 4) + " -4 minecraft:oak_log");
            server.runCommand("fill " + (x - 2) + " " + (ground + 3) + " -6 " + (x + 2) + " " + (ground + 4) + " -2 minecraft:oak_leaves[persistent=true] keep");
        }

        server.runCommand("gamemode spectator @p");
        camera(server, 3, ground + 1, 1, 180, -25);
        server.runOnServer(s -> {
            monkeys(s).forEach(MonkeyEntity::discard);
            MonkeyEntity monkey = ModEntities.MONKEY.get().create(s.overworld(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            monkey.snapTo(2.5, ground, 0.5, 180, 0);
            s.overworld().addFreshEntity(monkey);
            monkey.requestTreeClimb();
        });

        waitForState(context, server, MonkeyState.CLIMBING, 200);
        context.waitTicks(8);
        lookAtMonkey(context, server);
        context.takeScreenshot("tree_1_climb_trunk");
        waitForState(context, server, MonkeyState.SWINGING, 200);
        context.waitTicks(6);
        lookAtMonkey(context, server);
        context.takeScreenshot("tree_2_swing");
        waitForState(context, server, MonkeyState.CLIMBING, 200);
        context.waitTicks(8);
        lookAtMonkey(context, server);
        context.takeScreenshot("tree_3_climb_leaves");
        waitForState(context, server, MonkeyState.PERCHING, 200);
        context.waitTicks(10);
        lookAtMonkey(context, server);
        context.takeScreenshot("tree_4_perch");

        BlockPos perch = server.computeOnServer(s -> monkeys(s).getFirst().blockPosition());
        if (perch.getY() != ground + 5) {
            throw new AssertionError("Expected the monkey on top of the canopy at y " + (ground + 5) + ", but it's at " + perch);
        }

        // Over to the other tree
        double targetX = perch.getX() < 4 ? 6.5 : 1.5;
        server.runOnServer(s -> monkeys(s).getFirst().leapTo(new Vec3(targetX, ground + 5, -3.5), null));
        context.waitTicks(6);
        context.takeScreenshot("tree_5_leap");
        for (int tick = 0; tick < 40 && server.computeOnServer(s -> monkeys(s).getFirst().isGliding()); tick++) {
            context.waitTicks(1);
        }

        Vec3 landed = server.computeOnServer(s -> monkeys(s).getFirst().position());
        if (Math.abs(landed.x - targetX) > 0.5 || Math.abs(landed.y - (ground + 5)) > 0.1) {
            throw new AssertionError("Expected the monkey to land on the other tree at x " + targetX + ", but it's at " + landed);
        }
    }

    /// A player standing on top of a tree with a banana gets a monkey climbing up to them
    private static void climbToPlayer(ClientGameTestContext context, TestServerContext server, int ground) {
        server.runCommand("gamemode creative @p");
        server.runCommand("tp @p 7.5 " + (ground + 5) + " -3.5 -90 30");
        server.runCommand("item replace entity @p weapon.mainhand with cheeky_monkeys:banana");
        server.runOnServer(s -> {
            ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        });
        server.runOnServer(s -> {
            monkeys(s).forEach(MonkeyEntity::discard);
            MonkeyEntity monkey = ModEntities.MONKEY.get().create(s.overworld(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            monkey.snapTo(-1.5, ground, 3.5, 0, 0);
            s.overworld().addFreshEntity(monkey);
        });

        boolean climbing = false;
        for (int tick = 0; tick < 600; tick++) {
            Vec3 monkey = server.computeOnServer(s -> monkeys(s).getFirst().position());
            if (!climbing && server.computeOnServer(s -> monkeys(s).getFirst().getState()) == MonkeyState.CLIMBING) {
                climbing = true;
                context.waitTicks(6);
                Vec3 at = server.computeOnServer(s -> monkeys(s).getFirst().position());
                server.runCommand("tp @p 7.5 " + (ground + 5) + " -3.5 facing " + at.x + " " + (at.y + 0.3) + " " + at.z);
                context.waitTicks(2);
                context.takeScreenshot("tree_6_climb_to_player");
            }

            if (monkey.y >= ground + 4.5 && monkey.distanceTo(new Vec3(7.5, ground + 5, -3.5)) < 2.5) {
                server.runCommand("tp @p 7.5 " + (ground + 5) + " -3.5 facing " + monkey.x + " " + (monkey.y + 0.3) + " " + monkey.z);
                context.waitTicks(10);
                context.takeScreenshot("tree_7_reached_player");
                server.runCommand("item replace entity @p weapon.mainhand with minecraft:air");
                return;
            }

            context.waitTicks(1);
        }

        Vec3 monkey = server.computeOnServer(s -> monkeys(s).getFirst().position());
        String details = server.computeOnServer(s -> {
            MonkeyEntity m = monkeys(s).getFirst();
            ServerPlayer p = s.getPlayerList().getPlayers().getFirst();
            var route = me.teajay.cheeky.monkeys.entity.ai.TreeSpots.findClimb(s.overworld(), m.blockPosition(), 16, m.getRandom(), m.getVisualScale(), p.position());
            return m.getState() + " at " + m.position() + "; player at " + p.position() + " onGround " + p.onGround() + " below " + s.overworld().getBlockState(p.blockPosition().below())
                + " height " + me.teajay.cheeky.monkeys.entity.ai.TreeSpots.heightAboveGround(s.overworld(), p.blockPosition()) + " hand " + p.getMainHandItem()
                + " route " + (route == null ? null : route.start() + " perches " + route.perches());
        });
        throw new AssertionError("The monkey never climbed up to the player; it's " + details);
    }

    /// A monkey that can't walk to where it wants to go leaps over the gap instead
    private static void gapLeap(ClientGameTestContext context, TestServerContext server, int ground) {
        int top = ground + 4;
        server.runCommand("fill -8 " + ground + " -10 12 " + (ground + 8) + " 0 minecraft:air");
        server.runCommand("fill -3 " + ground + " 0 3 " + (top - 1) + " 2 minecraft:stone");
        server.runCommand("fill -3 " + ground + " -7 3 " + (top - 1) + " -4 minecraft:stone");
        camera(server, 7, top + 2, -2, 90, 25);
        server.runOnServer(s -> {
            monkeys(s).forEach(MonkeyEntity::discard);
            MonkeyEntity monkey = ModEntities.MONKEY.get().create(s.overworld(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            monkey.snapTo(0.5, top, 1.5, 180, 0);
            s.overworld().addFreshEntity(monkey);
        });

        for (int tick = 0; tick < 300; tick++) {
            MonkeyState state = server.computeOnServer(s -> {
                MonkeyEntity monkey = monkeys(s).getFirst();
                if (monkey.getNavigation().getPath() == null || !monkey.getNavigation().getPath().getTarget().equals(new BlockPos(0, top, -6))) {
                    monkey.getNavigation().moveTo(0.5, top, -5.5, 1.0);
                }

                return monkey.getState();
            });
            if (state == MonkeyState.LEAPING) {
                break;
            }

            context.waitTicks(1);
        }

        context.waitTicks(4);
        context.takeScreenshot("gap_leap");
        for (int tick = 0; tick < 40 && server.computeOnServer(s -> monkeys(s).getFirst().isGliding()); tick++) {
            context.waitTicks(1);
        }

        Vec3 landed = server.computeOnServer(s -> monkeys(s).getFirst().position());
        if (landed.z > -3.0 || landed.y < top) {
            throw new AssertionError("Expected the monkey to have leapt over the gap, but it's at " + landed);
        }
    }

    /// Moves the (spectating) player a few blocks away from the monkey, looking at it
    private static void lookAtMonkey(ClientGameTestContext context, TestServerContext server) {
        Vec3 monkey = server.computeOnServer(s -> monkeys(s).getFirst().position());
        server.runCommand("tp @p " + (monkey.x + 2.5) + " " + (monkey.y + 0.3) + " " + (monkey.z + 3.0) + " facing " + monkey.x + " " + (monkey.y + 0.4) + " " + monkey.z);
        context.waitTicks(2);
    }

    /// A wild monkey races for a thrown banana, grabs one and runs off to eat it
    private static void thrownBanana(ClientGameTestContext context, TestServerContext server, int ground) {
        server.runCommand("fill -8 " + ground + " 2 8 " + (ground + 8) + " 16 minecraft:air");
        server.runCommand("tp @p 0.5 " + ground + " 13.5 180 25");
        MonkeyEntity spawned = server.computeOnServer(s -> {
            monkeys(s).forEach(MonkeyEntity::discard);
            MonkeyEntity monkey = ModEntities.MONKEY.get().create(s.overworld(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            monkey.snapTo(-4.5, ground, 4.5, 0, 0);
            s.overworld().addFreshEntity(monkey);
            ItemEntity bananas = new ItemEntity(s.overworld(), 0.5, ground + 0.2, 8.5, new ItemStack(ModItems.BANANA.get(), 3));
            bananas.setThrower(s.getPlayerList().getPlayers().getFirst());
            s.overworld().addFreshEntity(bananas);
            return monkey;
        });

        waitUntil(context, server, "the monkey grabs a banana", 200, s -> spawned.getMainHandItem().is(ModItems.BANANA.get()));
        context.waitTicks(3);
        context.takeScreenshot("banana_grab");
        int left = server.computeOnServer(s -> s.overworld().getEntitiesOfClass(ItemEntity.class, new AABB(-10, ground - 2, 0, 10, ground + 4, 16))
            .stream().mapToInt(item -> item.getItem().getCount()).sum());
        if (left != 2) {
            String items = server.computeOnServer(s -> s.overworld().getEntitiesOfClass(ItemEntity.class, new AABB(-30, ground - 5, -30, 30, ground + 10, 30))
                .stream().map(item -> item.getItem() + " at " + item.position()).toList()
                + " monkeys " + s.overworld().getEntitiesOfClass(MonkeyEntity.class, new AABB(-100, -100, -100, 100, 100, 100)).stream().map(m -> m.position() + " " + m.getMainHandItem()).toList()
                + " player bananas " + s.getPlayerList().getPlayers().getFirst().getInventory().countItem(ModItems.BANANA.get()));
            throw new AssertionError("Expected the monkey to take exactly one banana, but " + left + " are left: " + items + ", monkey holds " + spawned.getMainHandItem());
        }

        waitUntil(context, server, "the monkey eats the banana", 400, s -> spawned.isEating());
        context.waitTicks(10);
        lookAtMonkey(context, server);
        context.takeScreenshot("banana_eat");
        server.runCommand("kill @e[type=minecraft:item]");
    }

    /// A wild monkey sneaks up behind the player and steals from their pockets; a smack or a banana gets the loot back
    private static void pickpocket(ClientGameTestContext context, TestServerContext server, int ground) {
        server.runCommand("clear @p");
        server.runCommand("give @p minecraft:diamond 5");
        // Looking away from the monkey
        server.runCommand("tp @p 0.5 " + ground + " 12.5 0 0");
        MonkeyEntity thief = server.computeOnServer(s -> {
            monkeys(s).forEach(MonkeyEntity::discard);
            MonkeyEntity monkey = ModEntities.MONKEY.get().create(s.overworld(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            monkey.snapTo(0.5, ground, 5.5, 0, 0);
            s.overworld().addFreshEntity(monkey);
            monkey.setTheftCooldown(0);
            return monkey;
        });

        waitUntil(context, server, "the monkey steals something", 500, s -> thief.hasLoot());
        context.waitTicks(4);
        lookAtMonkey(context, server);
        context.takeScreenshot("pickpocket");
        int diamonds = server.computeOnServer(s -> s.getPlayerList().getPlayers().getFirst().getInventory().countItem(Items.DIAMOND));
        if (diamonds >= 5 || !thief.getMainHandItem().is(Items.DIAMOND)) {
            throw new AssertionError("Expected the monkey to hold stolen diamonds, but the player still has " + diamonds + " and the monkey " + thief.getMainHandItem());
        }

        // A smack makes it drop the loot
        server.runOnServer(s -> {
            ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
            thief.hurtServer(s.overworld(), s.overworld().damageSources().playerAttack(player), 1.0F);
        });
        context.waitTicks(2);
        boolean dropped = server.computeOnServer(s -> !thief.hasLoot() && !s.overworld().getEntitiesOfClass(ItemEntity.class, thief.getBoundingBox().inflate(4.0),
            item -> item.getItem().is(Items.DIAMOND)).isEmpty());
        if (!dropped) {
            throw new AssertionError("Expected the monkey to drop the diamonds when hit");
        }

        // So does a banana
        server.runOnServer(s -> {
            ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
            thief.takeLoot(new ItemStack(Items.EMERALD));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BANANA.get()));
            thief.mobInteract(player, InteractionHand.MAIN_HAND);
        });
        if (server.computeOnServer(s -> thief.hasLoot() || !thief.isEating())) {
            throw new AssertionError("Expected the monkey to swap its loot for the banana");
        }

        server.runCommand("kill @e[type=minecraft:item]");
    }

    private static void waitUntil(ClientGameTestContext context, TestServerContext server, String what, int timeout, java.util.function.Predicate<MinecraftServer> condition) {
        for (int tick = 0; tick < timeout; tick++) {
            if (server.computeOnServer(condition::test)) {
                return;
            }

            context.waitTicks(1);
        }

        String state = server.computeOnServer(s -> {
            ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
            return monkeys(s).stream().map(m -> m.getState() + " at " + m.position() + " holding " + m.getMainHandItem() + " runningOff " + m.isRunningOff()
                + " canPickpocket " + m.canPickpocket() + " path " + (m.getNavigation().getPath() == null ? null : m.getNavigation().getPath().getTarget())).toList()
                + "; player at " + player.position() + " " + player.gameMode() + " look " + player.getLookAngle() + " inventory " + player.getInventory().getNonEquipmentItems().getFirst();
        });
        throw new AssertionError("Timed out waiting until " + what + ": " + state);
    }

    /// Puts the player on an invisible block at the given spot, looking the given way
    private static void camera(TestServerContext server, int x, int y, int z, int yaw, int pitch) {
        server.runCommand("setblock " + x + " " + (y - 1) + " " + z + " minecraft:barrier");
        server.runCommand("tp @p " + (x + 0.5) + " " + y + " " + (z + 0.5) + " " + yaw + " " + pitch);
    }

    private static void waitForState(ClientGameTestContext context, TestServerContext server, MonkeyState state, int timeout) {
        for (int tick = 0; tick < timeout; tick++) {
            if (server.computeOnServer(s -> monkeys(s).getFirst().getState()) == state) {
                return;
            }

            context.waitTicks(1);
        }

        String details = server.computeOnServer(s -> {
            MonkeyEntity monkey = monkeys(s).getFirst();
            var route = me.teajay.cheeky.monkeys.entity.ai.TreeSpots.findClimb(s.overworld(), monkey.blockPosition(), 12, monkey.getRandom(), monkey.getVisualScale(), null);
            var path = monkey.getNavigation().getPath();
            return monkey.getState() + " at " + monkey.position() + ", onGround " + monkey.onGround() + ", requested " + monkey.isTreeClimbRequested()
                + ", route " + (route == null ? null : route.start() + " " + route.path()) + ", path " + (path == null ? null : path.getTarget() + " done " + path.isDone() + " reach " + path.canReach());
        });
        throw new AssertionError("The monkey never got to " + state + "; it's " + details);
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
