package love.marblegate.boomood.item;

import love.marblegate.boomood.mechanism.ReversionFactory;
import love.marblegate.boomood.misc.MiscUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class TriggerItem extends Item {
    public TriggerItem() {
        super(new Properties().tab(CreativeModeTab.TAB_COMBAT));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand interactionHand) {
        if (!level.isClientSide()) {
            var groundZero = MiscUtils.findLookAt(player);
            var area = MiscUtils.createScanningArea(groundZero);
            // Extinguish fire
            ReversionFactory.fireBurnRevert(area).revert(player ,groundZero);
            // Remove non-source liquid & Water
            ReversionFactory.fluidFlowRevert(area).revert(player ,groundZero);
            // Revert item Drop
            ReversionFactory.itemDropRevert(level, groundZero, area).revert(player ,groundZero);

        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(interactionHand), level.isClientSide());
    }


}
