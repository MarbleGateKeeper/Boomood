package love.marblegate.boomood.mechanism.itemstackreversion.cases;

import love.marblegate.boomood.Boomood;
import love.marblegate.boomood.config.Configuration;
import love.marblegate.boomood.mechanism.itemstackreversion.dataholder.AvailableBlockPosHolder;
import love.marblegate.boomood.mechanism.itemstackreversion.dataholder.IntermediateResultHolder;
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
    public void add(IntermediateResultHolder pack) {
        // TODO need to String
        Boomood.LOGGER.debug("Reverting EntityDeath. Details: " + this);
        // TODO
    }

    @Override
    public void revert(Player manipulator, AvailableBlockPosHolder blockPosHolder) {
        // TODO
    }
}
