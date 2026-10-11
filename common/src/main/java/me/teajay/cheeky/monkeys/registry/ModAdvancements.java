package me.teajay.cheeky.monkeys.registry;

import me.teajay.cheeky.monkeys.CheekyMonkeys;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;

/// Advancements that no vanilla trigger covers, granted from code. They live in `data/cheeky_monkeys/advancement/husbandry`
/// with a single `granted` criterion.
public final class ModAdvancements {
    public static final String MONKEY_ON_YOUR_BACK = "husbandry/monkey_on_your_back";
    public static final String DAYLIGHT_ROBBERY = "husbandry/daylight_robbery";
    public static final String FINDERS_KEEPERS = "husbandry/finders_keepers";

    private ModAdvancements() {
    }

    public static void award(ServerPlayer player, String advancement) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(CheekyMonkeys.id(advancement));
        if (holder != null) {
            player.getAdvancements().award(holder, "granted");
        }
    }
}
