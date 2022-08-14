package love.marblegate.boomood.mechanism;

import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import love.marblegate.boomood.mechanism.itemstackreversion.result.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

public abstract class Reversion {
    public static Codec<ReversionSituationResult> CODEC = Codec.PASSTHROUGH.comapFlatMap(dynamic ->
    {
        try {
            var st = dynamic.convert(JsonOps.INSTANCE).getValue().getAsJsonObject();
            var type = st.getAsJsonPrimitive("type").getAsString();
            return switch (type) {
                case "entity_death" -> DataResult.success(new EntityDeathSituationResult(st));
                case "chest_destruction" -> DataResult.success(new ChestDestructionSituationResult(st));
                case "item_frame_destruction" -> DataResult.success(new ItemFrameDestructionSituationResult(st));
                case "armor_stand_destruction" -> DataResult.success(new ArmorStandDestructionSituationResult());
                case "block_destruction" -> DataResult.success(new BlockDestructionSituationResult(st));
                default ->
                        throw new JsonSyntaxException("Expected type to be \"entity_death\", \"chest_destruction\", \"item_frame_destruction\", \"armor_stand_destruction\" or \"block_destruction\", was " + st);
            };
        } catch (Exception e) {
            return DataResult.error(e.getMessage());
        }
    }, handler -> new Dynamic<>(JsonOps.INSTANCE, handler.toJson()));

    public abstract void revert(Entity manipulator, BlockPos blockPos);
}
