package me.teajay.cheeky.monkeys.mixin;

import com.mojang.serialization.Codec;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.gen.tree.TreeDecorator;
import net.minecraft.world.gen.tree.TreeDecoratorType;

@Mixin(TreeDecoratorType.class)
public interface TreeDecoratorTypeInvoker {
    @Invoker("register")
    public static <P extends TreeDecorator> TreeDecoratorType<P> invokeRegister(String id, Codec<P> codec) {
        throw new AssertionError();
    }
    
}
