package love.marblegate.boomood.registry;

import love.marblegate.boomood.Boomood;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;


@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ItemPropertyRegistry {

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientSetUpEvent(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
        {
            ItemProperties.register(ItemRegistry.RESULT_REVERSER.get(), new ResourceLocation(Boomood.MOD_ID, "time"), (stack, level, living, id) -> {
                if(level==null) {
                    if(living == null) return 0F;
                    else level = (ClientLevel) living.level;
                }
                var d = living.isUsingItem() && living.getUseItem() == stack? 20: 200;
                return Math.max(1-(level.getDayTime() % d / (float) d),0);
            });
        });
    }
}
