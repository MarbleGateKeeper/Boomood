package love.marblegate.boomood.mechanism.noisolpxe;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import love.marblegate.boomood.config.Configuration;
import love.marblegate.boomood.misc.MiscUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;

public abstract class ItemStackDropSituationHandler {
    protected static Random RND = new Random();

    public static ItemStackDropSituationHandler createArmorHandler() {
        return new ArmorStandDestruction();
    }

    public static ItemStackDropSituationHandler createDefaultHandler(ItemStack itemStack) {
        return new ChestDestruction(itemStack);
    }

    abstract void revert(Level level, BlockPos blockPos, List<ItemStack> itemStacks, Player manipulator);

    abstract int priority();

    abstract JsonObject toJson();

    abstract List<List<ItemStack>> mergeItemStack(List<List<ItemStack>> itemStackListList);

    static ItemStackDropSituationHandler create(JsonObject jsonObject) {
        var st = GsonHelper.getAsString(jsonObject, "type");
        return switch (st) {
            case "entity_death" -> new EntityDeath(jsonObject);
            case "chest_destruction" -> new ChestDestruction(jsonObject);
            case "item_frame_destruction" -> new ItemFrameDestruction(jsonObject);
            case "armor_stand_destruction" -> new ArmorStandDestruction();
            case "block_destruction" -> new BlockDestruction(jsonObject);
            default -> throw new JsonSyntaxException("Expected type to be \"entity_death\", \"chest_destruction\", \"item_frame_destruction\", \"armor_stand_destruction\" or \"block_destruction\", was " + st);
        };
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

        @Override
        public void revert(Level level, BlockPos blockPos, List<ItemStack> itemStacks, Player manipulator) {
            var optional = MiscUtils.randomizeDestination(level,blockPos);
            if(optional.isEmpty()) return;
            var destination = optional.get();
            // Falling block should have support block
            if(blockState.getBlock() instanceof FallingBlock && level.getBlockState(destination.below()).is(Blocks.AIR)){
                level.setBlockAndUpdate(destination.below(), MiscUtils.getSupportBlock(level,destination.below()));
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
        int priority() {
            return 100;
        }

        @Override
        JsonObject toJson() {
            var ret = new JsonObject();
            ret.addProperty("type","block_destruction");
            ret.addProperty("block",blockState.getBlock().getRegistryName().toString());
            CompoundTag propertiesTag = new CompoundTag();
            ImmutableMap<Property<?>, Comparable<?>> immutablemap = blockState.getValues();
            for(Map.Entry<Property<?>, Comparable<?>> entry : immutablemap.entrySet()) {
                Property<?> property = entry.getKey();
                propertiesTag.putString(property.getName(), getName(property, entry.getValue()));
            }
            var propertiesJson = NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, propertiesTag);
            ret.add("property",propertiesJson);
            if(tags!=null){
                var nbtJson = CompoundTag.CODEC.encodeStart(JsonOps.INSTANCE,tags);
                ret.add("tag",nbtJson.getOrThrow(false,err -> {
                    throw new JsonSyntaxException("Invalid block tag: " + nbtJson + ".Error: " + err);
                }));
            }
            return ret;
        }

        @Override
        public List<List<ItemStack>> mergeItemStack(List<List<ItemStack>> itemStackListList) {
            // For reverting block destruction, how itemStack arrangement is preprocessed is irrelevant.
            // So just return what is passed in.
            return itemStackListList;
        }

        // TODO 改用序列化BlockState的方法
        private static <T extends Comparable<T>> String getName(Property<T> p_129211_, Comparable<?> p_129212_) {
            return p_129211_.getName((T)p_129212_);
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
                level.setBlockAndUpdate(destination.below(), MiscUtils.getSupportBlock(level,destination.below()));
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
        int priority() {
            return 30;
        }

        @Override
        JsonObject toJson() {
            var ret = new JsonObject();
            ret.addProperty("type","armor_stand_destruction");
            return ret;
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
        private final ItemStack target;

        ChestDestruction(JsonObject jsonObject) {
            if(jsonObject.has("itemstack")) {
                var itemStackJson = GsonHelper.getAsJsonObject(jsonObject, "itemstack");
                var itemStackResult = ItemStack.CODEC.parse(JsonOps.INSTANCE, itemStackJson);
                target = itemStackResult.getOrThrow(false, err -> {
                    throw new JsonSyntaxException("Invalid itemstack nbt: " + itemStackJson + ".Error: " + err);
                });
            }
            else target = null;
        }

        public ChestDestruction(ItemStack target) {
            this.target = target;
        }

        @Override
        public void revert(Level level, BlockPos blockPos, List<ItemStack> itemStacks, Player manipulator) {
            var insertItemStack = target==null? itemStacks.get(0): target;
            insertItemStack = MiscUtils.searchValidChestAndInsert(level,blockPos,insertItemStack);
            if(!insertItemStack.isEmpty()){
                var optional = MiscUtils.randomizeDestination(level,blockPos);
                if(optional.isEmpty()) return;
                var destination = optional.get();
                var facings = ChestBlock.FACING.getAllValues().toList().stream().map(Property.Value::value).toList();
                level.setBlockAndUpdate(destination,Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,facings.get(new Random().nextInt(facings.size()))));
                BlockEntity chestBlockEntity = level.getBlockEntity(destination);
                var itemhandler = chestBlockEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
                if(itemhandler.isPresent()){
                    ItemStack finalInsertItemStack = insertItemStack;
                    itemhandler.ifPresent(cap->{
                        cap.insertItem(0, finalInsertItemStack, false);
                    });
                }
            }
            // TODO add custom particle effect for indication & add implement explosion particle
        }


        @Override
        int priority() {
            return 50;
        }

        @Override
        JsonObject toJson() {
            var ret = new JsonObject();
            ret.addProperty("type","chest_destruction");
            if(target!=null){
                ret.add("itemstack",ItemStack.CODEC.encodeStart(JsonOps.INSTANCE,target).get().left().get());
            }
            return ret;
        }

        @Override
        List<List<ItemStack>> mergeItemStack(List<List<ItemStack>> itemStackListList) {
            // No need to merge
            return itemStackListList;
        }
    }

    static class ItemFrameDestruction extends ItemStackDropSituationHandler {

        @Nullable private final ItemStack target;

        ItemFrameDestruction(JsonObject jsonObject) {
            if(jsonObject.has("itemstack")) {
                var itemStackJson = GsonHelper.getAsJsonObject(jsonObject, "itemstack");
                var itemStackResult = ItemStack.CODEC.parse(JsonOps.INSTANCE, itemStackJson);
                target = itemStackResult.getOrThrow(false, err -> {
                    throw new JsonSyntaxException("Invalid block nbt: " + itemStackJson + ".Error: " + err);
                });
            }
            else target = null;
        }

        @Override
        public void revert(Level level, BlockPos blockPos, List<ItemStack> itemStacks, Player manipulator) {
            var isGlowItemFrame = Math.random() < Configuration.NOISOLPXE_GLOW_ITEM_FRAME_POSSIBILITY.get();
            var blockPosList = MiscUtils.createBottomToTopBlockPosList(MiscUtils.createScanningArea(blockPos));
            ItemFrame itemFrame;
            var displayItemStack = target==null? itemStacks.get(0): target;
            for(var bp:blockPosList){
                for(var direction: Direction.values()){
                    if(isGlowItemFrame) itemFrame = new GlowItemFrame(level, bp, direction);
                    else itemFrame = new ItemFrame(level, bp, direction);
                    if(itemFrame.survives()) {
                        itemFrame.setItem(displayItemStack);
                        level.addFreshEntity(itemFrame);
                        displayItemStack = ItemStack.EMPTY;
                        // TODO add custom particle effect for indication & add implement explosion particle
                        break;
                    }
                }
                if(displayItemStack.isEmpty()) break;
            }
            if(!displayItemStack.isEmpty()){
                if(Configuration.NOISOLPXE_ITEM_FRAME_SITUATION_REMEDY_IS_SUPPORT_BLOCK.get()){
                    var tryTime = 0;
                    while(tryTime<3){
                        var optional = MiscUtils.randomizeDestination(level,blockPos);
                        if(optional.isEmpty()) return;
                        var destination = optional.get();
                        var od = getEmptyNeighborDirection(level, destination);
                        if(od.isEmpty()){
                            tryTime++;
                        }else{
                            level.setBlockAndUpdate(destination.relative(od.get()),MiscUtils.getSupportBlock(level,destination.relative(od.get())));
                            if(isGlowItemFrame) itemFrame = new GlowItemFrame(level, destination, od.get().getOpposite());
                            else itemFrame = new ItemFrame(level, destination, od.get().getOpposite());
                            itemFrame.setItem(displayItemStack);
                            level.addFreshEntity(itemFrame);
                            // TODO add custom particle effect for indication & add implement explosion particle
                            break;
                        }
                    }
                } else {
                    tryPutIntoChest(level,blockPos,displayItemStack);
                }
            }
        }


        private Optional<Direction> getEmptyNeighborDirection(Level level, BlockPos blockPos){
            for(var direction: Direction.values()){
                if(level.getBlockState(blockPos.relative(direction)).getBlock().equals(Blocks.AIR)) {
                    return Optional.of(direction);
                }
            }
            return Optional.empty();
        }

        private void tryPutIntoChest(Level level, BlockPos blockPos, ItemStack itemStack){
            MiscUtils.searchValidChestAndInsert(level, blockPos, itemStack);
            if(!itemStack.isEmpty()){
                var optional = MiscUtils.randomizeDestination(level,blockPos);
                if(optional.isEmpty()) return;
                var destination = optional.get();
                var facings = ChestBlock.FACING.getAllValues().toList().stream().map(Property.Value::value).toList();
                level.setBlockAndUpdate(destination,Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,facings.get(new Random().nextInt(facings.size()))));
                BlockEntity chestBlockEntity = level.getBlockEntity(destination);
                var itemhandler = chestBlockEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
                if(itemhandler.isPresent()){
                    itemhandler.ifPresent(cap-> cap.insertItem(0, itemStack, false));
                }
                // TODO add custom particle effect for indication & add implement explosion particle
            }
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

        @Override
        JsonObject toJson() {
            var ret = new JsonObject();
            ret.addProperty("type","item_frame_destruction");
            if(target!=null){
                ret.add("itemstack",ItemStack.CODEC.encodeStart(JsonOps.INSTANCE,target).get().left().get());
            }
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
        int priority() {
            return 0;
        }

        @Override
        public List<List<ItemStack>> mergeItemStack(List<List<ItemStack>> itemStackListList) {
            // For reverting entity death, how itemStack arrangement is preprocessed is irrelevant.
            // So just return what is passed in.
            return itemStackListList;
        }

        @Override
        JsonObject toJson() {
            var ret = new JsonObject();
            ret.addProperty("type","entity_death");
            ret.addProperty("entity",entityType.getRegistryName().toString());
            return ret;
        }
    }
}
