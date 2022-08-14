package love.marblegate.boomood.mechanism.itemstackreversion.cases;

import love.marblegate.boomood.Boomood;
import love.marblegate.boomood.mechanism.itemstackreversion.dataholder.AvailableBlockPosHolder;
import love.marblegate.boomood.mechanism.itemstackreversion.dataholder.EntityInfoHolder;
import love.marblegate.boomood.mechanism.itemstackreversion.dataholder.IntermediateResultHolder;
import love.marblegate.boomood.mechanism.itemstackreversion.result.EntityDeathSituationResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ServerLevelAccessor;

import java.util.ArrayList;
import java.util.List;

public class EntityDeathReversionCase implements ReversionCase {
    //TODO
    private final List<EntityInfoHolder> holderList = new ArrayList<>();

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public void add(IntermediateResultHolder pack) {
        holderList.addAll(((EntityDeathSituationResult) pack.result()).getHolderList());
    }

    @Override
    public void revert(Entity manipulator, AvailableBlockPosHolder blockPosHolder) {
        Boomood.LOGGER.debug("Reverting EntityDeath. Details: " + this);
        for (var holder : holderList) {
            for (int i = 0; i < holder.count(); i++) {
                var optional = blockPosHolder.next();
                if (optional.isEmpty()) return;
                var destination = optional.get();
                CompoundTag compoundtag = holder.tags().copy();
                compoundtag.putString("id", holder.entityType().getRegistryName().toString());
                Entity entity = EntityType.loadEntityRecursive(compoundtag, manipulator.getLevel(), (entity1) -> {
                    entity1.moveTo(destination.getX(), destination.getY(), destination.getZ(), entity1.getYRot(), entity1.getXRot());
                    return entity1;
                });
                if (entity != null) {
                    if (entity instanceof Mob) {
                        ((Mob) entity).finalizeSpawn((ServerLevelAccessor) manipulator.level, manipulator.level.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.EVENT, (SpawnGroupData) null, (CompoundTag) null);
                    }
                    ((ServerLevel) manipulator.level).tryAddFreshEntityWithPassengers(entity);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "EntityDeathReversionCase{" +
                "holderList=" + holderList +
                '}';
    }
}
