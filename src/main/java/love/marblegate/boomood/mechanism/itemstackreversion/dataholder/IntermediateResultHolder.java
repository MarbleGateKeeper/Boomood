package love.marblegate.boomood.mechanism.itemstackreversion.dataholder;

import love.marblegate.boomood.mechanism.itemstackreversion.result.ReversionSituationResult;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record IntermediateResultHolder(ReversionSituationResult result, List<ItemStack> items) {

    @Override
    public String toString() {
        return "IntermediateResultHolder{" +
                "result=" + result +
                ", items=" + items +
                '}';
    }
}
