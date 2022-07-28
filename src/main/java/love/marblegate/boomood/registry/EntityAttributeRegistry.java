package love.marblegate.boomood.registry;

import love.marblegate.boomood.Boomood;
import love.marblegate.boomood.entity.Singularity;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Boomood.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EntityAttributeRegistry {
    @SubscribeEvent
    public static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(EntityRegistry.SINGULARITY.get(), Singularity.prepareAttributes().build());
    }
}
