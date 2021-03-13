package me.lumpy.monkey.cheeky.monkeys.client.model;

import me.lumpy.monkey.cheeky.monkeys.common.CheekyMonkeys;
import me.lumpy.monkey.cheeky.monkeys.common.entity.MonkeyEntity;
import net.minecraft.util.Identifier;
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
    
}
