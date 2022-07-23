package love.marblegate.boomood.mechanism.itemstackreversion.handler;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import love.marblegate.boomood.config.Configuration;
import love.marblegate.boomood.misc.MiscUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemFrameDestructionHandler extends ItemStackRevertHandler {

    @Nullable
    private final List<ItemStack> targets;

    public ItemFrameDestructionHandler(JsonObject jsonObject) {
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

    @Override
    public void revert(Level level, BlockPos blockPos, List<ItemStack> itemStacks, Player manipulator) {
        // TODO need fix: will item frame spawn inside block
        var isGlowItemFrame = Math.random() < Configuration.ItemStackReversion.GLOW_ITEM_FRAME_POSSIBILITY.get();
        var blockPosList = MiscUtils.createShuffledBlockPosList(MiscUtils.createScanningArea(blockPos));
        ItemFrame itemFrame;
        var displayItemStack = targets == null ? itemStacks : targets;
        for(var itemStack:displayItemStack){
            var is = itemStack.copy();
            for (var bp : blockPosList) {
                for (var direction : Direction.values()) {
                    if (isGlowItemFrame) itemFrame = new GlowItemFrame(level, bp, direction);
                    else itemFrame = new ItemFrame(level, bp, direction);
                    if (itemFrame.survives()) {
                        itemFrame.setItem(is.copy());
                        level.addFreshEntity(itemFrame);
                        is = ItemStack.EMPTY;
                        // TODO add custom particle effect for indication & add implement explosion particle
                        break;
                    }
                }
                if (is.isEmpty()) break;
            }
            if (!is.isEmpty()) {
                if (Configuration.ItemStackReversion.ITEM_FRAME_SITUATION_REMEDY_IS_SUPPORT_BLOCK.get()) {
                    var tryTime = 0;
                    while (tryTime < 3) {
                        var optional = MiscUtils.randomizeDestination(level, blockPos);
                        if (optional.isEmpty()) return;
                        var destination = optional.get();
                        var od = getEmptyNeighborDirection(level, destination);
                        if (od.isEmpty()) {
                            tryTime++;
                        } else {
                            level.setBlockAndUpdate(destination.relative(od.get()), MiscUtils.getSupportBlock(level, destination.relative(od.get())));
                            if (isGlowItemFrame) itemFrame = new GlowItemFrame(level, destination, od.get().getOpposite());
                            else itemFrame = new ItemFrame(level, destination, od.get().getOpposite());
                            itemFrame.setItem(is);
                            level.addFreshEntity(itemFrame);
                            // TODO add custom particle effect for indication & add implement explosion particle
                            break;
                        }
                    }
                } else {
                    MiscUtils.insertIntoChestOrCreateChest(level, blockPos, is);
                }
            }
        }

    }


    private Optional<Direction> getEmptyNeighborDirection(Level level, BlockPos blockPos) {
        for (var direction : Direction.values()) {
            if (level.getBlockState(blockPos.relative(direction)).getBlock().equals(Blocks.AIR)) {
                return Optional.of(direction);
            }
        }
        return Optional.empty();
    }

    @Override
    public int priority() {
        return 30;
    }

    @Override
    public List<List<ItemStack>> mergeItemStack(List<List<ItemStack>> itemStackListList) {
        // Every itemFrame can hold only 1 Item.
        List<List<ItemStack>> ret = new ArrayList<>();
        itemStackListList.forEach(itemStackList -> itemStackList.forEach(itemStack -> {
            var givenItemStack = itemStack.copy();
            var amount = itemStack.getCount();
            givenItemStack.setCount(1);
            for (int i = 0; i < amount; i++) {
                var putIn = Lists.newArrayList(givenItemStack);
                ret.add(putIn);
            }
        }));
        return ret;
    }

    @Override
    public JsonObject toJson() {
        var ret = new JsonObject();
        ret.addProperty("type", "item_frame_destruction");
        if (targets != null) {
            if(targets.size()==1)
                ret.add("itemstack", MiscUtils.ITEMSTACK_CODEC.encodeStart(JsonOps.INSTANCE, targets.get(0)).get().left().get());
            else
                ret.add("itemstacks", MiscUtils.ITEMSTACK_CODEC.listOf().encodeStart(JsonOps.INSTANCE, targets).get().left().get());
        }
        return ret;
    }

    @Override
    public String toString() {
        return "ItemFrameDestructionHandler{" + "targets=" + targets + '}';
    }
}
