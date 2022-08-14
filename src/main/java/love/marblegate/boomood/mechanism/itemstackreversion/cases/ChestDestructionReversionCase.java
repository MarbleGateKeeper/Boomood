package love.marblegate.boomood.mechanism.itemstackreversion.cases;

import love.marblegate.boomood.Boomood;
import love.marblegate.boomood.mechanism.itemstackreversion.dataholder.AvailableBlockPosHolder;
import love.marblegate.boomood.mechanism.itemstackreversion.dataholder.IntermediateResultHolder;
import love.marblegate.boomood.mechanism.itemstackreversion.result.ChestDestructionSituationResult;
import love.marblegate.boomood.misc.MiscUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ChestDestructionReversionCase implements ReversionCase {
    private final List<ItemStack> targets = new ArrayList<>();

    @Override
    public int priority() {
        return 75;
    }

    @Override
    public void add(IntermediateResultHolder pack) {
        var result = (ChestDestructionSituationResult) pack.result();
        if (result.getTargets() == null)
            targets.addAll(pack.items());
        else
            targets.addAll(result.getTargets());
    }

    @Override
    public void revert(Entity manipulator, AvailableBlockPosHolder blockPosHolder) {
        Boomood.LOGGER.debug("Reverting ChestDestruction. Details: " + this);
        for (var itemStack : targets) {
            MiscUtils.insertIntoChestOrCreateChest(manipulator.level, blockPosHolder, itemStack.copy());
        }
        // TODO add custom particle effect for indication & add implement explosion particle
    }

    @Override
    public String toString() {
        return "ChestDestructionReversionCase{" +
                "targets=" + targets +
                '}';
    }
}

