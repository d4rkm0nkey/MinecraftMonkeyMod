package me.teajay.cheeky.monkeys.common.world;

import me.teajay.cheeky.monkeys.common.CheekyMonkeys;
import me.teajay.cheeky.monkeys.common.entity.MonkeyEntity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.SpawnReason;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.GameRules;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.gen.Spawner;
import net.minecraft.entity.SpawnRestriction;

import java.util.List;
import java.util.Random;

public class MonkeySpawner implements Spawner {
    private int ticksUntilNextSpawn;

    private int spawnMonkey(ServerWorld world, BlockPos pos) {
        if (MonkeyEntity.canSpawn(CheekyMonkeys.MONKEY, world, SpawnReason.NATURAL, pos, world.getRandom())) {
            MonkeyEntity monkeyEntity = CheekyMonkeys.MONKEY.create(world);
            if (monkeyEntity != null) {
                monkeyEntity.initialize(world, world.getLocalDifficulty(pos), SpawnReason.NATURAL, (EntityData) null, (CompoundTag) null);
                monkeyEntity.refreshPositionAndAngles(pos, 0.0F, 0.0F);
                world.spawnEntityAndPassengers(monkeyEntity);
                return 1;
            }
        }
        return 0;
    }

    @Override
    public int spawn(ServerWorld world, boolean spawnMonsters, boolean spawnAnimals) {
        int spawnedMonkeys = 0;
        if (spawnAnimals && world.getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING)) {
            --this.ticksUntilNextSpawn;
            if (this.ticksUntilNextSpawn <= 0) {
                this.ticksUntilNextSpawn = 300;
                Random random = world.random;
                for(ServerPlayerEntity player : world.getPlayers()) {
                    int i = (8 + random.nextInt(24)) * (random.nextBoolean() ? -1 : 1);
                    int j = (8 + random.nextInt(24)) * (random.nextBoolean() ? -1 : 1);
                    BlockPos blockPos = player.getBlockPos().add(i, 0, j);
                    if (world.isRegionLoaded(blockPos.getX() - 10, blockPos.getY() - 10, blockPos.getZ() - 10, blockPos.getX() + 10, blockPos.getY() + 10, blockPos.getZ() + 10)) {
                        if (SpawnHelper.canSpawn(SpawnRestriction.Location.ON_GROUND, world, blockPos, CheekyMonkeys.MONKEY)) {
                            List<MonkeyEntity> monkeys = world.getNonSpectatingEntities(MonkeyEntity.class, (new Box(blockPos)).expand(96.0, 16.0D, 96.0D));
                            if (monkeys.size() < 5) {
                                spawnedMonkeys += this.spawnMonkey(world, blockPos);
                            }
                        }
                    }
                };
            }
        }
        return spawnedMonkeys;
    }    
}
