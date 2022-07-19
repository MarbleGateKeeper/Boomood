package love.marblegate.boomood.registry;

import love.marblegate.boomood.mechanism.noisolpxe.NoisolpxeItemStackEntityRevertRecipe;
import net.minecraft.core.Registry;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class RecipeRegistry {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registry.RECIPE_TYPE_REGISTRY, "boomood");

    public static final RegistryObject<RecipeType<NoisolpxeItemStackEntityRevertRecipe>> TYPE_NOISOLPXE_ITEMSTACK_REVERT = RECIPE_TYPES.register("noisolpxe_itemstack_revert", () -> new RecipeType<>() {
        public String toString() {
            return "noisolpxe_itemstack_revert";
        }
    });

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, "boomood");
    public static final RegistryObject<RecipeSerializer<NoisolpxeItemStackEntityRevertRecipe>> SERIALIZER_NOISOLPXE_ITEMSTACK_REVERT = RECIPE_SERIALIZERS.register("noisolpxe_itemstack_revert", NoisolpxeItemStackEntityRevertRecipe.Serializer::new);
}
