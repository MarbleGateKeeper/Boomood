package love.marblegate.boomood.mechanism.itemstackreversion.cases;

import com.google.common.collect.Lists;
import love.marblegate.boomood.Boomood;
import love.marblegate.boomood.config.Configuration;
import love.marblegate.boomood.mechanism.itemstackreversion.dataholder.AvailableBlockPosHolder;
import love.marblegate.boomood.mechanism.itemstackreversion.result.ItemFrameDestructionSituationResult;
import love.marblegate.boomood.mechanism.itemstackreversion.dataholder.IntermediateResultHolder;
import love.marblegate.boomood.misc.MiscUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ItemFrameDestructionReversionCase implements ReversionCase {

    private final List<ItemStack> targets = new ArrayList<>();

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public void add(IntermediateResultHolder pack) {
        var result = (ItemFrameDestructionSituationResult) pack.result();
        if(result.getTargets()==null)
            targets.addAll(pack.items());
        else
            targets.addAll(result.getTargets());
    }

    @Override
    public void revert(Player manipulator, AvailableBlockPosHolder blockPosHolder) {
        Boomood.LOGGER.debug("Reverting ItemFrameDestruction. Details: " + this);
        // TODO need fix: will item frame spawn inside block
        // TODO still problematic!
        var level = manipulator.level;
        ItemFrame itemFrame;
        for(var itemStack:targets){
            var optional = blockPosHolder.next();
            // TODO all, not only this, if no place to place, add to chest
            if (optional.isEmpty()) return;
            var isGlowItemFrame = Math.random() < Configuration.ItemStackReversion.GLOW_ITEM_FRAME_POSSIBILITY.get();
            var is = itemStack.copy();
            var destination = optional.get();
            for (var direction : Direction.values()) {
                if (isGlowItemFrame) itemFrame = new GlowItemFrame(level, destination, direction);
                else itemFrame = new ItemFrame(level, destination, direction);
                if (itemFrame.survives()) {
                    itemFrame.setItem(is.copy());
                    level.addFreshEntity(itemFrame);
                    is = ItemStack.EMPTY;
                    // TODO add custom particle effect for indication & add implement explosion particle
                    break;
                }
            }
            if (is.isEmpty()) continue;
            var tryTime = 0;
            while (tryTime < 3) {
                optional = blockPosHolder.next();
                if (optional.isEmpty()) return;
                destination = optional.get();
                var od = getEmptyNeighborDirection(level, destination);
                if (od.isEmpty()) {
                    tryTime++;
                } else {
                    blockPosHolder.remove(destination.relative(od.get()));
                    level.setBlockAndUpdate(destination.relative(od.get()), MiscUtils.getSupportBlock(level, destination.relative(od.get())));
                    if (isGlowItemFrame) itemFrame = new GlowItemFrame(level, destination, od.get().getOpposite());
                    else itemFrame = new ItemFrame(level, destination, od.get().getOpposite());
                    itemFrame.setItem(is);
                    level.addFreshEntity(itemFrame);
                    is = ItemStack.EMPTY;
                    break;
                    // TODO add custom particle effect for indication & add implement explosion particle
                }
            }
            if (!is.isEmpty()) {
                MiscUtils.insertIntoChestOrCreateChest(level, blockPosHolder, is);
            }
        }
    }

    private Optional<Direction> getEmptyNeighborDirection(Level level, BlockPos blockPos) {
        var ds = Lists.newArrayList(Direction.values());
        Collections.shuffle(ds);
        for (var direction : ds) {
            if (level.getBlockState(blockPos.relative(direction)).getBlock().equals(Blocks.AIR)) {
                if(level.getEntities((Entity)null, new AABB(blockPos.relative(direction)), (entity) -> entity instanceof HangingEntity).isEmpty())
                    return Optional.of(direction);
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "ItemFrameDestructionReversionCase{" +
                "targets=" + targets +
                '}';
    }
}
