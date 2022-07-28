package love.marblegate.boomood.mechanism.itemstackreversion.cases;

import love.marblegate.boomood.mechanism.itemstackreversion.dataholder.AvailableBlockPosHolder;
import love.marblegate.boomood.mechanism.itemstackreversion.dataholder.IntermediateResultHolder;
import net.minecraft.world.entity.Entity;


public interface ReversionCase {

    int priority();

    void add(IntermediateResultHolder pack);

    void revert(Entity manipulator, AvailableBlockPosHolder blockPosHolder);
}
