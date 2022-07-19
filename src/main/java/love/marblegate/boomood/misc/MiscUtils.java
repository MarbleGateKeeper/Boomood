package love.marblegate.boomood.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class MiscUtils {
    public static BlockPos findLookAt(Player player) {
        HitResult hitResult = player.pick(128.0D, 0.0F, false);
        return ((BlockHitResult) hitResult).getBlockPos();
    }
}
