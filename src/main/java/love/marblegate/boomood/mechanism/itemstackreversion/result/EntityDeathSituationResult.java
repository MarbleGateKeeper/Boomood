package love.marblegate.boomood.mechanism.itemstackreversion.result;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import love.marblegate.boomood.mechanism.itemstackreversion.dataholder.EntityInfoHolder;
import net.minecraft.util.GsonHelper;

import java.util.List;

public class EntityDeathSituationResult extends ReversionSituationResult {

    private final List<EntityInfoHolder> holderList;

    public EntityDeathSituationResult(JsonObject jsonObject) {
        if (jsonObject.has("entity")) {
            try {
                var entity = GsonHelper.getAsJsonObject(jsonObject, "entity");
                var dataResult = EntityInfoHolder.CODEC.parse(JsonOps.INSTANCE, entity);
                holderList = List.of(dataResult.getOrThrow(false, err -> {
                    throw new JsonSyntaxException("Invalid entity: " + entity + ". Error: " + err);
                }));
            } catch (ClassCastException e) {
                throw new JsonSyntaxException("\"entity\" in recipe json must be a JsonObject to represent a entity.");
            }
        } else if (jsonObject.has("entities")) {
            try {
                var entities = GsonHelper.getAsJsonArray(jsonObject, "entities");
                var dataResult = EntityInfoHolder.CODEC.listOf().parse(JsonOps.INSTANCE, entities);
                holderList = dataResult.getOrThrow(false, err -> {
                    throw new JsonSyntaxException("Invalid entities: " + entities + ". Error: " + err);
                });
            } catch (ClassCastException e) {
                throw new JsonSyntaxException("\"entities\" in recipe json must be a JsonArray to represent entities.");
            }
        } else throw new JsonSyntaxException("\"entity\" or \"entities\" must appear in recipe json.");
    }

    @Override
    public JsonObject toJson() {
        var ret = new JsonObject();
        ret.addProperty("type", "entity_death");
        if (holderList.size() == 1)
            ret.add("entity", EntityInfoHolder.CODEC.encodeStart(JsonOps.INSTANCE, holderList.get(0)).get().left().get());
        else
            ret.add("entities", EntityInfoHolder.CODEC.listOf().encodeStart(JsonOps.INSTANCE, holderList).get().left().get());
        return ret;
    }

    @Override
    public String situationId() {
        return "entity_death";
    }

    public List<EntityInfoHolder> getHolderList() {
        return holderList;
    }

    @Override
    public String toString() {
        // TODO need regenerate after completion
        return "EntityDeathSituationResult{" +
                "entityType=" + holderList +
                '}';
    }
}
