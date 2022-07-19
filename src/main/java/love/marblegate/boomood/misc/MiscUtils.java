package love.marblegate.boomood.misc;

import love.marblegate.boomood.config.Configuration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Rotations;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Optional;
import java.util.Random;

public class MiscUtils {
    public static BlockPos findLookAt(Player player) {
        HitResult hitResult = player.pick(128.0D, 0.0F, false);
        return ((BlockHitResult) hitResult).getBlockPos();
    }

    public static AABB createScanningArea(BlockPos blockPos){
        return new AABB(blockPos)
                .expandTowards(Configuration.NOISOLPXE_HORIZONTAL_RADIUS.get(), Configuration.NOISOLPXE_SUGGESTED_VERTICAL_HEIGHT.get(), Configuration.NOISOLPXE_HORIZONTAL_RADIUS.get())
                .expandTowards(-Configuration.NOISOLPXE_HORIZONTAL_RADIUS.get(), -Configuration.NOISOLPXE_SUGGESTED_VERTICAL_HEIGHT.get(), -Configuration.NOISOLPXE_HORIZONTAL_RADIUS.get());
    }

    public static Optional<BlockPos> randomizeDestination(Level level, BlockPos blockPos){
        var random = new Random();
        var offset = Configuration.NOISOLPXE_HORIZONTAL_RADIUS.get();
        var tryTime = 0;
        var suggest_height_limit = blockPos.getY() + Configuration.NOISOLPXE_SUGGESTED_VERTICAL_HEIGHT.get();
        var destination = blockPos.east((int) Math.round(offset * random.nextGaussian(0,0.334))).south((int) Math.round(offset * random.nextGaussian(0,0.334)));
        while(!level.getBlockState(destination).is(Blocks.AIR)){
            destination = destination.above();
            if(tryTime<3 && destination.getY()>suggest_height_limit){
                destination = blockPos.east((int) Math.round(offset * random.nextGaussian(0,0.334))).south((int) Math.round(offset * random.nextGaussian(0,0.334)));
                tryTime ++;
                continue;
            }
            // Height Limit
            if(destination.getY()>256){
                return Optional.empty(); // Give the f**k up
            }
        }
        return Optional.of(destination);
    }

    public static BlockState getSupportBlock(){
        return Blocks.GLASS.defaultBlockState();
    }

    public static void randomizeArmorStandPose(ArmorStand armorStand) {
        Random random = new Random();
        Rotations rotations = armorStand.getHeadPose();
        float f = random.nextFloat() * 5.0F;
        float f1 = random.nextFloat() * 20.0F - 10.0F;
        Rotations rotations1 = new Rotations(rotations.getX() + f, rotations.getY() + f1, rotations.getZ());
        armorStand.setHeadPose(rotations1);
        rotations = armorStand.getBodyPose();
        f = random.nextFloat() * 10.0F - 5.0F;
        rotations1 = new Rotations(rotations.getX(), rotations.getY() + f, rotations.getZ());
        armorStand.setBodyPose(rotations1);
    }
}
