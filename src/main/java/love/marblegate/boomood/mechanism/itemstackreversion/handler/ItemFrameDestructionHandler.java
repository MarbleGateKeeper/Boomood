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
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class ItemFrameDestructionHandler extends ItemStackRevertHandler {

    @Nullable
    private final ItemStack target;

    public ItemFrameDestructionHandler(JsonObject jsonObject) {
        if (jsonObject.has("itemstack")) {
            var itemStackJson = GsonHelper.getAsJsonObject(jsonObject, "itemstack");
            var itemStackResult = ItemStack.CODEC.parse(JsonOps.INSTANCE, itemStackJson);
            target = itemStackResult.getOrThrow(false, err -> {
                throw new JsonSyntaxException("Invalid block nbt: " + itemStackJson + ".Error: " + err);
            });
        } else target = null;
    }

    @Override
    public void revert(Level level, BlockPos blockPos, List<ItemStack> itemStacks, Player manipulator) {
        var isGlowItemFrame = Math.random() < Configuration.ItemStackReversion.GLOW_ITEM_FRAME_POSSIBILITY.get();
        var blockPosList = MiscUtils.createShuffledBlockPosList(MiscUtils.createScanningArea(blockPos));
        ItemFrame itemFrame;
        var displayItemStack = target == null ? itemStacks.get(0) : target;
        for (var bp : blockPosList) {
            for (var direction : Direction.values()) {
                if (isGlowItemFrame) itemFrame = new GlowItemFrame(level, bp, direction);
                else itemFrame = new ItemFrame(level, bp, direction);
                if (itemFrame.survives()) {
                    itemFrame.setItem(displayItemStack);
                    level.addFreshEntity(itemFrame);
                    displayItemStack = ItemStack.EMPTY;
                    // TODO add custom particle effect for indication & add implement explosion particle
                    break;
                }
            }
            if (displayItemStack.isEmpty()) break;
        }
        if (!displayItemStack.isEmpty()) {
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
                        itemFrame.setItem(displayItemStack);
                        level.addFreshEntity(itemFrame);
                        // TODO add custom particle effect for indication & add implement explosion particle
                        break;
                    }
                }
            } else {
                MiscUtils.insertIntoChestOrCreateChest(level, blockPos, displayItemStack);
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
        if (target != null) {
            ret.add("itemstack", ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, target).get().left().get());
        }
        return ret;
    }
}
