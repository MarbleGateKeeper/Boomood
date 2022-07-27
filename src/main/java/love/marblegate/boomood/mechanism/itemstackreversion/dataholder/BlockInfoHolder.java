package love.marblegate.boomood.mechanism.itemstackreversion.dataholder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

public record BlockInfoHolder(BlockState blockState, CompoundTag tags, int count) {
    public static Codec<BlockInfoHolder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("blockstate").forGetter(BlockInfoHolder::blockState),
            CompoundTag.CODEC.optionalFieldOf("tags",new CompoundTag()).forGetter(BlockInfoHolder::tags),
            Codec.INT.optionalFieldOf("count",1).forGetter(BlockInfoHolder::count)).apply(instance, BlockInfoHolder::new));

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

    @Override
    public String toString() {
        return "BlockInfoHolder{" +
                "blockState=" + blockState +
                ", tags=" + tags +
                ", count=" + count +
                '}';
    }
}
