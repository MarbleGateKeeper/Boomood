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
        if (jsonObject.has("block")) {
            try{
                var block = GsonHelper.getAsJsonObject(jsonObject, "block");
                var dataResult = BlockInfoHolder.CODEC.parse(JsonOps.INSTANCE, block);
                blockInfoHolders = List.of(dataResult.getOrThrow(false, err -> {
                    throw new JsonSyntaxException("Invalid block: " + block + ". Error: " + err);
                }));
            }catch(ClassCastException e){
                throw new JsonSyntaxException("\"block\" in recipe json must be a JsonObject to represent a block.");
            }
        } else if (jsonObject.has("blocks")) {
            try{
                var blocks = GsonHelper.getAsJsonArray(jsonObject, "blocks");
                var dataResult = BlockInfoHolder.CODEC.listOf().parse(JsonOps.INSTANCE, blocks);
                blockInfoHolders = dataResult.getOrThrow(false, err -> {
                    throw new JsonSyntaxException("Invalid entities: " + blocks + ". Error: " + err);
                });
            }catch(ClassCastException e){
                throw new JsonSyntaxException("\"blocks\" in recipe json must be a JsonArray to represent blocks.");
            }
        } else throw new JsonSyntaxException("\"block\" or \"blocks\" must appear in recipe json.");
    }

    @Override
    public JsonObject toJson() {
        var ret = new JsonObject();
        ret.addProperty("type", "block_destruction");
        if(blockInfoHolders.size()==1)
            ret.add("block", BlockInfoHolder.CODEC.encodeStart(JsonOps.INSTANCE, blockInfoHolders.get(0)).get().left().get());
        else
            ret.add("blocks", BlockInfoHolder.CODEC.listOf().encodeStart(JsonOps.INSTANCE, blockInfoHolders).get().left().get());
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
