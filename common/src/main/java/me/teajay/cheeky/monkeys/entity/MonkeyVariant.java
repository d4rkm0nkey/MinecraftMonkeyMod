package me.teajay.cheeky.monkeys.entity;

import me.teajay.cheeky.monkeys.CheekyMonkeys;
import net.minecraft.resources.Identifier;

/// What a monkey looks like, decided by where it was born
public enum MonkeyVariant {
    /// The jungle monkey
    BROWN("brown", "textures/entity/monkey.png"),
    /// Grey-furred and red-faced, from snowy forests; never feels the cold
    SNOW("snow", "textures/entity/snow_monkey.png");

    private static final MonkeyVariant[] VALUES = values();
    private final String name;
    private final Identifier texture;

    MonkeyVariant(String name, String texture) {
        this.name = name;
        this.texture = CheekyMonkeys.id(texture);
    }

    public String serializedName() {
        return this.name;
    }

    public Identifier texture() {
        return this.texture;
    }

    public static MonkeyVariant byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : BROWN;
    }

    public static MonkeyVariant byName(String name) {
        for (MonkeyVariant variant : VALUES) {
            if (variant.name.equals(name)) {
                return variant;
            }
        }

        return BROWN;
    }
}
