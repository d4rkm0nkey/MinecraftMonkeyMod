package me.teajay.cheeky.monkeys.entity;

import com.geckolib.animation.RawAnimation;

/// Looping base animations, named as in `geckolib/animations/entity/monkey.animation.json`
public final class MonkeyAnimations {
    public static final RawAnimation IDLE = loop("idle");
    public static final RawAnimation WALK = loop("walk");
    public static final RawAnimation RUN = loop("run");
    public static final RawAnimation SIT = loop("sit");
    public static final RawAnimation SLEEP = loop("sleep");
    public static final RawAnimation CLIMB = loop("climb");
    public static final RawAnimation HANG = loop("hang");
    public static final RawAnimation PERCH = loop("perch");
    public static final RawAnimation LEAP = RawAnimation.begin().thenPlayAndHold(prefixed("leap"));
    public static final RawAnimation DANCE = loop("dance");

    private MonkeyAnimations() {
    }

    static String prefixed(String name) {
        return "animation.monkey." + name;
    }

    private static RawAnimation loop(String name) {
        return RawAnimation.begin().thenLoop(prefixed(name));
    }
}
