package love.marblegate.boomood.mechanism.itemstackreversion.dataholder;

import com.google.common.collect.Lists;
import love.marblegate.boomood.mechanism.itemstackreversion.cases.*;
import net.minecraft.world.entity.player.Player;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class ReversionCaseHolder {
    private final Map<String, ReversionCase> caseList;

    public ReversionCaseHolder() {
        this.caseList = new HashMap<>();
    }

    public void add(IntermediateResultHolder holder){
        if(!caseList.containsKey(holder.result().situationId()))
            caseList.put(holder.result().situationId(), createCase(holder.result().situationId()));
        caseList.get(holder.result().situationId()).add(holder);
    }

    public void apply(Player player, AvailableBlockPosHolder holder){
         var l = Lists.newArrayList(caseList.values());
         l.sort(Comparator.comparingInt(o -> -o.priority()));
         l.forEach(reversionCase -> reversionCase.revert(player,holder));
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
