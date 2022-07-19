package love.marblegate.boomood.mechanism.noisolpxe;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

class NoisolpxeItemStackDropSituation implements NoisolpxeSituation {
    private final Handler handler;
    private final List<ItemStack> items;

    NoisolpxeItemStackDropSituation(Handler handler, List<ItemStack> items) {
        this.handler = handler;
        this.items = items;
    }

    static NoisolpxeItemStackDropSituation create(Handler handler, List<ItemStack> items) {
        return new NoisolpxeItemStackDropSituation(handler, items);
    }

    Handler getHandler() {
        return handler;
    }

    List<ItemStack> getItems() {
        return items;
    }

    @Override
    public void revert(Level level, BlockPos blockPos, Player manipulator) {
        handler.revert(level, blockPos, items, manipulator);
    }

    @Override
    public String toString() {
        return "Situation{" + "handler=" + handler + ", items=" + items + "}\n";
    }


    public static abstract class Handler {

        public static Handler createArmorHandler() {
            return new ArmorStandDestruction();
        }

        public static Handler createDefaultHandler() {
            return new ChestDestruction();
        }

        abstract void revert(LevelAccessor level, BlockPos blockPos, List<ItemStack> itemStacks, Player manipulator);

        abstract void toNetwork(FriendlyByteBuf packetBuffer);

        abstract List<List<ItemStack>> mergeItemStack(List<List<ItemStack>> itemStackListList);

        static Handler create(JsonObject jsonObject) {
            var st = GsonHelper.getAsString(jsonObject, "type");
            return switch (st) {
                case "entity_death" -> new EntityDeath(jsonObject);
                case "chest_destruction" -> new ChestDestruction();
                case "item_frame_destruction" -> new ItemFrameDestruction();
                case "armor_stand_destruction" -> new ArmorStandDestruction();
                case "block_destruction" -> new BlockDestruction(jsonObject);
                default ->
                        throw new JsonSyntaxException("Expected type to be \"entity_death\", \"chest_destruction\", \"item_frame_destruction\", \"armor_stand_destruction\" or \"block_destruction\", was " + st);
            };
        }

        static Handler fromNetwork(FriendlyByteBuf packetBuffer) {
            var st = packetBuffer.readByte();
            if (st == 1) {
                var rl = packetBuffer.readResourceLocation();
                var entityType = ForgeRegistries.ENTITIES.getValue(rl);
                if (entityType == null) {
                    throw new JsonSyntaxException("NoisolpxeSituation.Handler#fromNetwork received bad packet. This causes recipe serialization issue. Invalid entity type: " + rl);
                }
                return new EntityDeath(entityType);
            } else if (st == 2) {
                return new ChestDestruction();
            } else if (st == 3) {
                return new ItemFrameDestruction();
            } else if (st == 4) {
                return new ArmorStandDestruction();
            } else if (st == 5) {
                var rl = packetBuffer.readResourceLocation();
                var block = ForgeRegistries.BLOCKS.getValue(rl);
                if (block == null) {
                    throw new JsonSyntaxException("NoisolpxeSituation.Handler#fromNetwork received bad packet. This causes recipe serialization issue. Invalid block: " + rl);
                }
                return new BlockDestruction(block);
            } else
                throw new RuntimeException("NoisolpxeSituation.Handler#fromNetwork received bad packet. This causes recipe serialization issue");
        }
    }

    private static class BlockDestruction extends Handler {
        private final Block block;

        BlockDestruction(JsonObject jsonObject) {
            var bs = GsonHelper.getAsString(jsonObject, "block");
            block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(bs));
            if (block == null) {
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

        @Override
        public List<List<ItemStack>> mergeItemStack(List<List<ItemStack>> itemStackListList) {
            // For reverting block destruction, how itemStack arrangement is preprocessed is irrelevant.
            // So just return what is passed in.
            return itemStackListList;
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

        @Override
        public List<List<ItemStack>> mergeItemStack(List<List<ItemStack>> itemStackListList) {
            // Every armorStand can hold only 1 suit of armor.
            List<List<ItemStack>> ret = new ArrayList<>();
            var flatItemStackList = itemStackListList.stream().reduce(new ArrayList<>(), (list1, list2) -> {
                list1.addAll(list2);
                return list1;
            });
            var helmets = flatItemStackList.stream().filter(itemStack -> ((ArmorItem) itemStack.getItem()).getSlot() == EquipmentSlot.HEAD).toList();
            var chestplates = flatItemStackList.stream().filter(itemStack -> ((ArmorItem) itemStack.getItem()).getSlot() == EquipmentSlot.CHEST).toList();
            var leggings = flatItemStackList.stream().filter(itemStack -> ((ArmorItem) itemStack.getItem()).getSlot() == EquipmentSlot.LEGS).toList();
            var boots = flatItemStackList.stream().filter(itemStack -> ((ArmorItem) itemStack.getItem()).getSlot() == EquipmentSlot.FEET).toList();
            var count = Math.max(Math.max(helmets.size(), chestplates.size()), Math.max(leggings.size(), boots.size()));

            for (int i = 0; i < count; i++) {
                List<ItemStack> suit = new ArrayList<>();
                if (i < helmets.size()) suit.add(helmets.get(i));
                if (i < chestplates.size()) suit.add(chestplates.get(i));
                if (i < leggings.size()) suit.add(leggings.get(i));
                if (i < boots.size()) suit.add(boots.get(i));
                ret.add(suit);
            }
            return ret;
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

        @Override
        List<List<ItemStack>> mergeItemStack(List<List<ItemStack>> itemStackListList) {
            // Every single chest can hold only 27 stack of Item.
            List<List<ItemStack>> ret = new ArrayList<>();
            var container = new SimpleContainer(27);
            itemStackListList.forEach(itemStackList -> itemStackList.forEach(itemStack -> {
                if (container.canAddItem(itemStack)) {
                    container.addItem(itemStack);
                } else {
                    ret.add(container.removeAllItems());
                    container.addItem(itemStack);
                }
            }));
            if (!container.isEmpty()) {
                ret.add(container.removeAllItems());
            }
            return ret;
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

        @Override
        public List<List<ItemStack>> mergeItemStack(List<List<ItemStack>> itemStackListList) {
            // Every itemFrame can hold only 1 Item.
            List<List<ItemStack>> ret = new ArrayList<>();
            itemStackListList.forEach(itemStackList -> itemStackList.forEach(itemStack -> {
                var givenItemStack = itemStack.copy();
                var amount = itemStack.getCount();
                givenItemStack.setCount(1);
                for (int i = 0; i < amount; i++) {
                    var putIn = Lists.newArrayList(givenItemStack);
                    ret.add(putIn);
                }
            }));
            return ret;
        }
    }


    private static class EntityDeath extends Handler {
        private final EntityType<?> entityType;

        EntityDeath(JsonObject jsonObject) {
            var es = GsonHelper.getAsString(jsonObject, "entity");
            entityType = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(es));
            if (entityType == null) {
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

        @Override
        public List<List<ItemStack>> mergeItemStack(List<List<ItemStack>> itemStackListList) {
            // For reverting entity death, how itemStack arrangement is preprocessed is irrelevant.
            // So just return what is passed in.
            return itemStackListList;
        }
    }

    public static class SituationSet extends ArrayList<NoisolpxeItemStackDropSituation> {

        public void revert(Level level, BlockPos pos, Player player) {
            for (var situation : this) {
                situation.revert(level, pos, player);
            }
            // TODO this is for debug only, Removal is needed.
            System.out.println(this);
        }

        public static class Raw extends SituationSet {

            public SituationSet merge() {
                Multimap<Handler, List<ItemStack>> mergeMap = MultimapBuilder.hashKeys().arrayListValues().build();
                SituationSet ret = new SituationSet();
                for (var situation : this) {
                    mergeMap.put(situation.getHandler(), situation.getItems());
                }
                for (var key : mergeMap.keySet()) {
                    var unmerged = mergeMap.get(key).stream().toList();
                    for (var merged : key.mergeItemStack(unmerged)) {
                        ret.add(new NoisolpxeItemStackDropSituation(key, merged));
                    }
                }
                return ret;
            }
        }
    }
}
