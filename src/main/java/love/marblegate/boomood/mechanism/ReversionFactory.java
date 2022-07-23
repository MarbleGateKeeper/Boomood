package love.marblegate.boomood.mechanism;

import love.marblegate.boomood.mechanism.firereversion.FireBurnReversion;
import love.marblegate.boomood.mechanism.fluidreversion.FluidFlowReversion;
import love.marblegate.boomood.mechanism.itemstackreversion.ItemStackDropReversion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;

import java.util.function.Predicate;

public class ReversionFactory {

    public static ItemStackDropReversion itemDropRevert(Level level, BlockPos eventCenter, AABB area) {
        return new ItemStackDropReversion(level, eventCenter, area);
    }

    public static FluidFlowReversion fluidFlowRevert(Predicate<Fluid> revertNonSource, Predicate<Fluid> revertAll, AABB area) {
        return new FluidFlowReversion(revertNonSource, revertAll, area);
    }

    public static FluidFlowReversion fluidFlowRevert(AABB area) {
        return fluidFlowRevert(null, null, area);
    }

    public static FireBurnReversion fireBurnRevert(Predicate<Block> isFire, boolean tryFixStructure, AABB area) {
        return new FireBurnReversion(isFire, tryFixStructure, area);
    }

    public static FireBurnReversion fireBurnRevert(AABB area) {
        return fireBurnRevert(null, false, area);
    }

}
