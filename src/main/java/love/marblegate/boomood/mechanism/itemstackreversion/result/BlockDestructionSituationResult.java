package love.marblegate.boomood.mechanism.itemstackreversion.result;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import love.marblegate.boomood.mechanism.itemstackreversion.dataholder.BlockInfoHolder;
import net.minecraft.util.GsonHelper;

import java.util.List;

public class BlockDestructionSituationResult extends ReversionSituationResult {
    private final List<BlockInfoHolder> blockInfoHolders;

    public BlockDestructionSituationResult(JsonObject jsonObject) {
        var json = GsonHelper.getAsJsonArray(jsonObject, "blocks");
        blockInfoHolders = BlockInfoHolder.BLOCK_INFO_HOLDER_CODEC.listOf().parse(JsonOps.INSTANCE,json).getOrThrow(false, err -> {
            throw new JsonSyntaxException("BlockInfos in BlockDestructionHandler deserialization failed. Error: " + err);
        });
    }

    @Override
    public JsonObject toJson() {
        var ret = new JsonObject();
        ret.addProperty("type", "block_destruction");
        var json = BlockInfoHolder.BLOCK_INFO_HOLDER_CODEC.listOf().encodeStart(JsonOps.INSTANCE, blockInfoHolders);
        ret.add("blocks", json.getOrThrow(false,err -> {
            throw new JsonSyntaxException("BlockInfos in BlockDestructionHandler serialization failed. Error: " + err);
        }));
        return ret;
    }

    @Override
    public String situationId() {
        return "block_destruction";
    }

    public List<BlockInfoHolder> getBlockInfoHolders() {
        return blockInfoHolders;
    }

    @Override
    public String toString() {
        return "BlockDestructionSituationResult{" +
                "blockInfoHolders=" + blockInfoHolders +
                '}';
    }
}
