package love.marblegate.boomood.mechanism;

import com.google.common.collect.Lists;
import love.marblegate.boomood.mechanism.firereversion.FireBurnSituation;
import love.marblegate.boomood.mechanism.fluidreversion.FluidFlowSituation;
import love.marblegate.boomood.mechanism.itemstackreversion.ItemStackDropSituation;
import love.marblegate.boomood.mechanism.itemstackreversion.handler.ItemStackRevertHandler;
import love.marblegate.boomood.mechanism.itemstackreversion.ItemStackRevertRecipe;
import love.marblegate.boomood.registry.RecipeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.Predicate;

public class SituationFactory {

    public static ItemStackDropSituation.SituationSet.Raw itemDropRevert(Level level, BlockPos eventCenter, AABB area) {
        var itemStackEntities = level.getEntities((Entity) null, area, entity -> entity instanceof ItemEntity);
        var container = new SimpleContainer(9);
        ItemStackDropSituation.SituationSet.Raw ret = new ItemStackDropSituation.SituationSet.Raw();
        if (!itemStackEntities.isEmpty()) {
            for (var itemStackEntity : itemStackEntities) {
                var item = ((ItemEntity) itemStackEntity).getItem();
                if (container.canAddItem(item)) {
                    container.addItem(item);
                } else {
                    List<ItemStack> oldItems = container.removeAllItems();
                    container = new SimpleContainer(container.getContainerSize() * 2);
                    for (var oldItem : oldItems) {
                        container.addItem(oldItem);
                    }
                    container.addItem(item);
                }
                itemStackEntity.remove(Entity.RemovalReason.DISCARDED);
            }
        }
        while (!container.isEmpty()) {
            ItemStackRevertRecipe recipe = level.getRecipeManager().getRecipeFor(RecipeRegistry.REVERTING_FROM_ITEM.get(), container, level).orElse(null);
            if (recipe != null) {
                var handler = recipe.produceSituationHandler(level, eventCenter);
                var consumedItems = recipe.consumeItemAfterProduceSituationHandler(container);
                if (handler.isEmpty()) {
                    handleRemainingItems(consumedItems, ret);
                } else {
                    ret.add(ItemStackDropSituation.create(handler.get(), consumedItems));
                }
            } else {
                var leftItems = container.removeAllItems().stream().filter(itemStack -> !itemStack.isEmpty()).toList();
                handleRemainingItems(leftItems, ret);
            }
        }
        return ret;
    }

    public static FluidFlowSituation fluidFlowRevert(Predicate<Fluid> revertNonSource, Predicate<Fluid> revertAll, AABB area) {
        return new FluidFlowSituation(revertNonSource, revertAll, area);
    }

    public static FluidFlowSituation fluidFlowRevert(AABB area) {
        return fluidFlowRevert(null, null, area);
    }

    public static FireBurnSituation fireBurnRevert(Predicate<Block> isFire, boolean tryFixStructure, AABB area) {
        return new FireBurnSituation(isFire, tryFixStructure, area);
    }

    public static FireBurnSituation fireBurnRevert(AABB area) {
        return fireBurnRevert(null, false, area);
    }

    private static void handleRemainingItems(List<ItemStack> items, List<ItemStackDropSituation> destinationList) {
        var armorConsumed = items.stream().filter(itemStack -> itemStack.getItem() instanceof ArmorItem).toList();
        if (!armorConsumed.isEmpty())
            destinationList.add(ItemStackDropSituation.create(ItemStackRevertHandler.createArmorHandler(), Lists.newArrayList(armorConsumed)));
        var commonConsumed = items.stream().filter(itemStack -> !(itemStack.getItem() instanceof ArmorItem)).toList();
        if (!commonConsumed.isEmpty()){
            for(var item:commonConsumed){
                destinationList.add(ItemStackDropSituation.create(ItemStackRevertHandler.createDefaultHandler(), Lists.newArrayList(item)));
            }
        }
    }

}
