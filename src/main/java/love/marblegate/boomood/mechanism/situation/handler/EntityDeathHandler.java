package love.marblegate.boomood.mechanism.situation.handler;

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

public class EntityDeathHandler extends ItemStackDropSituationHandler {
    private final EntityType<?> entityType;

    public EntityDeathHandler(JsonObject jsonObject) {
        var es = GsonHelper.getAsString(jsonObject, "entity");
        entityType = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(es));
        if (entityType == null) {
            throw new JsonSyntaxException("Invalid entity type: " + es);
        }
    }

    EntityDeathHandler(EntityType<?> entityType) {
        this.entityType = entityType;
    }

    @Override
    public void revert(Level level, BlockPos blockPos, List<ItemStack> itemStacks, Player manipulator) {
        //TODO
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public List<List<ItemStack>> mergeItemStack(List<List<ItemStack>> itemStackListList) {
        // For reverting entity death, how itemStack arrangement is preprocessed is irrelevant.
        // So just return what is passed in.
        return itemStackListList;
    }

    @Override
    public JsonObject toJson() {
        var ret = new JsonObject();
        ret.addProperty("type", "entity_death");
        ret.addProperty("entity", entityType.getRegistryName().toString());
        return ret;
    }
}
