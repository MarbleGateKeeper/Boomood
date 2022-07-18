package love.marblegate.boomood.item;

import com.google.common.collect.Lists;
import love.marblegate.boomood.misc.MiscUtils;
import love.marblegate.boomood.recipe.NoisolpxeRecipe;
import love.marblegate.boomood.recipe.NoisolpxeSituation;
import love.marblegate.boomood.registry.RecipeRegistry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TriggerItem extends Item {
    public TriggerItem() {
        super(new Properties().tab(CreativeModeTab.TAB_COMBAT));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand interactionHand) {
        if(!level.isClientSide()){
            var blockPos = MiscUtils.findLookAt(player);
            var area = new AABB(blockPos).expandTowards(3,3,3).expandTowards(-3,-1,-3);
            var itemStackEntities = level.getEntities((Entity) null, area, entity -> entity instanceof ItemEntity);
            if(!itemStackEntities.isEmpty()){
                var container = new SimpleContainer(9);
                for(var itemStackEntity: itemStackEntities){
                    var item = ((ItemEntity) itemStackEntity).getItem();
                    if(container.canAddItem(item)){
                        container.addItem(item);
                    } else {
                        List<ItemStack> oldItems = container.removeAllItems();
                        container = new SimpleContainer(container.getContainerSize()*2);
                        for(var oldItem: oldItems){
                            container.addItem(oldItem);
                        }
                        container.addItem(item);
                    }
                }
                List<NoisolpxeSituation> revertList = new ArrayList<>();
                while(!container.isEmpty()){
                    NoisolpxeRecipe recipe = level.getRecipeManager().getRecipeFor(RecipeRegistry.RECIPE_TYPE_NOISOLPXE.get(), container,level).orElse(null);
                    if(recipe!=null){
                        var handler = recipe.produceSituationHandler(level,blockPos);
                        var consumedItems = recipe.consumeItemAfterProduceSituationHandler(container);
                        if(handler.isEmpty()){
                            handleRemainingItems(consumedItems,revertList);
                        }else{
                            revertList.add(NoisolpxeSituation.create(handler.get(),consumedItems));
                        }
                    } else {
                        var leftItems = container.removeAllItems().stream().filter(itemStack -> !itemStack.isEmpty()).toList();
                        handleRemainingItems(leftItems,revertList);
                    }
                }
                System.out.println(revertList);
            }
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(interactionHand),level.isClientSide());
    }

    private void handleRemainingItems(List<ItemStack> items, List<NoisolpxeSituation> destinationList){
        var armorConsumed = items.stream().filter(itemStack -> itemStack.getItem() instanceof ArmorItem).toList();
        if(!armorConsumed.isEmpty()) destinationList.add(NoisolpxeSituation.create(NoisolpxeSituation.defaultArmorHandler(),Lists.newArrayList(armorConsumed)));
        var commonConsumed = items.stream().filter(itemStack -> !(itemStack.getItem() instanceof ArmorItem)).toList();
        if(!commonConsumed.isEmpty()) destinationList.add(NoisolpxeSituation.create(NoisolpxeSituation.defaultHandler(),Lists.newArrayList(commonConsumed)));
    }
}
