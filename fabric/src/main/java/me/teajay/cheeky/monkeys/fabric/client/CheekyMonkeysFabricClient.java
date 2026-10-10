package me.teajay.cheeky.monkeys.fabric.client;

import me.teajay.cheeky.monkeys.client.CheekyMonkeysClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public final class CheekyMonkeysFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CheekyMonkeysClient.registerEntityRenderers(new CheekyMonkeysClient.EntityRendererRegistrar() {
            @Override
            public <E extends Entity> void register(EntityType<? extends E> type, EntityRendererProvider<E> provider) {
                EntityRendererRegistry.register(type, provider);
            }
        });
    }
}
