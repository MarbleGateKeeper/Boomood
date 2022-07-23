package love.marblegate.boomood.mechanism.itemstackreversion.cases;

import love.marblegate.boomood.config.Configuration;
import love.marblegate.boomood.mechanism.itemstackreversion.result.ChestDestructionSituationResult;
import love.marblegate.boomood.mechanism.itemstackreversion.dataholder.ResultPack;
import love.marblegate.boomood.misc.MiscUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
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
    public void add(ResultPack pack) {
        var result = (ChestDestructionSituationResult) pack.result();
        if(result.getTargets()==null)
            targets.addAll(pack.items());
        else
            targets.addAll(result.getTargets());
    }

    @Override
    public void revert(Player manipulator, BlockPos blockPos) {
        if(Configuration.DEBUG_MODE.get()){
            System.out.println("Reverting ItemFrameDestruction. Details: " + this);
        }
        for (var itemStack : targets) {
            MiscUtils.insertIntoChestOrCreateChest(manipulator.level, blockPos, itemStack.copy());
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

