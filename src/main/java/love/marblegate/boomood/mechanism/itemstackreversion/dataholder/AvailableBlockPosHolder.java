package love.marblegate.boomood.mechanism.itemstackreversion.dataholder;

import love.marblegate.boomood.config.Configuration;
import love.marblegate.boomood.misc.MiscUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.*;

public class AvailableBlockPosHolder {
    private final Queue<BlockPos> blockPosQueue;

    private final List<BlockPos> availableChestBlockPosList = new ArrayList<>();


    public AvailableBlockPosHolder(Level level, BlockPos blockPos) {
        var a = MiscUtils.createShuffledBlockPosList(MiscUtils.createScanningArea(blockPos));
        var b = MiscUtils.createShuffledBlockPosList(MiscUtils.createRemedyScanningArea(level,blockPos));
        if(Configuration.Common.AREA_SHAPE.get().isSphere()){
            a = a.stream().filter(blockPos1 -> MiscUtils.isWithinReversionArea(blockPos,blockPos1)).toList();
        }
        if(!Configuration.Common.REMEDY_TYPE.get().goUpward() && Configuration.Common.AREA_SHAPE.get().isSphere()){
            b = b.stream().filter(blockPos1 -> MiscUtils.isWithinReversionArea(blockPos,blockPos1,2)).toList();
        }
        var c = a.stream().filter(blockPos1 -> level.getBlockState(blockPos1).isAir()).toList();
        var d = b.stream().filter(blockPos1 -> level.getBlockState(blockPos1).isAir()).toList();
        Set<BlockPos> temp = new LinkedHashSet<>((int) (d.size()*1.5));
        temp.addAll(c);
        temp.addAll(d);
        blockPosQueue = new LinkedList<>(temp);
    }

    public Optional<BlockPos> next(){
        var a = blockPosQueue.poll();
        return Optional.ofNullable(a);
    }

    public void remove(BlockPos blockPos){
        blockPosQueue.remove(blockPos);
    }

    public void appendChestLocation(BlockPos blockPos){
        availableChestBlockPosList.add(blockPos);
    }

    public List<BlockPos> chestLocations(){
        return availableChestBlockPosList;
    }


}
