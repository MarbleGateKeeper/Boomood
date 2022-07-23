package love.marblegate.boomood.misc;

import com.google.common.collect.HashMultimap;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import love.marblegate.boomood.config.Configuration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.Rotations;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.items.CapabilityItemHandler;
import org.apache.commons.compress.utils.Lists;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class MiscUtils {
    public static Codec<ItemStack> ITEMSTACK_CODEC = Codec.PASSTHROUGH.comapFlatMap(dynamic -> {
        var json = dynamic.convert(JsonOps.INSTANCE).getValue().getAsJsonObject();
        Item item;
        if(json.has("id"))
            item = Registry.ITEM.byNameCodec().parse(JsonOps.INSTANCE,json.getAsJsonPrimitive("id")).getOrThrow(false, err->{
                throw new JsonSyntaxException(err);
            });
        else return DataResult.error("id field is missing! Json representing ItemStack must has a id field which has a Resourcelocation of Item in it.");
        var ret = new ItemStack(item);
        var count = 1;
        if(json.has("Count")){
            try{
                count = json.getAsJsonPrimitive("Count").getAsInt();
                if(ret.getMaxStackSize()<count && count<1)
                    return DataResult.error("count field is invalid! Given item cannot be stacked to this amount in single ItemStack!");
                else ret.setCount(count);
            } catch(NumberFormatException | ClassCastException e){
                throw new JsonSyntaxException("Count field is invalid! it must be a integer!");
            }
        }
        CompoundTag tag;
        if(json.has("tag")) {
            try {
                tag = TagParser.parseTag(json.getAsJsonPrimitive("tag").getAsString());
                ret.setTag(tag);
            } catch (CommandSyntaxException e) {
                throw new JsonSyntaxException("tag field is invalid! it cannot be parsed into nbt!");
            } catch(ClassCastException e){
                throw new JsonSyntaxException("tag field is invalid! it must be a String!");
            }
        }
        return DataResult.success(ret);
    },itemStack -> {
        var ret = new JsonObject();
        ret.addProperty("id",itemStack.getItem().getRegistryName().toString());
        ret.addProperty("Count",itemStack.getCount());
        if(itemStack.getTag()!=null){
            ret.addProperty("nbt",itemStack.getTag().getAsString());
        }
        return new Dynamic<>(JsonOps.INSTANCE,ret);
    });

    public static BlockPos findLookAt(Player player) {
        HitResult hitResult = player.pick(128.0D, 0.0F, false);
        return ((BlockHitResult) hitResult).getBlockPos();
    }

    public static AABB createScanningArea(BlockPos blockPos){
        return new AABB(blockPos)
                .expandTowards(Configuration.Common.RADIUS.get(), Configuration.Common.RADIUS.get(), Configuration.Common.RADIUS.get())
                .expandTowards(-Configuration.Common.RADIUS.get(), 0, -Configuration.Common.RADIUS.get());
    }

    public static AABB createRemedyScanningArea(Level level, BlockPos blockPos){
        if(Configuration.Common.REMEDY_TYPE.get().goUpward())
            return new AABB(blockPos)
                .expandTowards(Configuration.Common.RADIUS.get(), level.getMaxBuildHeight(), Configuration.Common.RADIUS.get())
                .expandTowards(-Configuration.Common.RADIUS.get(), 0, -Configuration.Common.RADIUS.get());
        else return new AABB(blockPos)
                .expandTowards(Configuration.Common.RADIUS.get() * 2, Configuration.Common.RADIUS.get() * 2,Configuration.Common.RADIUS.get() * 2)
                .expandTowards(-Configuration.Common.RADIUS.get() * 2, 0, -Configuration.Common.RADIUS.get() * 2);
    }

    public static Optional<BlockPos> randomizeDestination(Level level, BlockPos blockPos){
        // TODO algorithm still need to be redesign. all space need to be fixed first? Maybe?
        var random = new Random();
        var radius = Configuration.Common.RADIUS.get();
        var tryTime = 0;
        var destination = blockPos.east((int) Math.round(radius * random.nextGaussian(0,0.334))).south((int) Math.round(radius * random.nextGaussian(0,0.334)));
        while(!isWithinReversionArea(destination,blockPos)){
            destination = blockPos.east((int) Math.round(radius * random.nextGaussian(0,0.334))).south((int) Math.round(radius * random.nextGaussian(0,0.334)));
        }
        while(!level.getBlockState(destination).is(Blocks.AIR)){
            destination = destination.above();
            if(tryTime<3 && !isWithinReversionArea(destination,blockPos)){
                destination = blockPos.east((int) Math.round(radius * random.nextGaussian(0,0.334))).south((int) Math.round(radius * random.nextGaussian(0,0.334)));
                while(!isWithinReversionArea(destination,blockPos)){
                    destination = blockPos.east((int) Math.round(radius * random.nextGaussian(0,0.334))).south((int) Math.round(radius * random.nextGaussian(0,0.334)));
                }
                tryTime ++;
                continue;
            } else if(tryTime<6){
                if(Configuration.Common.REMEDY_TYPE.get().goUpward()){
                    if(destination.getY()>=level.getMaxBuildHeight()){
                        destination = blockPos.east((int) Math.round(radius * random.nextGaussian(0,0.334))).south((int) Math.round(radius * random.nextGaussian(0,0.334)));
                        tryTime ++;
                        continue;
                    }
                } else {
                    if(!isWithinReversionArea(destination,blockPos,2)){
                        destination = blockPos.east((int) Math.round(radius * 2 * random.nextGaussian(0,0.334))).south((int) Math.round(radius * 2 * random.nextGaussian(0,0.334)));
                        tryTime ++;
                        continue;
                    }
                }
            } else if(tryTime==6) return Optional.empty();
        }
        return Optional.of(destination);
    }

    public static boolean isWithinReversionArea(BlockPos groundZero, BlockPos tested){
        return isWithinReversionArea(groundZero,tested,1);
    }

    public static boolean isWithinReversionArea(BlockPos groundZero, BlockPos tested, double extendingRate){
        return !Configuration.Common.AREA_SHAPE.get().isSphere() || groundZero.distSqr(tested) < Configuration.Common.RADIUS.get() * extendingRate;
    }

    public static List<BlockPos> createShuffledBlockPosList(AABB aabb){
        var ret = new ArrayList<BlockPos>();
        HashMultimap<Integer,BlockPos> map = HashMultimap.create();
        BlockPos.betweenClosedStream(aabb).forEach(blockPos -> {
            var pos = new BlockPos(blockPos);
            map.put(pos.getY(), pos);
        });
        map.keySet().forEach(key->{
            var temp = Lists.newArrayList(map.get(key).iterator());
            Collections.shuffle(temp);
            ret.addAll(temp);
        });
        return ret;
    }

    public static void insertIntoChestOrCreateChest(Level level, BlockPos blockPos, ItemStack insertItemStack){
        insertItemStack = searchValidChestAndInsert(level, blockPos, insertItemStack);
        if (!insertItemStack.isEmpty()) {
            var optional = MiscUtils.randomizeDestination(level, blockPos);
            if (optional.isEmpty()) return;
            var destination = optional.get();
            var facings = ChestBlock.FACING.getAllValues().toList().stream().map(Property.Value::value).toList();
            level.setBlockAndUpdate(destination, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facings.get(new Random().nextInt(facings.size()))));
            BlockEntity chestBlockEntity = level.getBlockEntity(destination);
            var itemhandler = chestBlockEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if (itemhandler.isPresent()) {
                ItemStack finalInsertItemStack = insertItemStack;
                itemhandler.ifPresent(cap -> cap.insertItem(0, finalInsertItemStack, false));
            }
        }
    }

    public static ItemStack searchValidChestAndInsert(Level level, BlockPos blockPos, ItemStack itemStack){
        var tryTime = 0;
        while(tryTime<2){
            List<BlockPos> blockPosList;
            if(tryTime==0){
                blockPosList = createShuffledBlockPosList(createScanningArea(blockPos));
                if(Configuration.Common.AREA_SHAPE.get().isSphere())
                    blockPosList = blockPosList.stream().filter(pos -> isWithinReversionArea(blockPos,pos)).toList();
            } else {
                // TODO fix remedy range chest wont be filled
                blockPosList = createShuffledBlockPosList(createRemedyScanningArea(level,blockPos));
                if(Configuration.Common.AREA_SHAPE.get().isSphere())
                    blockPosList = blockPosList.stream().filter(pos -> isWithinReversionArea(blockPos,pos,2)).toList();
            }
            for(var bp: blockPosList){
                if(level.getBlockState(bp).is(Blocks.CHEST)){
                    BlockEntity chestBlockEntity = level.getBlockEntity(bp);
                    var itemhandler = chestBlockEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
                    var bi = new AtomicReference<>(itemStack.copy());
                    if(itemhandler.isPresent()){
                        itemhandler.ifPresent(cap->{
                            for(int i=0;i<cap.getSlots();i++){
                                bi.set(cap.insertItem(i, bi.get(), false));
                                if(bi.get().isEmpty()) break;
                            }
                        });
                    }
                    itemStack = bi.get();
                }
                if(itemStack.isEmpty()) return itemStack;
            }
            tryTime ++;

        }
        return itemStack;
    }

    public static BlockState getSupportBlock(Level level, BlockPos blockPos){
        // TODO check wiki to edit this method
        return Blocks.GLASS.defaultBlockState();
    }

    public static void randomizeArmorStandPose(ArmorStand armorStand) {
        Random random = new Random();
        Rotations rotations = armorStand.getHeadPose();
        float f = random.nextFloat() * 5.0F;
        float f1 = random.nextFloat() * 20.0F - 10.0F;
        Rotations rotations1 = new Rotations(rotations.getX() + f, rotations.getY() + f1, rotations.getZ());
        armorStand.setHeadPose(rotations1);
        rotations = armorStand.getBodyPose();
        f = random.nextFloat() * 10.0F - 5.0F;
        rotations1 = new Rotations(rotations.getX(), rotations.getY() + f, rotations.getZ());
        armorStand.setBodyPose(rotations1);
    }
}
