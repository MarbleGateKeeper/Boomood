package love.marblegate.boomood.registry;

import love.marblegate.boomood.Boomood;
import love.marblegate.boomood.entity.Singularity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EntityRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, Boomood.MOD_ID);

    public static final RegistryObject<EntityType<Singularity>> SINGULARITY = ENTITIES.register("singularity",
            () -> EntityType.Builder.<Singularity>of(Singularity::new, MobCategory.MISC)
                    .fireImmune().sized(1F, 1F).clientTrackingRange(32).updateInterval(Integer.MAX_VALUE)
                    .build("singularity"));
}
