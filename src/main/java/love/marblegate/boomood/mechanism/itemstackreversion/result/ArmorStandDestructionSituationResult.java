package love.marblegate.boomood.mechanism.itemstackreversion.result;

import com.google.gson.JsonObject;

public class ArmorStandDestructionSituationResult extends ReversionSituationResult {

    @Override
    public JsonObject toJson() {
        var ret = new JsonObject();
        ret.addProperty("type", "armor_stand_destruction");
        return ret;
    }

    @Override
    public String situationId() {
        return "armor_stand_destruction";
    }

    @Override
    public String toString() {
        return "ArmorStandDestructionSituationResult{}";
    }
}
