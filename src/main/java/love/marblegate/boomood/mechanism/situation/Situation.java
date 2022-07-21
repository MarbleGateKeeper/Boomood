package love.marblegate.boomood.mechanism.situation;

import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import love.marblegate.boomood.mechanism.situation.handler.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public abstract class Situation {
    public static Codec<ItemStackDropSituationHandler> CODEC = Codec.PASSTHROUGH.comapFlatMap(dynamic ->
    {
        try {
            var st = dynamic.convert(JsonOps.INSTANCE).getValue().getAsJsonObject();
            var type = st.getAsJsonPrimitive("type").getAsString();
            return switch (type) {
                case "entity_death" -> DataResult.success(new EntityDeathHandler(st));
                case "chest_destruction" -> DataResult.success(new ChestDestructionHandler(st));
                case "item_frame_destruction" -> DataResult.success(new ItemFrameDestructionHandler(st));
                case "armor_stand_destruction" -> DataResult.success(new ArmorStandDestructionHandler());
                case "block_destruction" -> DataResult.success(new BlockDestructionHandler(st));
                default -> throw new JsonSyntaxException("Expected type to be \"entity_death\", \"chest_destruction\", \"item_frame_destruction\", \"armor_stand_destruction\" or \"block_destruction\", was " + st);
            };
        } catch (Exception e) {
            return DataResult.error(e.getMessage());
        }
    }, handler -> new Dynamic<>(JsonOps.INSTANCE,handler.toJson()));
    public abstract void revert(Level level, BlockPos blockPos, Player manipulator);
}
