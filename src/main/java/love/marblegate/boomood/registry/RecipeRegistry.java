package love.marblegate.boomood.registry;

import love.marblegate.boomood.recipe.NoisolpxeRecipe;
import net.minecraft.core.Registry;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class RecipeRegistry {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registry.RECIPE_TYPE_REGISTRY, "boomood");

    public static final RegistryObject<RecipeType<NoisolpxeRecipe>> RECIPE_TYPE_NOISOLPXE = RECIPE_TYPES.register("noisolpxe", () -> new RecipeType<>() {
        public String toString() {
            return "noisolpxe";
        }
    });

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, "boomood");
    public static final RegistryObject<RecipeSerializer<NoisolpxeRecipe>> RECIPE_SERIALIZER_NOISOLPXE = RECIPE_SERIALIZERS.register("noisolpxe", NoisolpxeRecipe.Serializer::new);
}
