package me.teajay.cheeky.monkeys.client.model;

import me.teajay.cheeky.monkeys.common.CheekyMonkeys;
import me.teajay.cheeky.monkeys.common.entity.MonkeyEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class MonkeyEntityModel extends AnimatedGeoModel<MonkeyEntity> {

    @Override
    public Identifier getAnimationFileLocation(MonkeyEntity monkey) {
        return new Identifier(CheekyMonkeys.MODID, "animations/entity/monkey.animation.json");
    }

    @Override
    public Identifier getModelLocation(MonkeyEntity monkey) {
        return new Identifier(CheekyMonkeys.MODID, "geo/entity/monkey.geo.json");
    }

    @Override
    public Identifier getTextureLocation(MonkeyEntity monkey) {
        return new Identifier(CheekyMonkeys.MODID, "textures/entity/monkey.png");
    }

    @Override
    public void setLivingAnimations(MonkeyEntity entity, Integer uniqueID, AnimationEvent customPredicate) {
        super.setLivingAnimations(entity, uniqueID, customPredicate);

        if (entity.isBaby()) {
            IBone root = this.getAnimationProcessor().getBone("Root");
            if (root != null) {
                root.setScaleX(0.5f);
                root.setScaleY(0.5f);
                root.setScaleZ(0.5f);
            }
        }
    }
}
