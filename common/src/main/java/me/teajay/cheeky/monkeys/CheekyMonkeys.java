package me.teajay.cheeky.monkeys;

import me.teajay.cheeky.monkeys.registry.ModBlockEntities;
import me.teajay.cheeky.monkeys.registry.ModBlocks;
import me.teajay.cheeky.monkeys.registry.ModEntities;
import me.teajay.cheeky.monkeys.registry.ModItems;
import me.teajay.cheeky.monkeys.registry.ModSounds;
import me.teajay.cheeky.monkeys.registry.ModFeatures;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CheekyMonkeys {
    public static final String MOD_ID = "cheeky_monkeys";
    public static final Logger LOGGER = LoggerFactory.getLogger("Cheeky Monkeys");

    private CheekyMonkeys() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /// Called by each loader during mod construction, before any registry events fire.
    public static void init() {
        CheekyMonkeysConfig.load();
        ModSounds.init();
        ModBlocks.init();
        ModBlockEntities.init();
        ModEntities.init();
        ModItems.init();
        ModFeatures.init();
    }
}
