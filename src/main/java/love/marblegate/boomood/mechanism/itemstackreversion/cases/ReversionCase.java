package love.marblegate.boomood.mechanism.itemstackreversion.cases;

import love.marblegate.boomood.mechanism.itemstackreversion.dataholder.ResultPack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;


public interface ReversionCase {

    int priority();

    void add(ResultPack pack);

    void revert(Player manipulator, BlockPos blockPos);
}
