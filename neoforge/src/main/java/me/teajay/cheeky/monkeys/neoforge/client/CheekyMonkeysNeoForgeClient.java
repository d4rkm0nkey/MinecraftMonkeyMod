package me.teajay.cheeky.monkeys.neoforge.client;

import me.teajay.cheeky.monkeys.CheekyMonkeys;
import me.teajay.cheeky.monkeys.client.CheekyMonkeysClient;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(value = CheekyMonkeys.MOD_ID, dist = Dist.CLIENT)
public final class CheekyMonkeysNeoForgeClient {
    public CheekyMonkeysNeoForgeClient(IEventBus modBus) {
        modBus.addListener(CheekyMonkeysNeoForgeClient::registerRenderers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        CheekyMonkeysClient.registerEntityRenderers(new CheekyMonkeysClient.EntityRendererRegistrar() {
            @Override
            public <E extends Entity> void register(EntityType<? extends E> type, EntityRendererProvider<E> provider) {
                event.registerEntityRenderer(type, provider);
            }
        });
    }
}
