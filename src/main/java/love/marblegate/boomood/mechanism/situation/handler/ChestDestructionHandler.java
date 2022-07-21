package love.marblegate.boomood.mechanism.situation.handler;

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

public class ChestDestructionHandler extends ItemStackDropSituationHandler {
    private final ItemStack target;

    public ChestDestructionHandler(JsonObject jsonObject) {
        if (jsonObject.has("itemstack")) {
            var itemStackJson = GsonHelper.getAsJsonObject(jsonObject, "itemstack");
            var itemStackResult = ItemStack.CODEC.parse(JsonOps.INSTANCE, itemStackJson);
            target = itemStackResult.getOrThrow(false, err -> {
                throw new JsonSyntaxException("Invalid itemstack nbt: " + itemStackJson + ".Error: " + err);
            });
        } else target = null;
    }

    public ChestDestructionHandler(ItemStack target) {
        this.target = target;
    }

    @Override
    public void revert(Level level, BlockPos blockPos, List<ItemStack> itemStacks, Player manipulator) {
        var insertItemStack = target == null ? itemStacks.get(0) : target;
        insertItemStack = MiscUtils.searchValidChestAndInsert(level, blockPos, insertItemStack);
        if (!insertItemStack.isEmpty()) {
            var optional = MiscUtils.randomizeDestination(level, blockPos);
            if (optional.isEmpty()) return;
            var destination = optional.get();
            var facings = ChestBlock.FACING.getAllValues().toList().stream().map(Property.Value::value).toList();
            level.setBlockAndUpdate(destination, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facings.get(new Random().nextInt(facings.size()))));
            BlockEntity chestBlockEntity = level.getBlockEntity(destination);
            var itemhandler = chestBlockEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if (itemhandler.isPresent()) {
                ItemStack finalInsertItemStack = insertItemStack;
                itemhandler.ifPresent(cap -> {
                    cap.insertItem(0, finalInsertItemStack, false);
                });
            }
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
}
