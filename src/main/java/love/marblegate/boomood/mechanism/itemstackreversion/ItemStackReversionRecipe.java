package love.marblegate.boomood.mechanism.itemstackreversion;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import love.marblegate.boomood.mechanism.itemstackreversion.result.ReversionSituationResult;
import love.marblegate.boomood.mechanism.itemstackreversion.dataholder.IngredientBox;
import love.marblegate.boomood.registry.RecipeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.registries.ForgeRegistryEntry;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class ItemStackReversionRecipe implements Recipe<Container> {

    public static Codec<ItemStackReversionRecipe> code(@Nullable ResourceLocation resourceLocation){
        return Codec.PASSTHROUGH.comapFlatMap(dynamic -> {
            var json = dynamic.convert(JsonOps.INSTANCE).getValue().getAsJsonObject();
            ResourceLocation rl;
            if(json.has("id"))
                rl = ResourceLocation.CODEC.parse(JsonOps.INSTANCE,json.getAsJsonPrimitive("id")).getOrThrow(false,err->{
                    throw new JsonSyntaxException(err);
                });
            else rl = resourceLocation;
            List<IngredientBox> boxes;
            if(json.has("cause")){
                boxes = new ArrayList<>();
                try{
                    var j = json.getAsJsonObject("cause");
                    boxes.add(IngredientBox.CODEC.parse(JsonOps.INSTANCE,j).getOrThrow(false,err->{
                        throw new JsonSyntaxException(err);
                    }));
                } catch(ClassCastException e) {
                    throw new JsonSyntaxException("\"cause\" in recipe json id"+ resourceLocation +" must be a JsonObject to represent an ingredient.");
                }

            } else if (json.has("causes")) {
                try{
                    var j = json.getAsJsonArray("causes");
                    boxes = IngredientBox.CODEC.listOf().parse(JsonOps.INSTANCE,j).getOrThrow(false,err->{
                        throw new JsonSyntaxException(err);});
                } catch(ClassCastException e) {
                    throw new JsonSyntaxException("\"causes\" in recipe json id"+ resourceLocation +" must be a JsonArray to represent ingredients.");
                }
            } else {
                return DataResult.error("Recipe json must have either cause or causes.");
            }
            List<ReversionSituation> situations;
            if(json.has("situation")){
                try{
                    var j = json.getAsJsonObject("situation");
                    situations = new ArrayList<>();
                    situations.add(ReversionSituation.CODEC.parse(JsonOps.INSTANCE,j).getOrThrow(false, err->{
                        throw new JsonSyntaxException(err);
                    }));
                } catch(ClassCastException e) {
                    throw new JsonSyntaxException("\"situation\" in recipe json id"+ resourceLocation +" must be a JsonObject to represent a situation.");
                }
            } else if (json.has("situations")) {
                try{
                    var j = json.getAsJsonArray("situations");
                    situations = ReversionSituation.CODEC.listOf().parse(JsonOps.INSTANCE,j).getOrThrow(false, err->{
                        throw new JsonSyntaxException(err);
                    });
                } catch(ClassCastException e) {
                    throw new JsonSyntaxException("\"situations\" in recipe json id"+ resourceLocation +" must be a JsonArray to represent situations.");
                }

            } else {
                return DataResult.error("Recipe json must have either situation or situations.");
            }
            return DataResult.success(new ItemStackReversionRecipe(rl,situations,boxes));
        },recipe->{
            var result = new JsonObject();
            result.add("id", ResourceLocation.CODEC.encodeStart(JsonOps.INSTANCE,recipe.id).getOrThrow(false, err->{}));
            if(recipe.ingredientBoxes.size()==1)
                result.add("cause", IngredientBox.CODEC.encodeStart(JsonOps.INSTANCE,recipe.ingredientBoxes.get(0)).getOrThrow(false, err->{}));
            else result.add("causes", IngredientBox.CODEC.listOf().encodeStart(JsonOps.INSTANCE,recipe.ingredientBoxes).getOrThrow(false, err->{}));
            if(recipe.predicates.size()==1)
                result.add("situation", ReversionSituation.CODEC.encodeStart(JsonOps.INSTANCE,recipe.predicates.get(0)).getOrThrow(false, err->{}));
            else result.add("situations", ReversionSituation.CODEC.listOf().encodeStart(JsonOps.INSTANCE,recipe.predicates).getOrThrow(false, err->{}));
            return new Dynamic<>(JsonOps.INSTANCE, result);
        });
    }


    private final ResourceLocation id;
    private final List<ReversionSituation> predicates;
    private final List<IngredientBox> ingredientBoxes;


    public ItemStackReversionRecipe(ResourceLocation id, List<ReversionSituation> predicates, List<IngredientBox> ingredientBoxes) {
        this.id = id;
        this.predicates = predicates;
        this.ingredientBoxes = ingredientBoxes;
    }

    @Override
    public boolean matches(@NotNull Container container, @NotNull Level level) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            var item = container.getItem(i);
            for(var ingredientBox: ingredientBoxes){
                if (ingredientBox.test(item)) return true;
            }
        }
        return false;
    }

    // Returns an Item that is the result of this recipe
    // Here we do not use this method for exporting result. So EMPTY is returned.
    @Override
    public ItemStack assemble(@NotNull Container container) {
        return ItemStack.EMPTY;
    }

    // Can Craft at any dimension
    @Override
    public boolean canCraftInDimensions(int i, int i1) {
        return true;
    }

    // Get the result of this recipe, usually for display purposes (e.g. recipe book).
    // Here we do not use this method for exporting result. So EMPTY is returned.
    @Override
    public ItemStack getResultItem() {
        return ItemStack.EMPTY;
    }


    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeRegistry.REVERTING_FROM_ITEM_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeRegistry.REVERTING_FROM_ITEM.get();
    }

    // Method for exporting result
    public Optional<ReversionSituationResult> produceSituationHandler(LevelAccessor level, BlockPos blockPos) {
        List<ReversionSituation> qualifiedList = predicates.stream()
                .filter(reversionSituation -> reversionSituation.valid(level, blockPos)).toList();
        if (qualifiedList.isEmpty()) {
            return Optional.empty();
        } else {
            int totalWeight = qualifiedList.stream().map(ReversionSituation::getWeight).reduce(0, Integer::sum);
            int idx = 0;
            for (double r = Math.random() * totalWeight; idx < qualifiedList.size() - 1; ++idx) {
                r -= qualifiedList.get(idx).getWeight();
                if (r <= 0.0) break;
            }
            return Optional.of(qualifiedList.get(idx).getReversionCase());
        }
    }

    // This method do not modify ItemStack in Container. It uses copy().
    // All consumed ItemStacks are returned.
    public List<ItemStack> consumeItemAfterProduceSituationHandler(Container container) {
        IngredientBox matchedBox = null;
        for (int i = 0; i < container.getContainerSize(); i++) {
            var item = container.getItem(i);
            for(var ingredientBox: ingredientBoxes)
                if (ingredientBox.test(item)) {
                    matchedBox = ingredientBox;
                    break;
                }
            if(matchedBox!=null) break;
        }
        if(matchedBox==null) return new ArrayList<>();
        var rc = matchedBox.max() == matchedBox.min() ? matchedBox.min() : matchedBox.min() + new Random().nextInt(matchedBox.max() - matchedBox.min()) + 1;
        List<ItemStack> ret = new ArrayList<>();
        for (int i = 0; i < container.getContainerSize(); i++) {
            var item = container.getItem(i);
            if (matchedBox.test(item)) {
                var s = item.getCount();
                if (s <= rc) {
                    ret.add(item);
                    container.setItem(i, ItemStack.EMPTY);
                    rc -= s;
                    if (rc == 0) break;
                } else {
                    var si = item.copy();
                    si.setCount(s - rc);
                    container.setItem(i, si);
                    item.setCount(rc);
                    ret.add(item);
                    break;
                }
            }
        }
        return ret;
    }

    public List<ReversionSituation> getItemStackEntityRevertPredicates() {
        return predicates;
    }

    public List<IngredientBox> getIngredientBoxs() {
        return ingredientBoxes;
    }

    public static class Serializer extends ForgeRegistryEntry<RecipeSerializer<?>> implements RecipeSerializer<ItemStackReversionRecipe> {

        @Override
        public ItemStackReversionRecipe fromJson(ResourceLocation resourceLocation, JsonObject jsonObject) {
            var a = code(resourceLocation).parse(JsonOps.INSTANCE,jsonObject);
            if(a.get().right().isPresent()){
                throw new JsonSyntaxException(a.get().right().get().message());
            }
            return a.get().left().get();
        }

        @Nullable
        @Override
        public ItemStackReversionRecipe fromNetwork(ResourceLocation resourceLocation, FriendlyByteBuf packetBuffer) {
            return packetBuffer.readWithCodec(code(resourceLocation));
        }

        @Override
        public void toNetwork(FriendlyByteBuf packetBuffer, ItemStackReversionRecipe itemStackReversionRecipe) {
            packetBuffer.writeWithCodec(code(null), itemStackReversionRecipe);
        }
    }
}
