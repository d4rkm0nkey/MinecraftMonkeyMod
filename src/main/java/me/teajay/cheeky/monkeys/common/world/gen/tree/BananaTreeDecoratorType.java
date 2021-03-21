package me.teajay.cheeky.monkeys.common.world.gen.tree;

import com.mojang.serialization.Codec;

import me.teajay.cheeky.monkeys.common.world.gen.BananaTreeDecorator;
import me.teajay.cheeky.monkeys.mixin.TreeDecoratorTypeInvoker;
import net.minecraft.world.gen.tree.TreeDecorator;
import net.minecraft.world.gen.tree.TreeDecoratorType;

public class BananaTreeDecoratorType<P extends TreeDecorator> {
    public static final TreeDecoratorType<BananaTreeDecorator> BANANA;

    private static <P extends TreeDecorator> TreeDecoratorType<P> register(String id, Codec<P> codec) {
        return TreeDecoratorTypeInvoker.invokeRegister(id, codec);
    }

    static {
        BANANA = register("banana", BananaTreeDecorator.CODEC);
    }
}