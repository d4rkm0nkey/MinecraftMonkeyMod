package me.lumpy.monkey.cheeky.monkeys.client;

import me.lumpy.monkey.cheeky.monkeys.client.render.entity.MonkeyEntityRenderer;
import me.lumpy.monkey.cheeky.monkeys.common.CheekyMonkeys;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;

public class CheekyMonkeysClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.INSTANCE.register(
            CheekyMonkeys.MONKEY,
            (entityRenderDispatcher, context) -> new MonkeyEntityRenderer(entityRenderDispatcher)
        );
    }
    
}
