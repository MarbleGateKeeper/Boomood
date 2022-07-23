package love.marblegate.boomood.mechanism.itemstackreversion;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import love.marblegate.boomood.mechanism.Reversion;
import love.marblegate.boomood.mechanism.itemstackreversion.result.ReversionSituationResult;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class ReversionSituation {
    static Codec<List<ReversionSituation.Condition>> CONDITION_CODEC = Codec.PASSTHROUGH.comapFlatMap(dynamic ->
    {
        try {
            var json = dynamic.convert(JsonOps.INSTANCE).getValue().getAsJsonObject();
            List<ReversionSituation.Condition> ret = new ArrayList<>();
            if (json.size() != 0) {
                var heightConditionJson = json.getAsJsonObject("height");
                if (heightConditionJson != null)
                    ret.add(new ReversionSituation.HeightPredicate(heightConditionJson));
                var biomeConditionJson = json.getAsJsonObject("biome");
                if (biomeConditionJson != null)
                    ret.add(new ReversionSituation.BiomePredicate(biomeConditionJson));
            }
            return DataResult.success(ret);
        } catch (Exception e) {
            return DataResult.error(e.getMessage());
        }
    }, conditions -> {
        var ret = new JsonObject();
        for(var condition:conditions){
            if(condition instanceof ReversionSituation.HeightPredicate heightPredicate){
                var temp = new JsonObject();
                if(heightPredicate.getType() == ReversionSituation.HeightPredicate.Type.GREATER)
                    temp.addProperty("type","greater");
                else temp.addProperty("type","less");
                temp.addProperty("content",heightPredicate.getLimit());
                ret.add("height",temp);
            } else {
                var condition1 = (ReversionSituation.BiomePredicate) condition;
                var temp = new JsonObject();
                if(condition1.getType() == ReversionSituation.BiomePredicate.Type.ALLOWLIST)
                    temp.addProperty("type","allowlist");
                else temp.addProperty("type","blocklist");
                var temp2 = new JsonArray();
                for(var biome : condition1.getBiomeList()){
                    temp2.add(biome.getRegistryName().toString());
                }
                temp.add("content",temp2);
            }
        }
        return new Dynamic<>(JsonOps.INSTANCE,ret);
    });

    public static Codec<ReversionSituation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Reversion.CODEC.fieldOf("detail").forGetter(ReversionSituation::getReversionCase),
            CONDITION_CODEC.optionalFieldOf("condition", new ArrayList<>()).forGetter(ReversionSituation::getConditions),
            Codec.intRange(0,385 * 10 * 10).fieldOf("weight").forGetter(ReversionSituation::getWeight)).apply(instance, ReversionSituation::new));
    private final ReversionSituationResult reversionSituationResult;
    private final List<Condition> conditions;
    private final int weight;

    public ReversionSituation(ReversionSituationResult reversionSituationResult, List<Condition> conditions, int weight) {
        this.reversionSituationResult = reversionSituationResult;
        this.conditions = conditions;
        this.weight = weight;
    }

    boolean valid(LevelAccessor level, BlockPos blockPos) {
        for (var c : conditions) {
            if (!c.valid(level, blockPos)) return false;
        }
        return true;
    }

    public ReversionSituationResult getReversionCase() {
        return reversionSituationResult;
    }

    public int getWeight() {
        return weight;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public interface Condition {
        boolean valid(LevelAccessor level, BlockPos blockPos);
    }

    public static class HeightPredicate implements Condition {
        private final int limit;
        private final Type type;

        public HeightPredicate(JsonObject jsonObject) {
            this.limit = GsonHelper.getAsInt(jsonObject, "content");
            var t = GsonHelper.getAsString(jsonObject, "type");
            if (t.equals("less")) type = Type.LESS;
            else if (t.equals("greater")) type = Type.GREATER;
            else throw new JsonSyntaxException("Expected type to be \"less\" or \"greater\", was " + t);
        }

        @Override
        public boolean valid(LevelAccessor level, BlockPos blockPos) {
            return (type == Type.LESS) ? blockPos.getY() < limit : blockPos.getY() > limit;
        }


        public int getLimit() {
            return limit;
        }

        public Type getType() {
            return type;
        }

        public enum Type {
            LESS,
            GREATER
        }
    }

    public static class BiomePredicate implements Condition {
        private final List<Biome> biomeList;
        private final Type type;

        public BiomePredicate(JsonObject jsonObject) {
            String t = GsonHelper.getAsString(jsonObject, "type");
            if (t.equals("allowlist")) type = Type.ALLOWLIST;
            else if (t.equals("blocklist")) type = Type.BLOCKLIST;
            else throw new JsonSyntaxException("Expected type to be \"allowlist\" or \"blocklist\", was " + t);
            this.biomeList = new ArrayList<>();
            for (var jsonElement : GsonHelper.getAsJsonArray(jsonObject, "content")) {
                var bs = jsonElement.getAsString();
                var result = ForgeRegistries.BIOMES.getValue(new ResourceLocation(bs));
                if (result == null) {
                    throw new JsonSyntaxException("Invalid biome: " + bs);
                }
                biomeList.add(result);
            }
        }

        @Override
        public boolean valid(LevelAccessor level, BlockPos blockPos) {
            return (type == Type.ALLOWLIST) == biomeList.contains(level.getBiome(blockPos).value());
        }

        public List<Biome> getBiomeList() {
            return biomeList;
        }

        public Type getType() {
            return type;
        }

        public enum Type {
            ALLOWLIST,
            BLOCKLIST
        }
    }
}
