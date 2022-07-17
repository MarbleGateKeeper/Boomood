package love.marblegate.boomood.registry;

import love.marblegate.boomood.Boomood;
import love.marblegate.boomood.item.TriggerItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Boomood.MOD_ID);
    public static final RegistryObject<Item> CAUSE = ITEMS.register("the_cause", TriggerItem::new);
}
