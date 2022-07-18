package love.marblegate.boomood.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import love.marblegate.boomood.registry.RecipeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
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

public class NoisolpxeRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final List<NoisolpxePredicate> noisolpxePredicates;
    private final Ingredient ingredient;
    private final int lowerBound;
    private final int upperBound;

    NoisolpxeRecipe(ResourceLocation id, List<NoisolpxePredicate> noisolpxePredicates, Ingredient ingredient, int lowerBound, int upperBound) {
        this.id = id;
        this.noisolpxePredicates = noisolpxePredicates;
        this.ingredient = ingredient;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    @Override
    public boolean matches(@NotNull Container container, @NotNull Level level) {
        var count = 0;
        for(int i=0;i<container.getContainerSize();i++){
            var item = container.getItem(i);
            if(ingredient.test(item)){
                var size = item.getCount();
                if(size<lowerBound-count){
                    count+=size;
                    if(count>=lowerBound) return true;
                }else{
                    return true;
                }
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
        return RecipeRegistry.RECIPE_SERIALIZER_NOISOLPXE.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeRegistry.RECIPE_TYPE_NOISOLPXE.get();
    }

    // Method for exporting result
    public Optional<NoisolpxeSituation.Handler> produceSituationHandler(LevelAccessor level, BlockPos blockPos) {
        List<NoisolpxePredicate> qualifiedList = noisolpxePredicates.stream()
                .filter(noisolpxePredicate -> noisolpxePredicate.valid(level, blockPos)).toList();
        if(qualifiedList.isEmpty()) {
            return Optional.empty();
        } else {
            int totalWeight = qualifiedList.stream().map(NoisolpxePredicate::getWeight).reduce(0,Integer::sum);
            int idx = 0;
            for (double r = Math.random() * totalWeight; idx < qualifiedList.size() - 1; ++idx) {
                r -= qualifiedList.get(idx).getWeight();
                if (r <= 0.0) break;
            }
            return Optional.of(qualifiedList.get(idx).getHandler());
        }
    }

    // This method do not modify ItemStack in Container. It uses copy().
    // All consumed ItemStacks are returned.
    public List<ItemStack> consumeItemAfterProduceSituationHandler(Container container){
        var rc = upperBound==lowerBound? lowerBound: lowerBound + new Random().nextInt(upperBound-lowerBound) + 1;
        List<ItemStack> ret = new ArrayList<>();
        for(int i=0;i<container.getContainerSize();i++){
            var item = container.getItem(i);
            if(ingredient.test(item)){
                var s = item.getCount();
                if(s<=rc){
                    ret.add(item);
                    container.setItem(i,ItemStack.EMPTY);
                    rc-=s;
                    if(rc==0) break;
                }else{
                    var si = item.copy();
                    si.setCount(s-rc);
                    container.setItem(i,si);
                    item.setCount(rc);
                    ret.add(item);
                    break;
                }
            }
        }
        return ret;
    }

    public static class Serializer extends ForgeRegistryEntry<RecipeSerializer<?>> implements RecipeSerializer<NoisolpxeRecipe> {

        @Override
        public NoisolpxeRecipe fromJson(ResourceLocation resourceLocation, JsonObject jsonObject) {
            var ingredient = Ingredient.fromJson(GsonHelper.getAsJsonObject(jsonObject,"cause"));
            var lowerBound = GsonHelper.getAsInt(jsonObject,"lower_bound");
            if(lowerBound<=0) throw new JsonSyntaxException("invalid lowerBound in" + resourceLocation.getNamespace() + ":" + resourceLocation.getPath());
            var upperBound = GsonHelper.getAsInt(jsonObject,"upper_bound");
            if(upperBound<=0 || upperBound<lowerBound) throw new JsonSyntaxException("invalid upperBound in" + resourceLocation.getNamespace() + ":" + resourceLocation.getPath());
            List<NoisolpxePredicate> noisolpxePredicates = new ArrayList<>();
            for (var jsonElement : GsonHelper.getAsJsonArray(jsonObject, "situations")){
                var np = new NoisolpxePredicate(jsonElement.getAsJsonObject());
                noisolpxePredicates.add(np);
            }
            return new NoisolpxeRecipe(resourceLocation, noisolpxePredicates, ingredient, lowerBound, upperBound);
        }

        @Nullable
        @Override
        public NoisolpxeRecipe fromNetwork(ResourceLocation resourceLocation, FriendlyByteBuf packetBuffer) {
            var lowerBound = packetBuffer.readInt();
            var upperBound = packetBuffer.readInt();
            var ingredient = Ingredient.fromNetwork(packetBuffer);
            var size = packetBuffer.readInt();
            List<NoisolpxePredicate> noisolpxePredicates = new ArrayList<>();
            for (int i = 0;i<size;i++){
                noisolpxePredicates.add(NoisolpxePredicate.fromNetwork(packetBuffer));
            }
            return new NoisolpxeRecipe(resourceLocation, noisolpxePredicates, ingredient, lowerBound, upperBound);
        }

        @Override
        public void toNetwork(FriendlyByteBuf packetBuffer, NoisolpxeRecipe noisolpxeRecipe) {
            packetBuffer.writeInt(noisolpxeRecipe.lowerBound);
            packetBuffer.writeInt(noisolpxeRecipe.upperBound);
            noisolpxeRecipe.ingredient.toNetwork(packetBuffer);
            packetBuffer.writeInt(noisolpxeRecipe.noisolpxePredicates.size());
            for(var np:noisolpxeRecipe.noisolpxePredicates){
                np.toNetwork(packetBuffer);
            }
        }
    }
}
