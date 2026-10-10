package me.teajay.cheeky.monkeys.neoforge;

import me.teajay.cheeky.monkeys.CheekyMonkeys;
import me.teajay.cheeky.monkeys.platform.Platform;
import me.teajay.cheeky.monkeys.registry.ModItems;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(CheekyMonkeys.MOD_ID)
public final class CheekyMonkeysNeoForge {
    public CheekyMonkeysNeoForge(IEventBus modBus) {
        CheekyMonkeys.init();
        ((NeoForgePlatform)Platform.INSTANCE).registerAll(modBus);
        modBus.addListener(CheekyMonkeysNeoForge::addCreativeTabItems);
    }

    private static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        CreativeModeTab.TabVisibility visibility = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.insertAfter(new ItemStack(Items.SWEET_BERRIES), new ItemStack(ModItems.BANANA.get()), visibility);
        } else if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.insertAfter(new ItemStack(Items.COCOA_BEANS), new ItemStack(ModItems.BANANA.get()), visibility);
        } else if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.insertAfter(new ItemStack(Items.MOOSHROOM_SPAWN_EGG), new ItemStack(ModItems.MONKEY_SPAWN_EGG.get()), visibility);
        }
    }
}
