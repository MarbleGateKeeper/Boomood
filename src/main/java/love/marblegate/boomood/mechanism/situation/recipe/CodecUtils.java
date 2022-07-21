package love.marblegate.boomood.mechanism.situation.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.item.crafting.Ingredient;

public class CodecUtils {
    public static final Codec<Ingredient> INGREDIENT_CODEC = Codec.PASSTHROUGH.comapFlatMap(dynamic ->
            {
                try {
                    Ingredient ingredient = Ingredient.fromJson(dynamic.convert(JsonOps.INSTANCE).getValue());
                    return DataResult.success(ingredient);
                } catch (Exception e) {
                    return DataResult.error(e.getMessage());
                }
            },
            ingredient -> new Dynamic<>(JsonOps.INSTANCE, ingredient.toJson()));

}
