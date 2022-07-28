package love.marblegate.boomood.mechanism.itemstackreversion.cases;

import love.marblegate.boomood.Boomood;
import love.marblegate.boomood.mechanism.itemstackreversion.dataholder.AvailableBlockPosHolder;
import love.marblegate.boomood.mechanism.itemstackreversion.dataholder.BlockInfoHolder;
import love.marblegate.boomood.mechanism.itemstackreversion.dataholder.IntermediateResultHolder;
import love.marblegate.boomood.mechanism.itemstackreversion.result.BlockDestructionSituationResult;
import love.marblegate.boomood.misc.MiscUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public class BlockDestructionReversionCase implements ReversionCase {
    private final List<BlockInfoHolder> blockInfos = new ArrayList<>();

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public void add(IntermediateResultHolder pack) {
        blockInfos.addAll(((BlockDestructionSituationResult) pack.result()).getBlockInfoHolders());
    }

    @Override
    public void revert(Entity manipulator, AvailableBlockPosHolder blockPosHolder) {
        Boomood.LOGGER.debug("Reverting BlockDestruction. Details: " + this);
        var level = manipulator.level;
        for(var blockInfo:blockInfos){
            for(int i=0;i<blockInfo.count();i++){
                var optional = blockPosHolder.next();
                if (optional.isEmpty()) return;
                var destination = optional.get();
                // Falling block should have support block
                if (blockInfo.blockState().getBlock() instanceof FallingBlock && level.getBlockState(destination.below()).is(Blocks.AIR)) {
                    level.setBlockAndUpdate(destination.below(), MiscUtils.getSupportBlock(level, destination.below()));
                }
                level.setBlockAndUpdate(destination, blockInfo.blockState());
                if (blockInfo.tags() != null) {
                    BlockEntity blockentity = level.getBlockEntity(destination);
                    if (blockentity != null) {
                        blockentity.load(blockInfo.tags());
                    }
                }
                // TODO add custom particle effect for indication & add implement explosion particle
            }
        }
    }

    @Override
    public String toString() {
        return "BlockDestructionReversionCase{" +
                "blockInfos=" + blockInfos +
                '}';
    }
}
