package me.teajay.cheeky.monkeys.registry;

import java.util.function.Supplier;
import me.teajay.cheeky.monkeys.block.BananaBunchBlock;
import me.teajay.cheeky.monkeys.block.MonkeyStashBlock;
import me.teajay.cheeky.monkeys.platform.Platform;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class ModBlocks {
    public static final Supplier<BananaBunchBlock> BANANA_BUNCH = Platform.INSTANCE.register(
        Registries.BLOCK,
        "banana_bunch",
        key -> new BananaBunchBlock(
            BlockBehaviour.Properties.of()
                .setId(key)
                .mapColor(MapColor.PLANT)
                .randomTicks()
                .strength(0.2F, 3.0F)
                .sound(SoundType.WOOD)
                .noOcclusion()
                .pushReaction(PushReaction.POPPED)
        )
    );

    public static final Supplier<MonkeyStashBlock> MONKEY_STASH = Platform.INSTANCE.register(
        Registries.BLOCK,
        "monkey_stash",
        key -> new MonkeyStashBlock(
            BlockBehaviour.Properties.of()
                .setId(key)
                .mapColor(MapColor.WOOD)
                .strength(0.5F)
                .sound(SoundType.GRASS)
                .noOcclusion()
                .ignitedByLava()
        )
    );

    private ModBlocks() {
    }

    public static void init() {
    }
}
