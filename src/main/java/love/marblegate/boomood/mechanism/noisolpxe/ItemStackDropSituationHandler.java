package love.marblegate.boomood.mechanism.noisolpxe;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import love.marblegate.boomood.config.Configuration;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;

public abstract class NoisolpxeItemStackDropSituationHandler {
    protected static Random RND = new Random();

    public static NoisolpxeItemStackDropSituationHandler createArmorHandler() {
        return new ArmorStandDestruction();
    }

    public static NoisolpxeItemStackDropSituationHandler createDefaultHandler() {
        return new ChestDestruction();
    }

    abstract void revert(Level level, BlockPos blockPos, List<ItemStack> itemStacks, Player manipulator);

    abstract void toNetwork(FriendlyByteBuf packetBuffer);

    abstract int priority();

    abstract List<List<ItemStack>> mergeItemStack(List<List<ItemStack>> itemStackListList);

    static NoisolpxeItemStackDropSituationHandler create(JsonObject jsonObject) {
        var st = GsonHelper.getAsString(jsonObject, "type");
        return switch (st) {
            case "entity_death" -> new EntityDeath(jsonObject);
            case "chest_destruction" -> new ChestDestruction();
            case "item_frame_destruction" -> new ItemFrameDestruction();
            case "armor_stand_destruction" -> new ArmorStandDestruction();
            case "block_destruction" -> new BlockDestruction(jsonObject, properties, tag);
            default -> throw new JsonSyntaxException("Expected type to be \"entity_death\", \"chest_destruction\", \"item_frame_destruction\", \"armor_stand_destruction\" or \"block_destruction\", was " + st);
        };
    }

    static NoisolpxeItemStackDropSituationHandler fromNetwork(FriendlyByteBuf packetBuffer) {
        var st = packetBuffer.readByte();
        if (st == 1) {
            var rl = packetBuffer.readResourceLocation();
            var entityType = ForgeRegistries.ENTITIES.getValue(rl);
            if (entityType == null) {
                throw new JsonSyntaxException("NoisolpxeItemStackDropSituationHandler#fromNetwork received bad packet. This causes recipe serialization issue. Invalid entity type: " + rl);
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
                throw new JsonSyntaxException("NoisolpxeItemStackDropSituationHandler#fromNetwork received bad packet. This causes recipe serialization issue. Invalid block: " + rl);
            }
            return new BlockDestruction(block, properties, tag);
        } else
            throw new RuntimeException("NoisolpxeItemStackDropSituationHandler#fromNetwork received bad packet. This causes recipe serialization issue");
    }

    static class BlockDestruction extends NoisolpxeItemStackDropSituationHandler {
        private final Block block;
        @Nullable private final Set<Property.Value<? extends Comparable<?>>> properties;
        @Nullable private final CompoundTag tags;

        BlockDestruction(JsonObject jsonObject) {
            var bs = GsonHelper.getAsString(jsonObject, "block");
            block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(bs));
            if (block == null) {
                throw new JsonSyntaxException("Invalid block: " + bs);
            }
            if(jsonObject.has("nbt")){
                var nbtString = GsonHelper.getAsString(jsonObject, "nbt");
                try{
                    tags = TagParser.parseTag(nbtString);
                } catch (CommandSyntaxException e) {
                    throw new JsonSyntaxException("Invalid block nbt: " + nbtString);
                }
            } else {
                tags = null;
            }
            if(jsonObject.has("property")){
                properties = new HashSet<>();
                for(var entry: GsonHelper.getAsJsonObject(jsonObject, "property").entrySet()){
                    var property = block.getStateDefinition().getProperty(entry.getKey());
                    if(property == null){
                        throw new JsonSyntaxException("Invalid block property key: " + entry.getKey());
                    }
                    Optional<? extends Comparable<?>> optional = property.getValue(entry.getValue().getAsString());
                    if(optional.isEmpty()){
                        throw new JsonSyntaxException("Invalid block property value: " + entry.getValue().getAsString());
                    }
                    properties.add(property.value(optional.get()));
                }
            } else {
                properties = null;
            }

        }

        BlockDestruction(Block block, @Nullable Map<Property<?>, Comparable<?>> properties, @Nullable CompoundTag tags) {
            this.block = block;
            this.properties = properties;
            this.tags = tags;
        }

        @Override
        public void revert(Level level, BlockPos blockPos, List<ItemStack> itemStacks, Player manipulator) {
            var offset = Configuration.NOISOLPXE_HORIZONTAL_RADIUS.get();
            var tryTime = 0;
            var suggest_height_limit = blockPos.getY() + Configuration.NOISOLPXE_SUGGESTED_VERTICAL_HEIGHT.get();
            var destination = blockPos.east((int) Math.round(offset * RND.nextGaussian(0,0.334))).south((int) Math.round(offset * RND.nextGaussian(0,0.334)));
            while(!level.getBlockState(destination).is(Blocks.AIR)){
                destination = destination.above();
                if(tryTime<3 && destination.getY()>suggest_height_limit){
                    destination = blockPos.east((int) Math.round(offset * RND.nextGaussian(0,0.334))).south((int) Math.round(offset * RND.nextGaussian(0,0.334)));
                    tryTime ++;
                    continue;
                }
                // Height Limit
                if(destination.getY()>256){
                   break; // Give the f**k up
                }
            }
            // Falling block should have support block
            if(block instanceof FallingBlock && level.getBlockState(destination.below()).is(Blocks.AIR)){
                // TODO what is good for support block?
                level.setBlockAndUpdate(destination.below(),Blocks.GLASS.defaultBlockState());
            }
            level.setBlockAndUpdate(destination,block.defaultBlockState());
            if(tags !=null){
                BlockEntity blockentity = level.getBlockEntity(destination);
                if (blockentity != null) {
                    blockentity.load(tags);
                }
            }
            // TODO custom add particle effect for indication & add implement explosion particle
        }

        @Override
        void toNetwork(FriendlyByteBuf packetBuffer) {
            packetBuffer.writeByte(1);
            packetBuffer.writeResourceLocation(block.getRegistryName());
            packetBuffer.writeBoolean(tags!=null);
            if(tags!=null){
                packetBuffer.writeNbt(tags);
            }
            packetBuffer.writeBoolean(properties!=null);
            if(properties!=null){
                for(var entry:properties.entrySet()){
                    packetBuffer.writeWithCodec((Codec<Property<? extends Comparable<?>>>) entry.getKey().codec(),entry.getKey());
                    packetBuffer.<Property.Value<?>>writeWithCodec(entry.getKey().valueCodec(),entry.getValue());
                }
            }
        }

        @Override
        int priority() {
            return 100;
        }

        @Override
        public List<List<ItemStack>> mergeItemStack(List<List<ItemStack>> itemStackListList) {
            // For reverting block destruction, how itemStack arrangement is preprocessed is irrelevant.
            // So just return what is passed in.
            return itemStackListList;
        }
    }

    static class ArmorStandDestruction extends NoisolpxeItemStackDropSituationHandler {

        @Override
        public void revert(Level level, BlockPos blockPos, List<ItemStack> itemStacks, Player manipulator) {
            //TODO
        }

        @Override
        void toNetwork(FriendlyByteBuf packetBuffer) {
            packetBuffer.writeByte(2);
        }

        @Override
        int priority() {
            return 30;
        }

        @Override
        public List<List<ItemStack>> mergeItemStack(List<List<ItemStack>> itemStackListList) {
            // Every armorStand can hold only 1 suit of armor.
            List<List<ItemStack>> ret = new ArrayList<>();
            var flatItemStackList = itemStackListList.stream().reduce(new ArrayList<>(), (list1, list2) -> {
                list1.addAll(list2);
                return list1;
            }).stream().filter(itemStack ->itemStack.getItem() instanceof ArmorItem).toList();
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

    static class ChestDestruction extends NoisolpxeItemStackDropSituationHandler {

        @Override
        public void revert(Level level, BlockPos blockPos, List<ItemStack> itemStacks, Player manipulator) {
            //TODO
        }

        @Override
        void toNetwork(FriendlyByteBuf packetBuffer) {
            packetBuffer.writeByte(3);
        }

        @Override
        int priority() {
            return 50;
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

    static class ItemFrameDestruction extends NoisolpxeItemStackDropSituationHandler {

        @Override
        public void revert(Level level, BlockPos blockPos, List<ItemStack> itemStacks, Player manipulator) {
            //TODO
        }

        @Override
        void toNetwork(FriendlyByteBuf packetBuffer) {
            packetBuffer.writeByte(4);
        }

        @Override
        int priority() {
            return 40;
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

    static class EntityDeath extends NoisolpxeItemStackDropSituationHandler {
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
        public void revert(Level level, BlockPos blockPos, List<ItemStack> itemStacks, Player manipulator) {
            //TODO
        }

        @Override
        void toNetwork(FriendlyByteBuf packetBuffer) {
            packetBuffer.writeByte(5);
            packetBuffer.writeResourceLocation(entityType.getRegistryName());
        }

        @Override
        int priority() {
            return 0;
        }

        @Override
        public List<List<ItemStack>> mergeItemStack(List<List<ItemStack>> itemStackListList) {
            // For reverting entity death, how itemStack arrangement is preprocessed is irrelevant.
            // So just return what is passed in.
            return itemStackListList;
        }
    }
}