package love.marblegate.boomood.recipe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class NoisolpxeSituations {
    public static class BlockDestruction implements NoisolpxeSituation{
        private Block block;

        public BlockDestruction(Block block) {
            this.block = block;
        }

        @Override
        public void revert(LevelAccessor level, BlockPos blockPos, List<ItemStack> items, Player manipulator) {

        }
    }

    public static class ArmorStandDestruction implements NoisolpxeSituation{

        @Override
        public void revert(LevelAccessor level, BlockPos blockPos, List<ItemStack> items, Player manipulator) {

        }
    }

    public static class ChestDestruction implements NoisolpxeSituation{

        @Override
        public void revert(LevelAccessor level, BlockPos blockPos, List<ItemStack> items, Player manipulator) {

        }
    }

    public static class EntityDeath implements NoisolpxeSituation{
        private Entity entity;

        public EntityDeath(Entity entity) {
            this.entity = entity;
        }

        @Override
        public void revert(LevelAccessor level, BlockPos blockPos, List<ItemStack> items, Player manipulator) {

        }
    }
}
