package love.marblegate.boomood.mechanism.itemstackreversion.handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.*;

public abstract class ItemStackRevertHandler {
    public static ItemStackRevertHandler createArmorHandler() {
        return new ArmorStandDestructionHandler();
    }

    public static ItemStackRevertHandler createDefaultHandler(ItemStack itemStack) {
        return new ChestDestructionHandler(itemStack);
    }

    public abstract void revert(Level level, BlockPos blockPos, List<ItemStack> itemStacks, Player manipulator);

    public abstract int priority();

    public abstract JsonObject toJson();

    public abstract List<List<ItemStack>> mergeItemStack(List<List<ItemStack>> itemStackListList);

    public static ItemStackRevertHandler create(JsonObject jsonObject) {
        var st = GsonHelper.getAsString(jsonObject, "type");
        return switch (st) {
            case "entity_death" -> new EntityDeathHandler(jsonObject);
            case "chest_destruction" -> new ChestDestructionHandler(jsonObject);
            case "item_frame_destruction" -> new ItemFrameDestructionHandler(jsonObject);
            case "armor_stand_destruction" -> new ArmorStandDestructionHandler();
            case "block_destruction" -> new BlockDestructionHandler(jsonObject);
            default -> throw new JsonSyntaxException("Expected type to be \"entity_death\", \"chest_destruction\", \"item_frame_destruction\", \"armor_stand_destruction\" or \"block_destruction\", was " + st);
        };
    }

}
