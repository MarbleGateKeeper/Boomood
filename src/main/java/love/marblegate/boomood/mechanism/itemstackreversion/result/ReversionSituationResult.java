package love.marblegate.boomood.mechanism.itemstackreversion.result;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.*;

public abstract class ReversionSituationResult {
    public static ReversionSituationResult createArmorHandler() {
        return new ArmorStandDestructionSituationResult();
    }

    public static ReversionSituationResult createDefaultHandler() {
        return new ChestDestructionSituationResult();
    }

    public abstract JsonObject toJson();

    public abstract String situationId();

    public static ReversionSituationResult create(JsonObject jsonObject) {
        var st = GsonHelper.getAsString(jsonObject, "type");
        return switch (st) {
            case "entity_death" -> new EntityDeathSituationResult(jsonObject);
            case "chest_destruction" -> new ChestDestructionSituationResult(jsonObject);
            case "item_frame_destruction" -> new ItemFrameDestructionSituationResult(jsonObject);
            case "armor_stand_destruction" -> new ArmorStandDestructionSituationResult();
            case "block_destruction" -> new BlockDestructionSituationResult(jsonObject);
            default -> throw new JsonSyntaxException("Expected type to be \"entity_death\", \"chest_destruction\", \"item_frame_destruction\", \"armor_stand_destruction\" or \"block_destruction\", was " + st);
        };
    }

}
