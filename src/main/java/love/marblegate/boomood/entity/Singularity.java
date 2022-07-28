package love.marblegate.boomood.entity;

import love.marblegate.boomood.config.Configuration;
import love.marblegate.boomood.mechanism.ReversionFactory;
import love.marblegate.boomood.misc.MiscUtils;
import love.marblegate.boomood.registry.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

// TODO
public class Singularity extends Mob {

    private BlockPos groundZero;

    private int lifespan = 100;

    public Singularity(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
        groundZero = new BlockPos(0,0,0);
    }

    public Singularity(Level level, BlockPos groundZero){
        super(EntityRegistry.SINGULARITY.get(),level);
        this.groundZero = groundZero;
    }

    @Override
    public void tick() {
        if(!level.isClientSide()){
            if(lifespan!=0){
                if(lifespan == 20){
                    if(level.isInWorldBounds(groundZero)){
                        var area = MiscUtils.createScanningArea(groundZero);
                        // Extinguish fire
                        ReversionFactory.fireBurnRevert(area).revert(this ,groundZero);
                        // Remove non-source liquid & Water
                        ReversionFactory.fluidFlowRevert(area).revert(this ,groundZero);
                        // Revert item Drop
                        ReversionFactory.itemDropRevert(level, groundZero, area).revert(this ,groundZero);
                    }
                }
                lifespan--;
            }
            else discard();
        } else {
            var radius = Configuration.Common.RADIUS.get();
            float v = (float) Math.PI * radius * radius;
            for (int i = 0; (float) i < v; ++i) {
                float v1 = random.nextFloat() * ((float) Math.PI * 2F);
                float v2 = Mth.sqrt(random.nextFloat()) * radius;
                float v3 = Mth.cos(v1) * v2;
                float v4 = Mth.sin(v1) * v2;
                level.addAlwaysVisibleParticle(ParticleTypes.ENCHANT, getX() + (double) v3, getY() + Configuration.Common.RADIUS.get(), getZ() + (double) v4, 0.98, 0.99, 1);
                level.addAlwaysVisibleParticle(ParticleTypes.DRAGON_BREATH, getX() + (double) v3, getY(), getZ() + (double) v4, 0.01, 0.05, 0.01);
            }
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        lifespan = compoundTag.getInt("lifespan");
        groundZero = NbtUtils.readBlockPos((CompoundTag) compoundTag.get("groundZero"));
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        compoundTag.putInt("lifespan",lifespan);
        compoundTag.put("groundZero",NbtUtils.writeBlockPos(groundZero));
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    public static AttributeSupplier.Builder prepareAttributes(){
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH,2048)
                .add(Attributes.FOLLOW_RANGE,0)
                .add(Attributes.MOVEMENT_SPEED,0)
                .add(Attributes.ATTACK_DAMAGE,2048);
    }
}
