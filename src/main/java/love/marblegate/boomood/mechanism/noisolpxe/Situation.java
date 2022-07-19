package love.marblegate.boomood.mechanism.noisolpxe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

interface NoisolpxeSituation {
    void revert(Level level, BlockPos blockPos, Player manipulator);
}
