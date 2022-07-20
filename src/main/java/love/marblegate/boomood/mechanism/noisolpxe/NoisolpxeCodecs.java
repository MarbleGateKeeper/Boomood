package love.marblegate.boomood.mechanism.noisolpxe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class NoisolpxeCodecs {
    static final Codec<Ingredient> INGREDIENT_CODEC = Codec.PASSTHROUGH.comapFlatMap(dynamic ->
            {
                try {
                    Ingredient ingredient = Ingredient.fromJson(dynamic.convert(JsonOps.INSTANCE).getValue());
                    return DataResult.success(ingredient);
                } catch (Exception e) {
                    return DataResult.error(e.getMessage());
                }
            },
            ingredient -> new Dynamic<>(JsonOps.INSTANCE, ingredient.toJson()));

    static Codec<ItemStackDropSituationHandler> SITUATION_HANDLER_CODEC = Codec.PASSTHROUGH.comapFlatMap(dynamic ->
    {
        try {
            var st = dynamic.convert(JsonOps.INSTANCE).getValue().getAsJsonObject();
            var type = st.getAsJsonPrimitive("type").getAsString();
            return switch (type) {
                case "entity_death" -> DataResult.success(new ItemStackDropSituationHandler.EntityDeath(st));
                case "chest_destruction" -> DataResult.success(new ItemStackDropSituationHandler.ChestDestruction());
                case "item_frame_destruction" -> DataResult.success(new ItemStackDropSituationHandler.ItemFrameDestruction());
                case "armor_stand_destruction" -> DataResult.success(new ItemStackDropSituationHandler.ArmorStandDestruction());
                case "block_destruction" -> DataResult.success(new ItemStackDropSituationHandler.BlockDestruction(st));
                default -> throw new JsonSyntaxException("Expected type to be \"entity_death\", \"chest_destruction\", \"item_frame_destruction\", \"armor_stand_destruction\" or \"block_destruction\", was " + st);
            };
        } catch (Exception e) {
            return DataResult.error(e.getMessage());
        }
    }, handler -> new Dynamic<>(JsonOps.INSTANCE,handler.toJson()));


    static Codec<List<ItemStackEntityRevertPredicate.Condition>> PREDICATE_CONDITIONS_CODEC = Codec.PASSTHROUGH.comapFlatMap(dynamic ->
    {
        try {
            var json = dynamic.convert(JsonOps.INSTANCE).getValue().getAsJsonObject();
            List<ItemStackEntityRevertPredicate.Condition> ret = new ArrayList<>();
            if (json.size() != 0) {
                var heightConditionJson = json.getAsJsonObject("height");
                if (heightConditionJson != null)
                    ret.add(new ItemStackEntityRevertPredicate.HeightPredicate(heightConditionJson));
                var biomeConditionJson = json.getAsJsonObject("biome");
                if (biomeConditionJson != null)
                    ret.add(new ItemStackEntityRevertPredicate.BiomePredicate(biomeConditionJson));
            }
            return DataResult.success(ret);
        } catch (Exception e) {
            return DataResult.error(e.getMessage());
        }
    }, conditions -> {
        var ret = new JsonObject();
        for(var condition:conditions){
            if(condition instanceof ItemStackEntityRevertPredicate.HeightPredicate heightPredicate){
                var temp = new JsonObject();
                if(heightPredicate.getType() == ItemStackEntityRevertPredicate.HeightPredicate.Type.GREATER)
                    temp.addProperty("type","greater");
                else temp.addProperty("type","less");
                temp.addProperty("content",heightPredicate.getLimit());
                ret.add("height",temp);
            } else {
                var condition1 = (ItemStackEntityRevertPredicate.BiomePredicate) condition;
                var temp = new JsonObject();
                if(condition1.getType() == ItemStackEntityRevertPredicate.BiomePredicate.Type.ALLOWLIST)
                    temp.addProperty("type","allowlist");
                else temp.addProperty("type","blocklist");
                var temp2 = new JsonArray();
                for(var biome : condition1.getBiomeList()){
                    temp2.add(biome.getRegistryName().toString());
                }
                temp.add("content",temp2);
            }
        }
        return new Dynamic<>(JsonOps.INSTANCE,ret);
    });

    static Codec<ItemStackEntityRevertPredicate> PREDICATE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SITUATION_HANDLER_CODEC.fieldOf("detail").forGetter(ItemStackEntityRevertPredicate::getHandler),
            PREDICATE_CONDITIONS_CODEC.optionalFieldOf("condition", new ArrayList<>()).forGetter(ItemStackEntityRevertPredicate::getConditions),
            Codec.INT.fieldOf("weight").forGetter(ItemStackEntityRevertPredicate::getWeight)).apply(instance, ItemStackEntityRevertPredicate::new));


    public static Codec<ItemStackEntityRevertRecipe> recipeCodec(@Nullable ResourceLocation resourceLocation){
        return RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.optionalFieldOf("id",resourceLocation).forGetter(ItemStackEntityRevertRecipe::getId),
                PREDICATE_CODEC.listOf().fieldOf("situations").forGetter(ItemStackEntityRevertRecipe::getItemStackEntityRevertPredicates),
                INGREDIENT_CODEC.fieldOf("cause").forGetter(ItemStackEntityRevertRecipe::getIngredient),
                Codec.INT.fieldOf("lower_bound").forGetter(ItemStackEntityRevertRecipe::getLowerBound),
                Codec.INT.fieldOf("upper_bound").forGetter(ItemStackEntityRevertRecipe::getUpperBound)
        ).apply(instance, ItemStackEntityRevertRecipe::new));
    }

}
