package me.teajay.cheeky.monkeys.client;

import me.teajay.cheeky.monkeys.client.network.EntityDispatcher;
import me.teajay.cheeky.monkeys.client.render.entity.MonkeyEntityRenderer;
import me.teajay.cheeky.monkeys.common.CheekyMonkeys;
import me.teajay.cheeky.monkeys.common.networking.Packets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.minecraft.client.render.RenderLayer;

public class CheekyMonkeysClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(Packets.SPAWN, EntityDispatcher::spawnFrom);
        EntityRendererRegistry.INSTANCE.register(
            CheekyMonkeys.MONKEY,
            (entityRenderDispatcher, context) -> new MonkeyEntityRenderer(entityRenderDispatcher)
        );
        BlockRenderLayerMap.INSTANCE.putBlock(CheekyMonkeys.BANANA_BLOCK, RenderLayer.getCutout());
    }
}
