package love.marblegate.boomood.item;

import love.marblegate.boomood.mechanism.noisolpxe.NoisolpxeSituationFactory;
import love.marblegate.boomood.misc.MiscUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

public class TriggerItem extends Item {
    public TriggerItem() {
        super(new Properties().tab(CreativeModeTab.TAB_COMBAT));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand interactionHand) {
        if (!level.isClientSide()) {
            var groundZero = MiscUtils.findLookAt(player);
            var area = new AABB(groundZero).expandTowards(3, 3, 3).expandTowards(-3, -1, -3);
            // Extinguish fire
            NoisolpxeSituationFactory.fireBurnRevert(area)
                    .revert(level, groundZero, player);
            // Remove non-source liquid & Water
            NoisolpxeSituationFactory.fluidFlowRevert(area)
                    .revert(level, groundZero, player);
            // Revert item Drop
            NoisolpxeSituationFactory.itemDropRevert(level, groundZero, area)
                    .merge()
                    .revert(level, groundZero, player);

        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(interactionHand), level.isClientSide());
    }


}
