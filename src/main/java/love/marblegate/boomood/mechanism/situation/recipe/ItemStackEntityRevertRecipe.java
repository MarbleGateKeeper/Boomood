package love.marblegate.boomood.mechanism.situation.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import love.marblegate.boomood.mechanism.situation.handler.ItemStackDropSituationHandler;
import love.marblegate.boomood.registry.RecipeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
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

public class ItemStackEntityRevertRecipe implements Recipe<Container> {

    public static Codec<ItemStackEntityRevertRecipe> code(@Nullable ResourceLocation resourceLocation){
        return RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.optionalFieldOf("id",resourceLocation).forGetter(ItemStackEntityRevertRecipe::getId),
                ItemStackEntityRevertPredicate.CODEC.listOf().fieldOf("situations").forGetter(ItemStackEntityRevertRecipe::getItemStackEntityRevertPredicates),
                CodecUtils.INGREDIENT_CODEC.fieldOf("cause").forGetter(ItemStackEntityRevertRecipe::getIngredient),
                Codec.INT.fieldOf("lowerBound").forGetter(ItemStackEntityRevertRecipe::getLowerBound),
                Codec.INT.fieldOf("upperBound").forGetter(ItemStackEntityRevertRecipe::getUpperBound)
        ).apply(instance, ItemStackEntityRevertRecipe::new));
    }
    private final ResourceLocation id;
    private final List<ItemStackEntityRevertPredicate> itemStackEntityRevertPredicates;
    private final Ingredient ingredient;
    private final int lowerBound;
    private final int upperBound;

    public ItemStackEntityRevertRecipe(ResourceLocation id, List<ItemStackEntityRevertPredicate> itemStackEntityRevertPredicates, Ingredient ingredient, int lowerBound, int upperBound) {
        this.id = id;
        this.itemStackEntityRevertPredicates = itemStackEntityRevertPredicates;
        this.ingredient = ingredient;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    @Override
    public boolean matches(@NotNull Container container, @NotNull Level level) {
        var count = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            var item = container.getItem(i);
            if (ingredient.test(item)) {
                var size = item.getCount();
                if (size < lowerBound - count) {
                    count += size;
                    if (count >= lowerBound) return true;
                } else {
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
        return RecipeRegistry.SERIALIZER_NOISOLPXE_ITEMSTACK_REVERT.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeRegistry.TYPE_NOISOLPXE_ITEMSTACK_REVERT.get();
    }

    // Method for exporting result
    public Optional<ItemStackDropSituationHandler> produceSituationHandler(LevelAccessor level, BlockPos blockPos) {
        List<ItemStackEntityRevertPredicate> qualifiedList = itemStackEntityRevertPredicates.stream()
                .filter(itemStackEntityRevertPredicate -> itemStackEntityRevertPredicate.valid(level, blockPos)).toList();
        if (qualifiedList.isEmpty()) {
            return Optional.empty();
        } else {
            int totalWeight = qualifiedList.stream().map(ItemStackEntityRevertPredicate::getWeight).reduce(0, Integer::sum);
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
    public List<ItemStack> consumeItemAfterProduceSituationHandler(Container container) {
        var rc = upperBound == lowerBound ? lowerBound : lowerBound + new Random().nextInt(upperBound - lowerBound) + 1;
        List<ItemStack> ret = new ArrayList<>();
        for (int i = 0; i < container.getContainerSize(); i++) {
            var item = container.getItem(i);
            if (ingredient.test(item)) {
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

    public List<ItemStackEntityRevertPredicate> getItemStackEntityRevertPredicates() {
        return itemStackEntityRevertPredicates;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public int getLowerBound() {
        return lowerBound;
    }

    public int getUpperBound() {
        return upperBound;
    }

    public static class Serializer extends ForgeRegistryEntry<RecipeSerializer<?>> implements RecipeSerializer<ItemStackEntityRevertRecipe> {

        @Override
        public ItemStackEntityRevertRecipe fromJson(ResourceLocation resourceLocation, JsonObject jsonObject) {
            var a = code(resourceLocation).parse(JsonOps.INSTANCE,jsonObject);
            if(a.get().right().isPresent()){
                throw new JsonSyntaxException(a.get().right().get().message());
            }
            return a.get().left().get();
        }

        @Nullable
        @Override
        public ItemStackEntityRevertRecipe fromNetwork(ResourceLocation resourceLocation, FriendlyByteBuf packetBuffer) {
            return packetBuffer.readWithCodec(code(resourceLocation));
        }

        @Override
        public void toNetwork(FriendlyByteBuf packetBuffer, ItemStackEntityRevertRecipe itemStackEntityRevertRecipe) {
            packetBuffer.writeWithCodec(code(null),itemStackEntityRevertRecipe);
        }
    }
}
