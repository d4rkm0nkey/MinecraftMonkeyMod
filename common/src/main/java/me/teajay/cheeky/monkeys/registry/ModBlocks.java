package me.teajay.cheeky.monkeys.registry;

import java.util.function.Supplier;
import me.teajay.cheeky.monkeys.block.BananaBunchBlock;
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

    private ModBlocks() {
    }

    public static void init() {
    }
}
