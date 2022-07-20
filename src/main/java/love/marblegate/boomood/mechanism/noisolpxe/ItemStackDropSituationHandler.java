package love.marblegate.boomood.mechanism.noisolpxe;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import love.marblegate.boomood.config.Configuration;
import love.marblegate.boomood.misc.MiscUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public abstract class ItemStackDropSituationHandler {
    protected static Random RND = new Random();

    public static ItemStackDropSituationHandler createArmorHandler() {
        return new ArmorStandDestruction();
    }

    public static ItemStackDropSituationHandler createDefaultHandler() {
        return new ChestDestruction();
    }

    abstract void revert(Level level, BlockPos blockPos, List<ItemStack> itemStacks, Player manipulator);

    abstract void toNetwork(FriendlyByteBuf packetBuffer);

    abstract int priority();

    abstract List<List<ItemStack>> mergeItemStack(List<List<ItemStack>> itemStackListList);

    static ItemStackDropSituationHandler create(JsonObject jsonObject) {
        var st = GsonHelper.getAsString(jsonObject, "type");
        return switch (st) {
            case "entity_death" -> new EntityDeath(jsonObject);
            case "chest_destruction" -> new ChestDestruction();
            case "item_frame_destruction" -> new ItemFrameDestruction();
            case "armor_stand_destruction" -> new ArmorStandDestruction();
            case "block_destruction" -> new BlockDestruction(jsonObject);
            default -> throw new JsonSyntaxException("Expected type to be \"entity_death\", \"chest_destruction\", \"item_frame_destruction\", \"armor_stand_destruction\" or \"block_destruction\", was " + st);
        };
    }

    static ItemStackDropSituationHandler fromNetwork(FriendlyByteBuf packetBuffer) {
        var st = packetBuffer.readByte();
        if (st == 5) {
            var rl = packetBuffer.readResourceLocation();
            var entityType = ForgeRegistries.ENTITIES.getValue(rl);
            if (entityType == null) {
                throw new JsonSyntaxException("ItemStackDropSituationHandler#fromNetwork received bad packet. This causes recipe serialization issue. Invalid entity type: " + rl);
            }
            return new EntityDeath(entityType);
        } else if (st == 3) {
            return new ChestDestruction();
        } else if (st == 4) {
            return new ItemFrameDestruction();
        } else if (st == 2) {
            return new ArmorStandDestruction();
        } else if (st == 1) {
            var blockstateNbt = packetBuffer.readNbt();
            var hasNbt = packetBuffer.readBoolean();
            CompoundTag tags = null;
            if(hasNbt){
                 tags = packetBuffer.readNbt();
            }
            return new BlockDestruction(NbtUtils.readBlockState(blockstateNbt), tags);
        } else
            throw new RuntimeException("ItemStackDropSituationHandler#fromNetwork received bad packet. This causes recipe serialization issue");
    }

    static class BlockDestruction extends ItemStackDropSituationHandler {
        private final BlockState blockState;
        @Nullable private final CompoundTag tags;

        BlockDestruction(JsonObject jsonObject) {
            var bs = GsonHelper.getAsString(jsonObject, "block");
            var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(bs));
            if (block == null) {
                throw new JsonSyntaxException("Invalid block: " + bs);
            }
            if(jsonObject.has("nbt")){
                var nbtString = GsonHelper.getAsJsonObject(jsonObject, "nbt");
                try{
                    tags = TagParser.parseTag(nbtString.toString());
                } catch (CommandSyntaxException e) {
                    throw new JsonSyntaxException("Invalid block nbt: " + nbtString);
                }
            } else {
                tags = null;
            }
            if(jsonObject.has("property")){
                var propertyEntryJson = GsonHelper.getAsJsonObject(jsonObject, "property");
                if(propertyEntryJson.size()==0){
                    blockState = block.defaultBlockState();
                } else {
                    // Verify Property
                    for(var entry: propertyEntryJson.entrySet()){
                        var property = block.getStateDefinition().getProperty(entry.getKey());
                        if(property == null){
                            throw new JsonSyntaxException("Invalid block property key: " + entry.getKey());
                        }
                        Optional<? extends Comparable<?>> optional = property.getValue(entry.getValue().getAsString());
                        if(optional.isEmpty()){
                            throw new JsonSyntaxException("Invalid block property value: " + entry.getValue().getAsString());
                        }
                    }
                    var tempTag = new CompoundTag();
                    tempTag.putString("Name",bs);
                    try{
                        tempTag.put("Properties",TagParser.parseTag(propertyEntryJson.toString()));
                    } catch (CommandSyntaxException e) {
                        throw new JsonSyntaxException("Invalid block property: " + propertyEntryJson.toString());
                    }
                    blockState = NbtUtils.readBlockState(tempTag);
                }
            } else {
                blockState = block.defaultBlockState();
            }

        }

        BlockDestruction(BlockState blockState, @Nullable CompoundTag tags) {
            this.blockState = blockState;
            this.tags = tags;
        }

        @Override
        public void revert(Level level, BlockPos blockPos, List<ItemStack> itemStacks, Player manipulator) {
            var optional = MiscUtils.randomizeDestination(level,blockPos);
            if(optional.isEmpty()) return;
            var destination = optional.get();
            // Falling block should have support block
            if(blockState.getBlock() instanceof FallingBlock && level.getBlockState(destination.below()).is(Blocks.AIR)){
                level.setBlockAndUpdate(destination.below(), MiscUtils.getSupportBlock());
            }
            level.setBlockAndUpdate(destination,blockState);
            if(tags !=null){
                BlockEntity blockentity = level.getBlockEntity(destination);
                if (blockentity != null) {
                    blockentity.load(tags);
                }
            }
            // TODO add custom particle effect for indication & add implement explosion particle
        }

        @Override
        void toNetwork(FriendlyByteBuf packetBuffer) {
            packetBuffer.writeByte(1);
            packetBuffer.writeNbt(NbtUtils.writeBlockState(blockState));
            packetBuffer.writeBoolean(tags!=null);
            if(tags!=null){
                packetBuffer.writeNbt(tags);
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

    static class ArmorStandDestruction extends ItemStackDropSituationHandler {

        @Override
        public void revert(Level level, BlockPos blockPos, List<ItemStack> itemStacks, Player manipulator) {
            var optional = MiscUtils.randomizeDestination(level,blockPos);
            if(optional.isEmpty()) return;
            var destination = optional.get();
            // Armor stand should have support block
            if(level.getBlockState(destination.below()).is(Blocks.AIR)){
                level.setBlockAndUpdate(destination.below(), MiscUtils.getSupportBlock());
            }
            Vec3 vec3 = Vec3.atBottomCenterOf(destination);
            AABB aabb = EntityType.ARMOR_STAND.getDimensions().makeBoundingBox(vec3.x(), vec3.y(), vec3.z());
            if (level.noCollision((Entity)null, aabb) && level.getEntities((Entity)null, aabb).isEmpty()){
                ArmorStand armorStand = new ArmorStand(level,vec3.x,vec3.y,vec3.z);
                if(Configuration.NOISOLPXE_ARMOR_STAND_POSE_RANDOMIZE.get()){
                    armorStand.moveTo(armorStand.getX(), armorStand.getY(), armorStand.getZ(), (float) (Math.random() * 360), 0.0F);
                    MiscUtils.randomizeArmorStandPose(armorStand);
                }
                for(var itemStack: itemStacks){
                    EquipmentSlot equipmentslot = Mob.getEquipmentSlotForItem(itemStack);
                    armorStand.setItemSlot(equipmentslot,itemStack);
                }
                level.addFreshEntity(armorStand);
            }
            // TODO add custom particle effect for indication & add implement explosion particle
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

    static class ChestDestruction extends ItemStackDropSituationHandler {

        @Override
        public void revert(Level level, BlockPos blockPos, List<ItemStack> itemStacks, Player manipulator) {
            var optional = MiscUtils.randomizeDestination(level,blockPos);
            if(optional.isEmpty()) return;
            var destination = optional.get();
            var facings = ChestBlock.FACING.getAllValues().toList().stream().map(Property.Value::value).toList();
            level.setBlockAndUpdate(destination,Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,facings.get(new Random().nextInt(facings.size()))));
            BlockEntity chestBlockEntity = level.getBlockEntity(destination);
            var itemhandler = chestBlockEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if(itemhandler.isPresent()){
                itemhandler.ifPresent(cap->{
                    int i = 0;
                    for(var itemStack: itemStacks){
                        cap.insertItem(i, itemStack, false);
                        i++;
                    }
                });
            }
            // TODO add custom particle effect for indication & add implement explosion particle
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

    static class ItemFrameDestruction extends ItemStackDropSituationHandler {

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

    static class EntityDeath extends ItemStackDropSituationHandler {
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
