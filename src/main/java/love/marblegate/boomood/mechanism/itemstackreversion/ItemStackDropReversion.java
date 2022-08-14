package love.marblegate.boomood.mechanism.itemstackreversion;

import com.google.common.collect.Lists;
import love.marblegate.boomood.Boomood;
import love.marblegate.boomood.mechanism.Reversion;
import love.marblegate.boomood.mechanism.itemstackreversion.dataholder.AvailableBlockPosHolder;
import love.marblegate.boomood.mechanism.itemstackreversion.dataholder.IntermediateResultHolder;
import love.marblegate.boomood.mechanism.itemstackreversion.dataholder.ReversionCaseHolder;
import love.marblegate.boomood.mechanism.itemstackreversion.result.ReversionSituationResult;
import love.marblegate.boomood.registry.RecipeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public class ItemStackDropReversion extends Reversion {

    private final ReversionCaseHolder caseHolder;

    public ItemStackDropReversion(Level level, BlockPos eventCenter, AABB area) {
        Boomood.LOGGER.debug("ItemStackDropReversion is initializing at " + area + ". Reversion center is " + eventCenter);
        var itemStackEntities = level.getEntities((Entity) null, area, entity -> entity instanceof ItemEntity);
        var container = new SimpleContainer(9);
        List<IntermediateResultHolder> intermediateResultHolders = new ArrayList<>();
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
            ItemStackReversionRecipe recipe = level.getRecipeManager().getRecipeFor(RecipeRegistry.REVERTING_FROM_ITEM.get(), container, level).orElse(null);
            if (recipe != null) {
                var caseOptional = recipe.produceSituationHandler(level, eventCenter);
                var consumedItems = recipe.consumeItemAfterProduceSituationHandler(container);
                if (caseOptional.isEmpty()) {
                    handleRemainingItems(consumedItems, intermediateResultHolders);
                } else {
                    intermediateResultHolders.add(new IntermediateResultHolder(caseOptional.get(), consumedItems));
                }
            } else {
                var leftItems = container.removeAllItems().stream().filter(itemStack -> !itemStack.isEmpty()).toList();
                handleRemainingItems(leftItems, intermediateResultHolders);
            }
        }
        caseHolder = new ReversionCaseHolder();
        intermediateResultHolders.forEach(resultPack -> {
            caseHolder.add(resultPack);
            Boomood.LOGGER.debug("IntermediateResultHolder " + resultPack + " has been add into ReversionCaseHolder");
        });
    }

    private static void handleRemainingItems(List<ItemStack> items, List<IntermediateResultHolder> intermediateResultHolders) {
        var armorConsumed = items.stream().filter(itemStack -> itemStack.getItem() instanceof ArmorItem).toList();
        if (!armorConsumed.isEmpty())
            intermediateResultHolders.add(new IntermediateResultHolder(ReversionSituationResult.createArmorHandler(), Lists.newArrayList(armorConsumed)));
        var commonConsumed = items.stream().filter(itemStack -> !(itemStack.getItem() instanceof ArmorItem)).toList();
        if (!commonConsumed.isEmpty()) {
            for (var item : commonConsumed) {
                intermediateResultHolders.add(new IntermediateResultHolder(ReversionSituationResult.createDefaultHandler(), Lists.newArrayList(item)));
            }
        }
    }

    @Override
    public void revert(Entity manipulator, BlockPos blockPos) {
        Boomood.LOGGER.debug("ItemStackDropReversion is in reverting progress.");
        // TODO
        caseHolder.apply(manipulator, new AvailableBlockPosHolder(manipulator.level, blockPos));
    }

}
