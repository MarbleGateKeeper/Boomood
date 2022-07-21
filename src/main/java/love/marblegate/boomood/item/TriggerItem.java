package love.marblegate.boomood.item;

import love.marblegate.boomood.mechanism.situation.SituationFactory;
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
            SituationFactory.fireBurnRevert(area)
                    .revert(level, groundZero, player);
            // Remove non-source liquid & Water
            SituationFactory.fluidFlowRevert(area)
                    .revert(level, groundZero, player);
            // Revert item Drop
            SituationFactory.itemDropRevert(level, groundZero, area)
                    .merge()
                    .sort()
                    .revert(level, groundZero, player);

        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(interactionHand), level.isClientSide());
    }


}
