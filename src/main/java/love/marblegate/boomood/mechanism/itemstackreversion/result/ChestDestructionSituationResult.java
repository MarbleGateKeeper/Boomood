package love.marblegate.boomood.mechanism.itemstackreversion.result;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import love.marblegate.boomood.misc.MiscUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class ChestDestructionSituationResult extends ReversionSituationResult {
    private final List<ItemStack> targets;

    public ChestDestructionSituationResult(JsonObject jsonObject) {
        if (jsonObject.has("itemstack")) {
            try{
                var itemStackJson = GsonHelper.getAsJsonObject(jsonObject, "itemstack");
                var itemStackResult = MiscUtils.ITEMSTACK_CODEC.parse(JsonOps.INSTANCE, itemStackJson);
                targets = List.of(itemStackResult.getOrThrow(false, err -> {
                    throw new JsonSyntaxException("Invalid itemstack: " + itemStackJson + ". Error: " + err);
                }));
            }catch(ClassCastException e){
                throw new JsonSyntaxException("\"itemstack\" in recipe json must be a JsonObject to represent a itemstack.");
            }
        } else if (jsonObject.has("itemstacks")) {
            try{
                var itemStackJson = GsonHelper.getAsJsonArray(jsonObject, "itemstacks");
                var itemStackResult = MiscUtils.ITEMSTACK_CODEC.listOf().parse(JsonOps.INSTANCE, itemStackJson);
                targets = itemStackResult.getOrThrow(false, err -> {
                    throw new JsonSyntaxException("Invalid itemstacks: " + itemStackJson + ". Error: " + err);
                });
            }catch(ClassCastException e){
                throw new JsonSyntaxException("\"itemstacks\" in recipe json must be a JsonArray to represent itemstacks.");
            }
        } else targets = null;
    }

    public ChestDestructionSituationResult() {
        this.targets = null;
    }


    @Override
    public JsonObject toJson() {
        var ret = new JsonObject();
        ret.addProperty("type", "chest_destruction");
        if (targets != null) {
            if(targets.size()==1)
                ret.add("itemstack", MiscUtils.ITEMSTACK_CODEC.encodeStart(JsonOps.INSTANCE, targets.get(0)).get().left().get());
            else
                ret.add("itemstacks", MiscUtils.ITEMSTACK_CODEC.listOf().encodeStart(JsonOps.INSTANCE, targets).get().left().get());
        }
        return ret;
    }

    @Override
    public String situationId() {
        return "chest_destruction";
    }


    public List<ItemStack> getTargets() {
        return targets;
    }

    @Override
    public String toString() {
        return "ChestDestructionSituationResult{" +
                "targets=" + targets +
                '}';
    }
}
