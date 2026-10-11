package me.teajay.cheeky.monkeys.registry;

import java.util.Set;
import java.util.function.Supplier;
import me.teajay.cheeky.monkeys.block.entity.MonkeyStashBlockEntity;
import me.teajay.cheeky.monkeys.platform.Platform;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static final Supplier<BlockEntityType<MonkeyStashBlockEntity>> MONKEY_STASH = Platform.INSTANCE.register(
        Registries.BLOCK_ENTITY_TYPE, "monkey_stash", key -> new BlockEntityType<>(MonkeyStashBlockEntity::new, Set.of(ModBlocks.MONKEY_STASH.get()))
    );

    private ModBlockEntities() {
    }

    public static void init() {
    }
}
