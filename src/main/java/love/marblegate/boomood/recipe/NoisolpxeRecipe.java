package love.marblegate.boomood.recipe;

import com.google.gson.JsonObject;
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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

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
    public boolean matches(Container container, Level level) {
        return false;
    }

    // Returns an Item that is the result of this recipe
    // Here we do not use this method for exporting result. So EMPTY is returned.
    @Override
    public ItemStack assemble(Container container) {
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
    public NoisolpxeSituation getValidSituation(LevelAccessor level, BlockPos blockPos, ItemStack itemStack) {
        return null; //TODO
    }

    public static class Serializer extends ForgeRegistryEntry<RecipeSerializer<?>> implements RecipeSerializer<NoisolpxeRecipe> {

        @Override
        public NoisolpxeRecipe fromJson(ResourceLocation resourceLocation, JsonObject jsonObject) {
            var ingredient = Ingredient.fromJson(GsonHelper.getAsJsonObject(jsonObject,"cause"));
            var lowerBound = GsonHelper.getAsInt(jsonObject,"lower_bound");
            var upperBound = GsonHelper.getAsInt(jsonObject,"upper_bound");
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
