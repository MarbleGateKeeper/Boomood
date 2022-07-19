package love.marblegate.boomood.misc;

import love.marblegate.boomood.config.Configuration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class MiscUtils {
    public static BlockPos findLookAt(Player player) {
        HitResult hitResult = player.pick(128.0D, 0.0F, false);
        return ((BlockHitResult) hitResult).getBlockPos();
    }

    public static AABB createScanningArea(BlockPos blockPos){
        return new AABB(blockPos)
                .expandTowards(Configuration.NOISOLPXE_HORIZONTAL_RADIUS.get(), Configuration.NOISOLPXE_SUGGESTED_VERTICAL_HEIGHT.get(), Configuration.NOISOLPXE_HORIZONTAL_RADIUS.get())
                .expandTowards(-Configuration.NOISOLPXE_HORIZONTAL_RADIUS.get(), -Configuration.NOISOLPXE_SUGGESTED_VERTICAL_HEIGHT.get(), -Configuration.NOISOLPXE_HORIZONTAL_RADIUS.get());
    }
}
