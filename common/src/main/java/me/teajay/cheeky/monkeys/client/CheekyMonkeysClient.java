package me.teajay.cheeky.monkeys.client;

import java.util.function.Supplier;
import me.teajay.cheeky.monkeys.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public final class CheekyMonkeysClient {
    private CheekyMonkeysClient() {
    }

    public interface EntityRendererRegistrar {
        <E extends Entity> void register(EntityType<? extends E> type, EntityRendererProvider<E> provider);
    }

    public static void registerEntityRenderers(EntityRendererRegistrar registrar) {
        registerRenderer(registrar, ModEntities.MONKEY, MonkeyRenderer::new);
    }

    private static <E extends Entity> void registerRenderer(EntityRendererRegistrar registrar, Supplier<EntityType<E>> type, EntityRendererProvider<E> provider) {
        registrar.register(type.get(), provider);
    }
}
