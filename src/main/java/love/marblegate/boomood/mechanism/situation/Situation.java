package love.marblegate.boomood.mechanism.situation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

interface Situation {
    void revert(Level level, BlockPos blockPos, Player manipulator);
}
