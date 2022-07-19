package love.marblegate.boomood.registry;

import love.marblegate.boomood.Boomood;
import love.marblegate.boomood.entity.TntEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EntityRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, Boomood.MOD_ID);

    public static final RegistryObject<EntityType<TntEntity>> TNT = ENTITIES.register("tnt",
            () -> EntityType.Builder.of(TntEntity::new, MobCategory.MISC)
                    .fireImmune().sized(1.0F, 1.0F).clientTrackingRange(20).updateInterval(Integer.MAX_VALUE)
                    .build("tnt"));
}
