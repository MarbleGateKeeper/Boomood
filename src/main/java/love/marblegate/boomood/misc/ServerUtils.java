package love.marblegate.boomood.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class ServerUtils {
    public static <T extends ParticleOptions> void addParticle(Level level, T particle, double x, double y, double z, double xDist, double yDist, double zDist, double maxSpeed, int count) {
        ((ServerLevel) level).sendParticles(particle, x, y, z, count, xDist, yDist, zDist, maxSpeed);
    }

    public static <T extends ParticleOptions> void addParticle(Level level, T particle, BlockPos blockPos, double xDist, double yDist, double zDist, double maxSpeed, int count) {
        addParticle(level, particle, blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5, xDist, yDist, zDist, maxSpeed, count);
    }
}
