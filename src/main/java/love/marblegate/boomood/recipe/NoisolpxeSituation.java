package love.marblegate.boomood.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class NoisolpxeSituation {
    private final Handler handler;
    private final List<ItemStack> items;

    private NoisolpxeSituation(Handler handler, List<ItemStack> items) {
        this.handler = handler;
        this.items = items;
    }

    public static NoisolpxeSituation create(Handler handler, List<ItemStack> items){
        return new NoisolpxeSituation(handler, items);
    }

    public static Handler defaultArmorHandler(){
        return new ArmorStandDestruction();
    }

    public static Handler defaultHandler(){
        return new ChestDestruction();
    }

    public Handler getHandler() {
        return handler;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    @Override
    public String toString() {
        return "NoisolpxeSituation{" +
                "handler=" + handler +
                ", items=" + items +
                '}';
    }

    public static abstract class Handler {
        abstract void revert(LevelAccessor level, BlockPos blockPos, List<ItemStack> itemStacks, Player manipulator);

        abstract void toNetwork(FriendlyByteBuf packetBuffer);

        static Handler create(JsonObject jsonObject){
            var st = GsonHelper.getAsString(jsonObject,"type");
            if(st.equals("entity_death")) {
                return new EntityDeath(jsonObject);
            }
            else if(st.equals("chest_destruction")) {
                return new ChestDestruction();
            }
            else if(st.equals("item_frame_destruction")) {
                return new ItemFrameDestruction();
            }
            else if(st.equals("armor_stand_destruction")) {
                return new ArmorStandDestruction();
            }
            else if(st.equals("block_destruction")) {
                return new BlockDestruction(jsonObject);
            }
            else throw new JsonSyntaxException("Expected type to be \"entity_death\", \"chest_destruction\", \"item_frame_destruction\", \"armor_stand_destruction\" or \"block_destruction\", was " + st);
        }

        static Handler fromNetwork(FriendlyByteBuf packetBuffer){
            var st = packetBuffer.readByte();
            if(st==1) {
                var rl = packetBuffer.readResourceLocation();
                var entityType = ForgeRegistries.ENTITIES.getValue(rl);
                if(entityType ==null){
                    throw new JsonSyntaxException("NoisolpxeSituation.Handler#fromNetwork received bad packet. This causes recipe serialization issue. Invalid entity type: " + rl);
                }
                return new EntityDeath(entityType);
            }
            else if(st==2) {
                return new ChestDestruction();
            }
            else if(st==3) {
                return new ItemFrameDestruction();
            }
            else if(st==4) {
                return new ArmorStandDestruction();
            }
            else if(st==5) {
                var rl = packetBuffer.readResourceLocation();
                var block = ForgeRegistries.BLOCKS.getValue(rl);
                if(block ==null){
                    throw new JsonSyntaxException("NoisolpxeSituation.Handler#fromNetwork received bad packet. This causes recipe serialization issue. Invalid block: " + rl);
                }
                return new BlockDestruction(block);
            }
            else throw new RuntimeException("NoisolpxeSituation.Handler#fromNetwork received bad packet. This causes recipe serialization issue");
        }
    }

    private static class BlockDestruction extends Handler {
        private final Block block;

        BlockDestruction(JsonObject jsonObject) {
            var bs = GsonHelper.getAsString(jsonObject,"block");
            block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(bs));
            if(block==null){
                throw new JsonSyntaxException("Invalid block: " + bs);
            }
        }

        BlockDestruction(Block block) {
            this.block = block;
        }

        @Override
        public void revert(LevelAccessor level, BlockPos blockPos, List<ItemStack> itemStacks, Player manipulator) {
            //TODO
        }

        @Override
        void toNetwork(FriendlyByteBuf packetBuffer) {
            packetBuffer.writeByte(1);
            packetBuffer.writeResourceLocation(block.getRegistryName());
        }
    }

    private static class ArmorStandDestruction extends Handler {

        @Override
        public void revert(LevelAccessor level, BlockPos blockPos, List<ItemStack> itemStacks, Player manipulator) {
            //TODO
        }

        @Override
        void toNetwork(FriendlyByteBuf packetBuffer) {
            packetBuffer.writeByte(2);
        }
    }

    private static class ChestDestruction extends Handler {

        @Override
        public void revert(LevelAccessor level, BlockPos blockPos, List<ItemStack> itemStacks, Player manipulator) {
            //TODO
        }

        @Override
        void toNetwork(FriendlyByteBuf packetBuffer) {
            packetBuffer.writeByte(3);
        }
    }

    private static class ItemFrameDestruction extends Handler {

        @Override
        public void revert(LevelAccessor level, BlockPos blockPos, List<ItemStack> itemStacks, Player manipulator) {
            //TODO
        }

        @Override
        void toNetwork(FriendlyByteBuf packetBuffer) {
            packetBuffer.writeByte(4);
        }
    }


    private static class EntityDeath extends Handler {
        private final EntityType<?> entityType;

        EntityDeath(JsonObject jsonObject) {
            var es = GsonHelper.getAsString(jsonObject,"entity");
            entityType = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(es));
            if(entityType ==null){
                throw new JsonSyntaxException("Invalid entity type: " + es);
            }
        }

        EntityDeath(EntityType<?> entityType) {
            this.entityType = entityType;
        }

        @Override
        public void revert(LevelAccessor level, BlockPos blockPos, List<ItemStack> itemStacks, Player manipulator) {
            //TODO
        }

        @Override
        void toNetwork(FriendlyByteBuf packetBuffer) {
            packetBuffer.writeByte(5);
            packetBuffer.writeResourceLocation(entityType.getRegistryName());
        }
    }
}
