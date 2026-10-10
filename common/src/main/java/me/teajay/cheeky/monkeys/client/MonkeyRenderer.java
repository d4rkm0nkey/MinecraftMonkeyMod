package me.teajay.cheeky.monkeys.client;

import com.geckolib.constant.DataTickets;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.ItemInHandGeoLayer;
import me.teajay.cheeky.monkeys.CheekyMonkeys;
import me.teajay.cheeky.monkeys.entity.MonkeyEntity;
import me.teajay.cheeky.monkeys.entity.MonkeyState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

public class MonkeyRenderer extends GeoEntityRenderer<MonkeyEntity, LivingEntityRenderState> {
    // GeckoLib only applies the entity's scale attribute, not the baby age scale
    private static final float BABY_SCALE = 0.6F;

    public MonkeyRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(CheekyMonkeys.id("monkey")));
        this.shadowRadius = 0.35F;
        this.withRenderLayer(new ItemInHandGeoLayer<>(context, this));
    }

    @Override
    public void addRenderData(MonkeyEntity monkey, @Nullable Void relatedObject, LivingEntityRenderState renderState, float partialTick) {
        renderState.addGeckolibData(MonkeyEntity.STATE_TICKET, monkey.getState());
        renderState.addGeckolibData(MonkeyEntity.MOVE_SPEED_TICKET, renderState.walkAnimationSpeed);
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, float widthScale, float heightScale) {
        float ageScale = renderPassInfo.renderState().isBaby ? BABY_SCALE : 1.0F;
        super.scaleModelForRender(renderPassInfo, widthScale * ageScale, heightScale * ageScale);
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, BoneSnapshots snapshots) {
        LivingEntityRenderState renderState = renderPassInfo.renderState();
        MonkeyState state = renderPassInfo.getOrDefaultGeckolibData(MonkeyEntity.STATE_TICKET, MonkeyState.IDLE);
        float ageInTicks = renderState.ageInTicks;

        // Look at whatever the monkey is looking at, on top of the animated head pose
        if (state != MonkeyState.SLEEPING) {
            float pitch = renderPassInfo.getOrDefaultGeckolibData(DataTickets.ENTITY_PITCH, 0.0F);
            float yaw = renderPassInfo.getOrDefaultGeckolibData(DataTickets.ENTITY_YAW, 0.0F);

            snapshots.get("HeadBone").ifPresent(head -> {
                head.setRotX(head.getRotX() - pitch * Mth.DEG_TO_RAD);
                head.setRotY(head.getRotY() - yaw * Mth.DEG_TO_RAD);
            });
        }

        // A lazy tail sway so the monkey never looks frozen
        float swaySpeed = state == MonkeyState.SLEEPING ? 0.03F : 0.08F;
        float sway = Mth.sin(ageInTicks * swaySpeed) * 0.12F;
        snapshots.get("Tail").ifPresent(tail -> tail.setRotY(tail.getRotY() + sway));
        snapshots.get("TailEndBone").ifPresent(tail -> tail.setRotY(tail.getRotY() + sway * 1.5F));
    }
}
