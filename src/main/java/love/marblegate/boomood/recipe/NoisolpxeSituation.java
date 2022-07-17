package love.marblegate.boomood.recipe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;

import java.util.List;

public interface NoisolpxeSituation {
    void revert(LevelAccessor level, BlockPos blockPos,  List<ItemStack> items, Player manipulator);
}
