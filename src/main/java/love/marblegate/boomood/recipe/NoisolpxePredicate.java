package love.marblegate.boomood.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class NoisolpxePredicate {
    private final NoisolpxeSituation.Handler handler;
    private final List<NoisolpxeCondition> conditions;
    private final int weight;

    NoisolpxePredicate(JsonObject jsonObject) {
        conditions = new ArrayList<>();
        var conditionJson = jsonObject.getAsJsonObject("condition");
        if(conditionJson!=null){
            var heightConditionJson = conditionJson.getAsJsonObject("height");
            if(heightConditionJson!=null)
                conditions.add(new HeightPredicate(heightConditionJson));
            var biomeConditionJson = conditionJson.getAsJsonObject("biome");
            if(biomeConditionJson!=null)
                conditions.add(new BiomePredicate(biomeConditionJson));
        }
        weight = GsonHelper.getAsInt(jsonObject,"weight");
        handler = NoisolpxeSituation.Handler.create(jsonObject);
    }

    NoisolpxePredicate(NoisolpxeSituation.Handler handler, List<NoisolpxeCondition> conditions, int weight) {
        this.handler = handler;
        this.conditions = conditions;
        this.weight = weight;
    }

    boolean valid(LevelAccessor level, BlockPos blockPos){
        return true; //TODO
    }



    void toNetwork(FriendlyByteBuf packetBuffer){
        packetBuffer.writeInt(weight);
        handler.toNetwork(packetBuffer);
        packetBuffer.writeInt(conditions.size());
        for(var cd:conditions){
            cd.toNetwork(packetBuffer);
        }
    }

    static NoisolpxePredicate fromNetwork(FriendlyByteBuf packetBuffer) {
        var weight = packetBuffer.readInt();
        var handler = NoisolpxeSituation.Handler.fromNetwork(packetBuffer);
        var size = packetBuffer.readInt();
        List<NoisolpxeCondition> conditions = new ArrayList<>();
        for (int i = 0;i<size;i++){
            conditions.add(NoisolpxeCondition.fromNetwork(packetBuffer));
        }
        return new NoisolpxePredicate(handler,conditions,weight);
    }

    interface NoisolpxeCondition{
        boolean valid(LevelAccessor level, BlockPos blockPos);

        void toNetwork(FriendlyByteBuf packetBuffer);

        static NoisolpxeCondition fromNetwork(FriendlyByteBuf packetBuffer) {
            var st = packetBuffer.readByte();
            if(st==1) {
                var limit = packetBuffer.readInt();
                HeightPredicate.Type type;
                try{
                    type = packetBuffer.readEnum(HeightPredicate.Type.class);
                } catch(ArrayIndexOutOfBoundsException e){
                    throw new JsonSyntaxException("NoisolpxePredicate.NoisolpxeCondition#fromNetwork received bad packet. This causes recipe serialization issue. Invalid HeightPredicate type");
                }
                return new HeightPredicate(limit,type);
            }
            else if(st==2) {
                BiomePredicate.Type type;
                try{
                    type = packetBuffer.readEnum(BiomePredicate.Type.class);
                } catch(ArrayIndexOutOfBoundsException e){
                    throw new JsonSyntaxException("NoisolpxePredicate.NoisolpxeCondition#fromNetwork received bad packet. This causes recipe serialization issue. Invalid BiomePredicate type");
                }
                var size = packetBuffer.readInt();
                List<Biome> biomeList = new ArrayList<>();
                for (int i = 0;i<size;i++){
                    var rl = packetBuffer.readResourceLocation();
                    var biome = ForgeRegistries.BIOMES.getValue(rl);
                    if(biome ==null){
                        throw new JsonSyntaxException("NoisolpxeSituation.Handler#fromNetwork received bad packet. This causes recipe serialization issue. Invalid biome: " + rl);
                    }
                    biomeList.add(biome);
                }
                return new BiomePredicate(biomeList,type);
            }
            else throw new RuntimeException("NoisolpxePredicate.NoisolpxeCondition#fromNetwork received bad packet. This causes recipe serialization issue");
        }
    }

    private static class HeightPredicate implements NoisolpxeCondition{
        private final int limit;
        private final Type type;

        HeightPredicate(JsonObject jsonObject) {
            this.limit = GsonHelper.getAsInt(jsonObject,"content");
            var t = GsonHelper.getAsString(jsonObject,"type");
            if(t.equals("less")) type = Type.LESS;
            else if(t.equals("greater")) type = Type.GREATER;
            else throw new JsonSyntaxException("Expected type to be \"less\" or \"greater\", was " + t);
        }

        HeightPredicate(int limit, Type type) {
            this.limit = limit;
            this.type = type;
        }

        @Override
        public boolean valid(LevelAccessor level, BlockPos blockPos) {
            return (type == Type.LESS)? blockPos.getY()<limit: blockPos.getY()>limit;
        }

        @Override
        public void toNetwork(FriendlyByteBuf packetBuffer) {
            packetBuffer.writeByte(1);
            packetBuffer.writeInt(limit);
            packetBuffer.writeEnum(type);
        }

        private enum Type{
            LESS,
            GREATER
        }
    }

    private static class BiomePredicate implements NoisolpxeCondition{
        private final List<Biome> biomeList;
        private final Type type;

        BiomePredicate(JsonObject jsonObject) {
            String t = GsonHelper.getAsString(jsonObject,"type");
            if(t.equals("allowlist")) type = Type.ALLOWLIST;
            else if(t.equals("blocklist")) type = Type.BLOCKLIST;
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

        @Override
        public void toNetwork(FriendlyByteBuf packetBuffer) {
            packetBuffer.writeByte(2);
            packetBuffer.writeEnum(type);
            packetBuffer.writeInt(biomeList.size());
            for(var biome:biomeList){
                packetBuffer.writeResourceLocation(biome.getRegistryName());
            }
        }

        private enum Type{
            ALLOWLIST,
            BLOCKLIST
        }
    }
}
