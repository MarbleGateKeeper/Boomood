package love.marblegate.boomood;

import com.mojang.logging.LogUtils;
import love.marblegate.boomood.config.Configuration;
import love.marblegate.boomood.registry.EntityRegistry;
import love.marblegate.boomood.registry.ItemRegistry;
import love.marblegate.boomood.registry.RecipeRegistry;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod("boomood")
public class Boomood {
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    public static String MOD_ID = "boomood";

    public Boomood() {
        ItemRegistry.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        EntityRegistry.ENTITIES.register(FMLJavaModLoadingContext.get().getModEventBus());
        RecipeRegistry.RECIPE_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
        RecipeRegistry.RECIPE_SERIALIZERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Configuration.MOD_CONFIG);
    }
}
