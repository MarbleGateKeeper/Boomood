package love.marblegate.boomood.mechanism.situation.handler;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import love.marblegate.boomood.misc.MiscUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BlockDestructionHandler extends ItemStackDropSituationHandler {

    public record BlockInfo(BlockState blockState,CompoundTag tags,int count){
        @Override
        public BlockState blockState() {
            return blockState;
        }

        @Override
        public CompoundTag tags() {
            return tags;
        }

        @Override
        public int count() {
            return count;
        }
    }

    public static Codec<BlockInfo> BLOCK_INFO_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("blockstate").forGetter(BlockInfo::blockState),
            CompoundTag.CODEC.optionalFieldOf("tags",new CompoundTag()).forGetter(BlockInfo::tags),
            Codec.INT.optionalFieldOf("count",1).forGetter(BlockInfo::count)).apply(instance,BlockInfo::new));


    private final List<BlockInfo> blockInfos;

    public BlockDestructionHandler(JsonObject jsonObject) {
        var json = GsonHelper.getAsJsonArray(jsonObject, "blocks");
        blockInfos = BLOCK_INFO_CODEC.listOf().parse(JsonOps.INSTANCE,json).getOrThrow(false,err -> {
            throw new JsonSyntaxException("BlockInfos in BlockDestructionHandler deserialization failed. Error: " + err);
        });
    }

    @Override
    public void revert(Level level, BlockPos blockPos, List<ItemStack> itemStacks, Player manipulator) {
        for(var blockInfo:blockInfos){
            for(int i=0;i<blockInfo.count;i++){
                var optional = MiscUtils.randomizeDestination(level, blockPos);
                if (optional.isEmpty()) return;
                var destination = optional.get();
                // Falling block should have support block
                if (blockInfo.blockState.getBlock() instanceof FallingBlock && level.getBlockState(destination.below()).is(Blocks.AIR)) {
                    level.setBlockAndUpdate(destination.below(), MiscUtils.getSupportBlock(level, destination.below()));
                }
                level.setBlockAndUpdate(destination, blockInfo.blockState);
                if (blockInfo.tags != null) {
                    BlockEntity blockentity = level.getBlockEntity(destination);
                    if (blockentity != null) {
                        blockentity.load(blockInfo.tags);
                    }
                }
                // TODO add custom particle effect for indication & add implement explosion particle
            }
        }
    }


    @Override
    public int priority() {
        return 100;
    }

    @Override
    public JsonObject toJson() {
        var ret = new JsonObject();
        ret.addProperty("type", "block_destruction");
        var json = BLOCK_INFO_CODEC.listOf().encodeStart(JsonOps.INSTANCE,blockInfos);
        ret.add("blocks", json.getOrThrow(false,err -> {
            throw new JsonSyntaxException("BlockInfos in BlockDestructionHandler serialization failed. Error: " + err);
        }));
        return ret;
    }

    @Override
    public List<List<ItemStack>> mergeItemStack(List<List<ItemStack>> itemStackListList) {
        // For reverting block destruction, how itemStack arrangement is preprocessed is irrelevant.
        // So just return what is passed in.
        return itemStackListList;
    }
}
