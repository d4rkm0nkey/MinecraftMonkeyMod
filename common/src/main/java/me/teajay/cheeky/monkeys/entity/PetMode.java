package me.teajay.cheeky.monkeys.entity;

import net.minecraft.network.chat.Component;

/// What a tame monkey does when it's not on its owner's shoulder. Right-clicking it cycles through these in order.
public enum PetMode {
    /// Stays close to its owner, warns about monsters and snatches their weapons
    FOLLOW("follow"),
    /// Sits where it is
    STAY("stay"),
    /// Roams and climbs around the spot it was left at
    WANDER("wander"),
    /// Follows and fetches items lying around its owner
    GATHER("gather");

    private static final PetMode[] VALUES = values();
    private final String name;

    PetMode(String name) {
        this.name = name;
    }

    public String serializedName() {
        return this.name;
    }

    public PetMode next() {
        return VALUES[(this.ordinal() + 1) % VALUES.length];
    }

    public boolean followsOwner() {
        return this == FOLLOW || this == GATHER;
    }

    /// "Coco is now following you" and so on
    public Component describe(Component monkeyName) {
        return Component.translatable("message.cheeky_monkeys.mode." + this.name, monkeyName);
    }

    public static PetMode byName(String name) {
        for (PetMode mode : VALUES) {
            if (mode.name.equals(name)) {
                return mode;
            }
        }

        return FOLLOW;
    }
}
