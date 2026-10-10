package me.teajay.cheeky.monkeys.entity;

import com.geckolib.animation.RawAnimation;
import java.util.function.Supplier;
import me.teajay.cheeky.monkeys.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import org.jspecify.annotations.Nullable;

/// One-off animations played over the base pose by the monkey's action controller
public enum MonkeyEmote {
    CHEST_POUND("chest_pound", 40, ModSounds.MONKEY_CHEST_POUND),
    CHEST_RUB("chest_rub", 50, null),
    SCRATCH_HEAD("scratch_head", 40, null),
    LOOK_AROUND("look_around", 50, null),
    HAPPY("happy", 50, ModSounds.MONKEY_HAPPY),
    EAT("eat", 40, ModSounds.MONKEY_EAT);

    /// Emotes a monkey may do on its own while idling
    public static final MonkeyEmote[] IDLE_EMOTES = {CHEST_POUND, CHEST_RUB, SCRATCH_HEAD, LOOK_AROUND};

    private final String name;
    private final int durationTicks;
    private final RawAnimation animation;
    private final @Nullable Supplier<SoundEvent> sound;

    MonkeyEmote(String name, int durationTicks, @Nullable Supplier<SoundEvent> sound) {
        this.name = name;
        this.durationTicks = durationTicks;
        this.animation = RawAnimation.begin().thenPlay(MonkeyAnimations.prefixed(name));
        this.sound = sound;
    }

    public String animationName() {
        return this.name;
    }

    public int durationTicks() {
        return this.durationTicks;
    }

    public RawAnimation animation() {
        return this.animation;
    }

    public @Nullable SoundEvent sound() {
        return this.sound == null ? null : this.sound.get();
    }
}
