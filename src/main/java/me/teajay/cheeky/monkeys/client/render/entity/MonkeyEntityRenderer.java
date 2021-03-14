package me.teajay.cheeky.monkeys.client.render.entity;

import me.teajay.cheeky.monkeys.client.model.MonkeyEntityModel;
import me.teajay.cheeky.monkeys.common.entity.MonkeyEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import software.bernie.geckolib3.renderer.geo.GeoEntityRenderer;

public class MonkeyEntityRenderer extends GeoEntityRenderer<MonkeyEntity>
{
    public MonkeyEntityRenderer(EntityRenderDispatcher renderManager) {
        super(renderManager, new MonkeyEntityModel());
        this.shadowRadius = 0.5F; //change 0.7 to the desired shadow size.
    }

    public void render(MonkeyEntity entity, float entityYaw, float partialTicks, MatrixStack stack, VertexConsumerProvider bufferIn, int packedLightIn) {
		stack.scale(0.5f, 0.5f, 0.5f);
		super.render(entity, entityYaw, partialTicks, stack, bufferIn, packedLightIn);
	}
}