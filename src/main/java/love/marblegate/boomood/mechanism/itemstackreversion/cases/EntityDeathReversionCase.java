package love.marblegate.boomood.mechanism.itemstackreversion.cases;

import love.marblegate.boomood.Boomood;
import love.marblegate.boomood.config.Configuration;
import love.marblegate.boomood.mechanism.itemstackreversion.dataholder.AvailableBlockPosHolder;
import love.marblegate.boomood.mechanism.itemstackreversion.dataholder.EntityInfoHolder;
import love.marblegate.boomood.mechanism.itemstackreversion.dataholder.IntermediateResultHolder;
import love.marblegate.boomood.mechanism.itemstackreversion.result.BlockDestructionSituationResult;
import love.marblegate.boomood.mechanism.itemstackreversion.result.EntityDeathSituationResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class EntityDeathReversionCase implements ReversionCase {
    //TODO
    private final List<EntityInfoHolder> holderList = new ArrayList<>();

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public void add(IntermediateResultHolder pack) {
        holderList.addAll(((EntityDeathSituationResult) pack.result()).getHolderList());
    }

    @Override
    public void revert(Player manipulator, AvailableBlockPosHolder blockPosHolder) {
        Boomood.LOGGER.debug("Reverting EntityDeath. Details: " + this);
        // TODO
    }

    @Override
    public String toString() {
        return "EntityDeathReversionCase{" +
                "holderList=" + holderList +
                '}';
    }
}
