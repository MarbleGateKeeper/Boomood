package love.marblegate.boomood.mechanism.itemstackreversion.dataholder;

import com.google.common.collect.Lists;
import love.marblegate.boomood.mechanism.itemstackreversion.cases.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class ReversionCaseHolder {
    private final Map<String, ReversionCase> caseList;

    public ReversionCaseHolder() {
        this.caseList = new HashMap<>();
    }

    public void add(ResultPack pack){
        if(!caseList.containsKey(pack.result().situationId()))
            caseList.put(pack.result().situationId(), createCase(pack.result().situationId()));
        caseList.get(pack.result().situationId()).add(pack);
    }

    public void apply(BlockPos eventCenter, Player player){
         var l = Lists.newArrayList(caseList.values());
         l.sort(Comparator.comparingInt(o -> -o.priority()));
         l.forEach(reversionCase -> reversionCase.revert(player,eventCenter));
    }

    private ReversionCase createCase(String situationId){
        return switch(situationId){
            case "entity_death" -> new EntityDeathReversionCase();
            case "chest_destruction" -> new ChestDestructionReversionCase();
            case "item_frame_destruction" -> new ItemFrameDestructionReversionCase();
            case "armor_stand_destruction" -> new ArmorStandDestructionReversionCase();
            case "block_destruction" -> new BlockDestructionReversionCase();
            default -> throw new RuntimeException("Unrecognizable situationId: " + situationId);
        };
    }
}
