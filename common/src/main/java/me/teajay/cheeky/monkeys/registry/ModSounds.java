package me.teajay.cheeky.monkeys.registry;

import java.util.function.Supplier;
import me.teajay.cheeky.monkeys.CheekyMonkeys;
import me.teajay.cheeky.monkeys.platform.Platform;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
    public static final Supplier<SoundEvent> MONKEY_AMBIENT = register("entity.monkey.ambient");
    public static final Supplier<SoundEvent> MONKEY_HURT = register("entity.monkey.hurt");
    public static final Supplier<SoundEvent> MONKEY_DEATH = register("entity.monkey.death");
    public static final Supplier<SoundEvent> MONKEY_EAT = register("entity.monkey.eat");
    public static final Supplier<SoundEvent> MONKEY_HAPPY = register("entity.monkey.happy");
    public static final Supplier<SoundEvent> MONKEY_CHEST_POUND = register("entity.monkey.chest_pound");

    private ModSounds() {
    }

    private static Supplier<SoundEvent> register(String name) {
        return Platform.INSTANCE.register(Registries.SOUND_EVENT, name, key -> SoundEvent.createVariableRangeEvent(CheekyMonkeys.id(name)));
    }

    public static void init() {
    }
}
