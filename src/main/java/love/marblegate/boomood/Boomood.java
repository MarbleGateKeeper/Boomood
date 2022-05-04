package love.marblegate.boomood;

import love.marblegate.boomood.registry.EntityRegistry;
import love.marblegate.boomood.registry.ItemRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("boomood")
public class Boomood {
    public static String MOD_ID = "boomood";

    public Boomood(){
        ItemRegistry.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        EntityRegistry.ENTITIES.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
