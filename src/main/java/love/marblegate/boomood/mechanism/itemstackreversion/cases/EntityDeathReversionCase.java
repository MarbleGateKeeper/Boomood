package love.marblegate.boomood.mechanism.itemstackreversion.cases;

import love.marblegate.boomood.config.Configuration;
import love.marblegate.boomood.mechanism.itemstackreversion.dataholder.ResultPack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

public class EntityDeathReversionCase implements ReversionCase {
    //TODO
    private final EntityType<?> entityType = null;

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public void add(ResultPack pack) {
        if(Configuration.DEBUG_MODE.get()){
            // TODO need to String
            System.out.println("Reverting ItemFrameDestruction. Details: " + this);
        }
        // TODO
    }

    @Override
    public void revert(Player manipulator, BlockPos blockPos) {
        // TODO
    }
}
