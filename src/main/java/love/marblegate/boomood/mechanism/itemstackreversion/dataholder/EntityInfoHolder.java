package love.marblegate.boomood.mechanism.itemstackreversion.dataholder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

public record EntityInfoHolder(EntityType<?> entityType, CompoundTag tags, int count) {
    public static Codec<EntityInfoHolder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ForgeRegistries.ENTITIES.getCodec().fieldOf("id").forGetter(EntityInfoHolder::entityType),
            CompoundTag.CODEC.optionalFieldOf("tag",new CompoundTag()).forGetter(EntityInfoHolder::tags),
            Codec.INT.optionalFieldOf("Count",1).forGetter(EntityInfoHolder::count)).apply(instance, EntityInfoHolder::new));

    @Override
    public EntityType<?> entityType() {
        return entityType;
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
        return "EntityInfoHolder{" +
                "entityType=" + entityType +
                ", tags=" + tags +
                ", count=" + count +
                '}';
    }
}
