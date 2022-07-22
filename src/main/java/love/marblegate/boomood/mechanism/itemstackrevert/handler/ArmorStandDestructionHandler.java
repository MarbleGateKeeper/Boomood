package love.marblegate.boomood.mechanism.itemstackrevert.handler;

import com.google.gson.JsonObject;
import love.marblegate.boomood.config.Configuration;
import love.marblegate.boomood.misc.MiscUtils;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class ArmorStandDestructionHandler extends ItemStackRevertHandler {

    @Override
    public void revert(Level level, BlockPos blockPos, List<ItemStack> itemStacks, Player manipulator) {
        var optional = MiscUtils.randomizeDestination(level, blockPos);
        if (optional.isEmpty()) return;
        var destination = optional.get();
        // Armor stand should have support block
        if (level.getBlockState(destination.below()).is(Blocks.AIR)) {
            level.setBlockAndUpdate(destination.below(), MiscUtils.getSupportBlock(level, destination.below()));
        }
        Vec3 vec3 = Vec3.atBottomCenterOf(destination);
        AABB aabb = EntityType.ARMOR_STAND.getDimensions().makeBoundingBox(vec3.x(), vec3.y(), vec3.z());
        if (level.noCollision((Entity) null, aabb) && level.getEntities((Entity) null, aabb).isEmpty()) {
            ArmorStand armorStand = new ArmorStand(level, vec3.x, vec3.y, vec3.z);
            if (Configuration.NOISOLPXE_ARMOR_STAND_POSE_RANDOMIZE.get()) {
                armorStand.moveTo(armorStand.getX(), armorStand.getY(), armorStand.getZ(), (float) (Math.random() * 360), 0.0F);
                MiscUtils.randomizeArmorStandPose(armorStand);
            }
            for (var itemStack : itemStacks) {
                EquipmentSlot equipmentslot = Mob.getEquipmentSlotForItem(itemStack);
                armorStand.setItemSlot(equipmentslot, itemStack);
            }
            level.addFreshEntity(armorStand);
        }
        // TODO add custom particle effect for indication & add implement explosion particle
    }

    @Override
    public int priority() {
        return 30;
    }

    @Override
    public JsonObject toJson() {
        var ret = new JsonObject();
        ret.addProperty("type", "armor_stand_destruction");
        return ret;
    }

    @Override
    public List<List<ItemStack>> mergeItemStack(List<List<ItemStack>> itemStackListList) {
        // Every armorStand can hold only 1 suit of armor.
        List<List<ItemStack>> ret = new ArrayList<>();
        var flatItemStackList = itemStackListList.stream().reduce(new ArrayList<>(), (list1, list2) -> {
            list1.addAll(list2);
            return list1;
        }).stream().filter(itemStack -> itemStack.getItem() instanceof ArmorItem).toList();
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
