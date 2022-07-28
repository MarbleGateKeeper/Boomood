package love.marblegate.boomood.mechanism.fluidreversion;

import love.marblegate.boomood.mechanism.Reversion;
import love.marblegate.boomood.misc.ServerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.function.Predicate;

public class FluidFlowReversion extends Reversion {

    private final Predicate<Fluid> revertNonSource;
    private final Predicate<Fluid> revertAll;
    private final AABB area;

    private static final Predicate<Fluid> ALL = fluid -> true;
    private static final Predicate<Fluid> NONE = fluid -> false;

    public FluidFlowReversion(@Nullable Predicate<Fluid> revertNonSource, @Nullable Predicate<Fluid> revertAll, AABB area) {
        this.area = area;
        if (revertNonSource == null) this.revertNonSource = ALL;
        else this.revertNonSource = revertNonSource;
        if (revertNonSource == null) this.revertAll = NONE;
        else this.revertAll = revertAll;
    }

    @Override
    public void revert(Entity manipulator, BlockPos blockPos) {
        var blockPosStream = BlockPos.betweenClosedStream(area);
        var level = manipulator.level;
        blockPosStream.filter(bp -> {
            var fs = level.getFluidState(bp);
            return !fs.isEmpty() && ((!fs.isSource() && revertNonSource.test(fs.getType())) || revertAll.test(fs.getType()));
        }).forEach(bp -> {
            var fluidParticle = level.getFluidState(bp).getDripParticle();
            level.setBlockAndUpdate(bp, Blocks.AIR.defaultBlockState());
            if (fluidParticle != null) {
                ServerUtils.addParticle(level, fluidParticle, bp, 0, 3, 0, 1, 10);
            }
        });
    }
}
