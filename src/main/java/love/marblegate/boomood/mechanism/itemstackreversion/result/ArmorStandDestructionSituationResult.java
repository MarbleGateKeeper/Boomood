package love.marblegate.boomood.mechanism.itemstackreversion.result;

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

public class ArmorStandDestructionSituationResult extends ReversionSituationResult {

    @Override
    public JsonObject toJson() {
        var ret = new JsonObject();
        ret.addProperty("type", "armor_stand_destruction");
        return ret;
    }

    @Override
    public String situationId() {
        return "armor_stand_destruction";
    }

    @Override
    public String toString() {
        return "ArmorStandDestructionSituationResult{}";
    }
}
