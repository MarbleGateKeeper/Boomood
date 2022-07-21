package love.marblegate.boomood.mechanism.situation.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import love.marblegate.boomood.mechanism.situation.handler.ItemStackDropSituationHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class ItemStackEntityRevertPredicate {
    private final ItemStackDropSituationHandler handler;
    private final List<Condition> conditions;
    private final int weight;

    public ItemStackEntityRevertPredicate(ItemStackDropSituationHandler handler, List<Condition> conditions, int weight) {
        this.handler = handler;
        this.conditions = conditions;
        this.weight = weight;
    }

    boolean valid(LevelAccessor level, BlockPos blockPos) {
        for (var c : conditions) {
            if (!c.valid(level, blockPos)) return false;
        }
        return true;
    }

    public ItemStackDropSituationHandler getHandler() {
        return handler;
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

        HeightPredicate(int limit, Type type) {
            this.limit = limit;
            this.type = type;
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

        public BiomePredicate(List<Biome> biomeList, Type type) {
            this.biomeList = biomeList;
            this.type = type;
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
