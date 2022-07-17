package love.marblegate.boomood.recipe;

import com.google.gson.JsonObject;
import love.marblegate.boomood.registry.RecipeRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.List;

public class NoisolpxeRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final List<NoisolpxeSituation> situations;

    public NoisolpxeRecipe(ResourceLocation id, List<NoisolpxeSituation> situations) {
        this.id = id;
        this.situations = situations;
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
    public List<NoisolpxeSituation> getSituations() {
        return situations;
    }

    public static class Serializer extends ForgeRegistryEntry<RecipeSerializer<?>> implements RecipeSerializer<NoisolpxeRecipe> {

        @Override
        public NoisolpxeRecipe fromJson(ResourceLocation resourceLocation, JsonObject jsonObject) {
            return null;
            // TODO
        }

        @Nullable
        @Override
        public NoisolpxeRecipe fromNetwork(ResourceLocation resourceLocation, FriendlyByteBuf packetBuffer) {
            return null;
            // TODO
        }

        @Override
        public void toNetwork(FriendlyByteBuf packetBuffer, NoisolpxeRecipe noisolpxeRecipe) {

        }
    }
}
