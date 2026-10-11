package me.teajay.cheeky.monkeys.client;

import com.geckolib.constant.DataTickets;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.ItemInHandGeoLayer;
import me.teajay.cheeky.monkeys.CheekyMonkeys;
import me.teajay.cheeky.monkeys.entity.MonkeyEntity;
import me.teajay.cheeky.monkeys.entity.MonkeyState;
import me.teajay.cheeky.monkeys.entity.MonkeyVariant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class MonkeyRenderer extends GeoEntityRenderer<MonkeyEntity, LivingEntityRenderState> {
    // Extra bend per tail segment, so the tail curves up instead of sticking out straight
    private static final float TAIL_CURL_BASE = 15.0F * Mth.DEG_TO_RAD;
    private static final float TAIL_CURL_MIDDLE = 30.0F * Mth.DEG_TO_RAD;
    private static final float TAIL_CURL_END = 35.0F * Mth.DEG_TO_RAD;

    public MonkeyRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(CheekyMonkeys.id("monkey")) {
            @Override
            public Identifier getTextureResource(GeoRenderState renderState) {
                return renderState.getOrDefaultGeckolibData(MonkeyEntity.VARIANT_TICKET, MonkeyVariant.BROWN).texture();
            }
        });
        this.shadowRadius = 0.25F;
        this.withRenderLayer(new ItemInHandGeoLayer<>(context, this));
    }

    @Override
    public void addRenderData(MonkeyEntity monkey, @Nullable Void relatedObject, LivingEntityRenderState renderState, float partialTick) {
        renderState.addGeckolibData(MonkeyEntity.STATE_TICKET, monkey.getState());
        renderState.addGeckolibData(MonkeyEntity.VARIANT_TICKET, monkey.getVariant());
        renderState.addGeckolibData(MonkeyEntity.MOVE_SPEED_TICKET, renderState.walkAnimationSpeed);

        // On a shoulder, stick to the owner's shoulder as they're drawn this frame, rather than trailing behind where the server last put it
        Player shoulderOwner = monkey.getShoulderOwner();
        if (shoulderOwner != null) {
            float bodyYaw = Mth.rotLerp(partialTick, shoulderOwner.yBodyRotO, shoulderOwner.yBodyRot);
            Vec3 pos = MonkeyEntity.shoulderPosition(shoulderOwner, shoulderOwner.getPosition(partialTick), bodyYaw);
            renderState.x = pos.x;
            renderState.y = pos.y;
            renderState.z = pos.z;
            renderState.bodyRot = bodyYaw;
            renderState.addGeckolibData(DataTickets.ENTITY_BODY_YAW, bodyYaw);
        }

        // Climbing monkeys always face what they're climbing, whichever way they're headed
        if (monkey.getState() == MonkeyState.CLIMBING) {
            Direction face = monkey.getClimbedFace();
            if (face != null) {
                renderState.bodyRot = face.toYRot();
                renderState.yRot = 0.0F;
                renderState.addGeckolibData(DataTickets.ENTITY_BODY_YAW, renderState.bodyRot);
                renderState.addGeckolibData(DataTickets.ENTITY_YAW, 0.0F);
            }
        }
    }

    /// A monkey on your own shoulder would only block the view in first person
    @Override
    public boolean shouldRender(MonkeyEntity monkey, Frustum culler, double camX, double camY, double camZ, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (monkey.getShoulderOwner() != null && monkey.getShoulderOwner() == minecraft.getCameraEntity() && minecraft.options.getCameraType().isFirstPerson()) {
            return false;
        }

        return super.shouldRender(monkey, culler, camX, camY, camZ, partialTicks);
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, float widthScale, float heightScale) {
        float scale = MonkeyEntity.MODEL_SCALE * (renderPassInfo.renderState().isBaby ? MonkeyEntity.BABY_SCALE : 1.0F);
        super.scaleModelForRender(renderPassInfo, widthScale * scale, heightScale * scale);
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, BoneSnapshots snapshots) {
        LivingEntityRenderState renderState = renderPassInfo.renderState();
        MonkeyState state = renderPassInfo.getOrDefaultGeckolibData(MonkeyEntity.STATE_TICKET, MonkeyState.IDLE);
        float ageInTicks = renderState.ageInTicks;

        // Look at whatever the monkey is looking at, on top of the animated head pose
        if (state != MonkeyState.SLEEPING && state != MonkeyState.CLIMBING) {
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
        // and a curl upwards along the tail, where the pose doesn't already drape or wrap it
        float curl = switch (state) {
            case IDLE, DANCING, LEAPING -> 1.0F;
            case SITTING -> 0.5F;
            default -> 0.0F;
        };
        snapshots.get("Tail").ifPresent(tail -> {
            tail.setRotY(tail.getRotY() + sway);
            tail.setRotX(tail.getRotX() - curl * TAIL_CURL_BASE);
        });
        snapshots.get("TailBone").ifPresent(tail -> tail.setRotX(tail.getRotX() - curl * TAIL_CURL_MIDDLE));
        snapshots.get("TailEndBone").ifPresent(tail -> {
            tail.setRotY(tail.getRotY() + sway * 1.5F);
            tail.setRotX(tail.getRotX() - curl * TAIL_CURL_END);
        });
    }
}
