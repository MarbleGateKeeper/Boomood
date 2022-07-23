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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.items.CapabilityItemHandler;

import java.util.List;
import java.util.Random;

public class ChestDestructionHandler extends ItemStackRevertHandler {
    // TODO fix: it seems like itemstack info does not be correctly parsed
    private final ItemStack target;

    public ChestDestructionHandler(JsonObject jsonObject) {
        if (jsonObject.has("itemstack")) {
            var itemStackJson = GsonHelper.getAsJsonObject(jsonObject, "itemstack");
            var itemStackResult = ItemStack.CODEC.parse(JsonOps.INSTANCE, itemStackJson);
            target = itemStackResult.getOrThrow(false, err -> {
                throw new JsonSyntaxException("Invalid itemstack: " + itemStackJson + ".Error: " + err);
            });
        } else target = null;
    }

    public ChestDestructionHandler(ItemStack target) {
        this.target = target;
    }

    @Override
    public void revert(Level level, BlockPos blockPos, List<ItemStack> itemStacks, Player manipulator) {
        var insertItemStack = target == null ? itemStacks.get(0) : target;
        // TODO in recipe json, itemstack can be extended to array
        MiscUtils.insertIntoChestOrCreateChest(level, blockPos, insertItemStack);
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
        if (target != null) {
            ret.add("itemstack", ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, target).get().left().get());
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
        return "ChestDestructionHandler{" + "target=" + target + '}';
    }
}
