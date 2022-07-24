package love.marblegate.boomood.mechanism.itemstackreversion.dataholder;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.NBTIngredient;
import net.minecraftforge.common.crafting.PartialNBTIngredient;

import javax.annotation.Nullable;

public class IngredientBox {
    public static final Codec<IngredientBox> CODEC = Codec.PASSTHROUGH.comapFlatMap(dynamic ->
            {
                try {
                    var jsonObject = dynamic.convert(JsonOps.INSTANCE).getValue().getAsJsonObject();
                    Type type;
                    if(jsonObject.has("mode"))
                        type = Type.CODEC.parse(JsonOps.INSTANCE,jsonObject.getAsJsonPrimitive("mode")).getOrThrow(false,err->{
                            throw new JsonSyntaxException(err);
                        });
                    else type = Type.STANDARD_MATCH;
                    Ingredient ingredient = switch (type){
                        case STANDARD_MATCH -> Ingredient.fromJson(jsonObject.get("match"));
                        case EXACT_MATCH -> NBTIngredient.Serializer.INSTANCE.parse((JsonObject) jsonObject.get("match"));
                        case PARTIAL_MATCH -> PartialNBTIngredient.Serializer.INSTANCE.parse((JsonObject) jsonObject.get("match"));
                    };
                    int min, max;
                    if(jsonObject.has("min")){
                        min = jsonObject.getAsJsonPrimitive("min").getAsInt();
                        if(min<1) throw new JsonSyntaxException("min must not be less than 1! min:" + min + ".");
                    }
                    else min = 1;
                    if(jsonObject.has("max")){
                        max = jsonObject.getAsJsonPrimitive("max").getAsInt();
                        if(max<1) throw new JsonSyntaxException("max must not be less than 1! max:" + max + ".");
                    }
                    else max = min;
                    if(min>max) throw new JsonSyntaxException("max must not be less than mix! max:" + max + ". min:" + min +".");
                    var ret = new IngredientBox(ingredient,type,min,max);
                    return DataResult.success(ret);
                } catch (Exception e) {
                    return DataResult.error(e.getMessage());
                }
            },
            box -> {
                var result = new JsonObject();
                result.add("mode",Type.CODEC.encodeStart(JsonOps.INSTANCE,box.type).getOrThrow(false, err->{}));
                var ingredientJson = box.ingredient.toJson();
                if(box.type!=Type.STANDARD_MATCH){
                    ingredientJson.getAsJsonObject().remove("type");
                }
                result.add("match",ingredientJson);
                result.addProperty("min",box.minCount);
                result.addProperty("max",box.maxCount);
                return new Dynamic<>(JsonOps.INSTANCE, result);
            }
    );


    private final Ingredient ingredient;
    private final Type type;
    @Nullable
    private final int minCount;
    @Nullable
    private final int maxCount;

    public IngredientBox(Ingredient ingredient, Type type, int minCount, int maxCount) {
        this.ingredient = ingredient;
        this.type = type;
        this.minCount = minCount;
        this.maxCount = maxCount;
    }

    public boolean test(ItemStack itemStack){
        return itemStack.getCount() >= minCount && ingredient.test(itemStack);
    }

    public int min() {
        return minCount;
    }

    public int max() {
        return maxCount;
    }

    @Override
    public String toString() {
        return "IngredientBox{" +
                "ingredient=" + ingredient +
                ", type=" + type +
                ", minCount=" + minCount +
                ", maxCount=" + maxCount +
                '}';
    }

    public enum Type{
        STANDARD_MATCH("standard"),
        EXACT_MATCH("exact"),
        PARTIAL_MATCH("partial");

        static final Codec<Type> CODEC = Codec.PASSTHROUGH.comapFlatMap(dynamic ->
                {
                    try {
                        var s = dynamic.convert(JsonOps.INSTANCE).getValue().getAsString();
                        if(s.equals("standard")){
                            return DataResult.success(STANDARD_MATCH);
                        } else if(s.equals("exact")){
                            return DataResult.success(EXACT_MATCH);
                        } else if(s.equals("partial")){
                            return DataResult.success(PARTIAL_MATCH);
                        } else {
                            return DataResult.error("Expected mode to be \"standard\", \"exact\" or \"partial\", was " + s);
                        }
                    } catch (Exception e) {
                        return DataResult.error(e.getMessage());
                    }
                },
                type -> new Dynamic<>(JsonOps.INSTANCE, new JsonPrimitive(type.ts)));

        private final String ts;

        Type(String ts) {
            this.ts = ts;
        }
    }
}
