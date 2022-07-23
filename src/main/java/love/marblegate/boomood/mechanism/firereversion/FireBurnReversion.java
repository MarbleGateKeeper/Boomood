package love.marblegate.boomood.mechanism.firereversion;

import love.marblegate.boomood.mechanism.Reversion;
import love.marblegate.boomood.misc.ServerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.function.Predicate;

public class FireBurnReversion extends Reversion {

    private final boolean tryFixStructure;
    private final Predicate<Block> isFire;
    private final AABB area;

    private static final Predicate<Block> DEFAULT_FIRE = block -> block == Blocks.FIRE || block == Blocks.SOUL_FIRE;

    public FireBurnReversion(@Nullable Predicate<Block> isFire, boolean tryFixStructure, AABB area) {
        this.area = area;
        if (isFire == null) this.isFire = DEFAULT_FIRE;
        else this.isFire = isFire;
        this.tryFixStructure = tryFixStructure;
    }

    @Override
    public void revert(Player manipulator, BlockPos blockPos) {
        if (tryFixStructure) {
            //TODO
        } else {
            var blockPosStream = BlockPos.betweenClosedStream(area);
            var level = manipulator.level;
            blockPosStream.filter(bp -> {
                var bs = level.getBlockState(bp);
                return isFire.test(bs.getBlock());
            }).forEach(bp -> {
                level.setBlockAndUpdate(bp, Blocks.AIR.defaultBlockState());
                ServerUtils.addParticle(manipulator.level, ParticleTypes.LARGE_SMOKE, bp, 0, 3, 0, 0.01, 20);
            });
        }
    }
}
