package love.marblegate.boomood.mechanism.itemstackreversion.handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import love.marblegate.boomood.misc.MiscUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class ChestDestructionHandler extends ItemStackRevertHandler {
    // TODO fix: it seems like itemstack info does not be correctly parsed
    private final List<ItemStack> targets;

    public ChestDestructionHandler(JsonObject jsonObject) {
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

    public ChestDestructionHandler() {
        this.targets = null;
    }

    @Override
    public void revert(Level level, BlockPos blockPos, List<ItemStack> itemStacks, Player manipulator) {
        var insertItemStacks = targets == null ? List.of(itemStacks.get(0)) : targets;
        for(var itemStack:insertItemStacks){
            MiscUtils.insertIntoChestOrCreateChest(level, blockPos, itemStack.copy());
        }
        // TODO add custom particle effect for indication & add implement explosion particle
    }


    @Override
    public int priority() {
        return 50;
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
    public List<List<ItemStack>> mergeItemStack(List<List<ItemStack>> itemStackListList) {
        // No need to merge
        return itemStackListList;
    }

    @Override
    public String toString() {
        return "ChestDestructionHandler{" + "target=" + targets + '}';
    }
}
