package love.marblegate.boomood.mechanism.itemstackreversion.result;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class EntityDeathSituationResult extends ReversionSituationResult {
    private final EntityType<?> entityType;

    public EntityDeathSituationResult(JsonObject jsonObject) {
        var es = GsonHelper.getAsString(jsonObject, "entity");
        entityType = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(es));
        if (entityType == null) {
            throw new JsonSyntaxException("Invalid entity type: " + es);
        }
    }

    @Override
    public JsonObject toJson() {
        var ret = new JsonObject();
        ret.addProperty("type", "entity_death");
        ret.addProperty("entity", entityType.getRegistryName().toString());
        return ret;
    }

    @Override
    public String situationId() {
        return "entity_death";
    }

    @Override
    public String toString() {
        // TODO need regenerate after completion
        return "EntityDeathSituationResult{" +
                "entityType=" + entityType +
                '}';
    }
}
