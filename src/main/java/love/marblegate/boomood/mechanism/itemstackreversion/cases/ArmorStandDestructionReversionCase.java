package love.marblegate.boomood.mechanism.itemstackreversion.cases;

import love.marblegate.boomood.config.Configuration;
import love.marblegate.boomood.mechanism.itemstackreversion.dataholder.ResultPack;
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

public class ArmorStandDestructionReversionCase implements ReversionCase {

    private final List<ItemStack> itemStacks = new ArrayList<>();


    private static List<List<ItemStack>> arrangeIntoSuit(List<ItemStack> itemStacks) {
        // Every armorStand can hold only 1 suit of armor.
        List<List<ItemStack>> ret = new ArrayList<>();
        var helmets = itemStacks.stream().filter(itemStack -> ((ArmorItem) itemStack.getItem()).getSlot() == EquipmentSlot.HEAD).toList();
        var chestplates = itemStacks.stream().filter(itemStack -> ((ArmorItem) itemStack.getItem()).getSlot() == EquipmentSlot.CHEST).toList();
        var leggings = itemStacks.stream().filter(itemStack -> ((ArmorItem) itemStack.getItem()).getSlot() == EquipmentSlot.LEGS).toList();
        var boots = itemStacks.stream().filter(itemStack -> ((ArmorItem) itemStack.getItem()).getSlot() == EquipmentSlot.FEET).toList();
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

    @Override
    public int priority() {
        return 25;
    }

    @Override
    public void add(ResultPack pack) {
        itemStacks.addAll(pack.items().stream().filter(itemStack -> itemStack.getItem() instanceof ArmorItem).toList());
    }

    @Override
    public void revert(Player manipulator, BlockPos blockPos) {
        var suits = arrangeIntoSuit(itemStacks);
        if(Configuration.DEBUG_MODE.get()){
            System.out.println("Reverting ItemFrameDestruction. Details: " + toDetailedString(suits));
        }
        var level = manipulator.level;
        for(var suit:suits){
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
                if (Configuration.ItemStackReversion.ARMOR_STAND_POSE_RANDOMIZE.get()) {
                    armorStand.moveTo(armorStand.getX(), armorStand.getY(), armorStand.getZ(), (float) (Math.random() * 360), 0.0F);
                    MiscUtils.randomizeArmorStandPose(armorStand);
                }
                for (var itemStack:suit) {
                    EquipmentSlot equipmentslot = Mob.getEquipmentSlotForItem(itemStack);
                    armorStand.setItemSlot(equipmentslot, itemStack);
                }
                level.addFreshEntity(armorStand);
            } else {
                for(var itemStack:suit){
                    MiscUtils.insertIntoChestOrCreateChest(level,blockPos,itemStack);
                }
            }
        }
        // TODO add custom particle effect for indication & add implement explosion particle
    }

    public String toDetailedString(List<List<ItemStack>> lists) {
        return "ArmorStandDestructionReversionCase{" +
                "itemStacks=" + itemStacks +
                ", after rearranging: itemStacks=" +
                lists + '}';
    }
}
