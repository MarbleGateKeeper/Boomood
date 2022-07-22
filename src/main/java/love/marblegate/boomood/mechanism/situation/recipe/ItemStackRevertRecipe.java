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

public class ItemStackRevertRecipe implements Recipe<Container> {

    public static Codec<ItemStackRevertRecipe> code(@Nullable ResourceLocation resourceLocation){
        return RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.optionalFieldOf("id",resourceLocation).forGetter(ItemStackRevertRecipe::getId),
                ItemStackRevertPredicate.CODEC.listOf().fieldOf("situations").forGetter(ItemStackRevertRecipe::getItemStackEntityRevertPredicates),
               IngredientBox.CODEC.listOf().fieldOf("causes").forGetter(ItemStackRevertRecipe::getIngredientBoxs)
        ).apply(instance, ItemStackRevertRecipe::new));
    }
    private final ResourceLocation id;
    private final List<ItemStackRevertPredicate> itemStackRevertPredicates;
    private final List<IngredientBox> ingredientBoxes;


    public ItemStackRevertRecipe(ResourceLocation id, List<ItemStackRevertPredicate> itemStackRevertPredicates, List<IngredientBox> ingredientBoxes) {
        this.id = id;
        this.itemStackRevertPredicates = itemStackRevertPredicates;
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
    public Optional<ItemStackDropSituationHandler> produceSituationHandler(LevelAccessor level, BlockPos blockPos) {
        List<ItemStackRevertPredicate> qualifiedList = itemStackRevertPredicates.stream()
                .filter(itemStackRevertPredicate -> itemStackRevertPredicate.valid(level, blockPos)).toList();
        if (qualifiedList.isEmpty()) {
            return Optional.empty();
        } else {
            int totalWeight = qualifiedList.stream().map(ItemStackRevertPredicate::getWeight).reduce(0, Integer::sum);
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

    public List<ItemStackRevertPredicate> getItemStackEntityRevertPredicates() {
        return itemStackRevertPredicates;
    }

    public List<IngredientBox> getIngredientBoxs() {
        return ingredientBoxes;
    }

    public static class Serializer extends ForgeRegistryEntry<RecipeSerializer<?>> implements RecipeSerializer<ItemStackRevertRecipe> {

        @Override
        public ItemStackRevertRecipe fromJson(ResourceLocation resourceLocation, JsonObject jsonObject) {
            var a = code(resourceLocation).parse(JsonOps.INSTANCE,jsonObject);
            if(a.get().right().isPresent()){
                throw new JsonSyntaxException(a.get().right().get().message());
            }
            return a.get().left().get();
        }

        @Nullable
        @Override
        public ItemStackRevertRecipe fromNetwork(ResourceLocation resourceLocation, FriendlyByteBuf packetBuffer) {
            return packetBuffer.readWithCodec(code(resourceLocation));
        }

        @Override
        public void toNetwork(FriendlyByteBuf packetBuffer, ItemStackRevertRecipe itemStackRevertRecipe) {
            packetBuffer.writeWithCodec(code(null), itemStackRevertRecipe);
        }
    }
}
