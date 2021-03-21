package me.teajay.cheeky.monkeys.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import me.teajay.cheeky.monkeys.common.CheekyMonkeys;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.DefaultBiomeFeatures;

@Mixin(DefaultBiomeFeatures.class)
public class DefaultBiomeFeaturesMixin {

    // @Redirect(method="addJungleTrees()", @At=(value = "INVOKE", target = "Lnet/"))
    @Overwrite
    public static void addJungleTrees(GenerationSettings.Builder builder) {
        builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, CheekyMonkeys.BANANA_TREES_JUNGLE);
    }
}
