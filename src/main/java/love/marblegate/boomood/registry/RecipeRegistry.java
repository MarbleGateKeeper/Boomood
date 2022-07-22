package love.marblegate.boomood.registry;

import love.marblegate.boomood.mechanism.situation.recipe.ItemStackRevertRecipe;
import net.minecraft.core.Registry;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class RecipeRegistry {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registry.RECIPE_TYPE_REGISTRY, "boomood");

    public static final RegistryObject<RecipeType<ItemStackRevertRecipe>> REVERTING_FROM_ITEM = RECIPE_TYPES.register("reverting_from_item", () -> new RecipeType<>() {
        public String toString() {
            return "reverting_from_item";
        }
    });

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, "boomood");
    public static final RegistryObject<RecipeSerializer<ItemStackRevertRecipe>> REVERTING_FROM_ITEM_SERIALIZER = RECIPE_SERIALIZERS.register("reverting_from_item", ItemStackRevertRecipe.Serializer::new);
}
