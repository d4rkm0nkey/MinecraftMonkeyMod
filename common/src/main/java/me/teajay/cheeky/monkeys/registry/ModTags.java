package me.teajay.cheeky.monkeys.registry;

import me.teajay.cheeky.monkeys.CheekyMonkeys;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    /// Food that tempts, tames, heals and breeds monkeys
    public static final TagKey<Item> MONKEY_FOOD = TagKey.create(Registries.ITEM, CheekyMonkeys.id("monkey_food"));
    /// Blocks monkeys may naturally spawn on
    public static final TagKey<Block> MONKEYS_SPAWNABLE_ON = TagKey.create(Registries.BLOCK, CheekyMonkeys.id("monkeys_spawnable_on"));
    /// Biomes that get wild monkeys and banana trees
    public static final TagKey<Biome> HAS_MONKEYS = TagKey.create(Registries.BIOME, CheekyMonkeys.id("has_monkeys"));
    /// Snowy forests, home of the grey, red-faced snow monkeys
    public static final TagKey<Biome> HAS_SNOW_MONKEYS = TagKey.create(Registries.BIOME, CheekyMonkeys.id("has_snow_monkeys"));

    private ModTags() {
    }
}
