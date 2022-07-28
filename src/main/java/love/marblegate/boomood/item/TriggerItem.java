package love.marblegate.boomood.item;

import love.marblegate.boomood.config.Configuration;
import love.marblegate.boomood.entity.Singularity;
import love.marblegate.boomood.misc.MiscUtils;
import love.marblegate.boomood.misc.ServerUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class TriggerItem extends Item {
    private final static int ACTUAL_MAX_EFFECTIVE_DURATION = 100;
    public TriggerItem() {
        super(new Properties().tab(CreativeModeTab.TAB_COMBAT).stacksTo(1));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand interactionHand) {
        ItemStack itemstack = player.getItemInHand(interactionHand);
        player.startUsingItem(interactionHand);
        return InteractionResultHolder.consume(itemstack);
    }

    @Override
    public int getUseDuration(@NotNull ItemStack itemStack) {
        return 400;
    }

    @Override
    public void releaseUsing(@NotNull ItemStack itemStack, Level level, LivingEntity user, int timeLeft) {
        if (!level.isClientSide() && user instanceof Player player) {
            var groundZero = MiscUtils.findLookAt(player);
            if(level.isInWorldBounds(groundZero)){
                player.getCooldowns().addCooldown(this, 40);
                Singularity singularity = new Singularity(level, groundZero);
                singularity.moveTo(Vec3.atCenterOf(groundZero));
                level.addFreshEntity(singularity);
            }
        }
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack itemStack) {
        return UseAnim.SPEAR;
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack itemStack, Level level, LivingEntity user) {
        if (!level.isClientSide() && user instanceof Player player) {
            // TODO notice player that holding too long
            player.displayClientMessage(new TranslatableComponent("notice.boomood.activating_reverser_too_long"), true);
            player.getCooldowns().addCooldown(this, 400);
        }
        return itemStack;
    }


    @Override
    public void onUsingTick(ItemStack stack, LivingEntity user, int count) {
        if(!user.level.isClientSide()){
            if(user instanceof Player player){
                var groundZero = MiscUtils.findLookAt(player);
                if(user.level.isInWorldBounds(groundZero)){
                    ServerUtils.addParticle(user.level, ParticleTypes.ENCHANT, groundZero, 0D, 3D, 0D, 0.1, 10);
                    ServerUtils.addParticle(user.level, ParticleTypes.DRAGON_BREATH, groundZero, 0D, 3D, 0D, 0.1, 10);
                } else {
                    var lookVec = player.getLookAngle();
                    ServerUtils.addParticle(user.level, ParticleTypes.WITCH, user.eyeBlockPosition(), lookVec.x, lookVec.y, lookVec.z, 1, 3);
                }

            }
        }
    }
}
