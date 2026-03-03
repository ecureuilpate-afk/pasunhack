package net.rominux.pasunhack.mixin;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.rominux.pasunhack.AutoMiner;
import net.rominux.pasunhack.PasunhackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {

    @Inject(method = "addParticle(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"))
    private void onAddParticle(Particle particle, CallbackInfo ci) {
        if (!PasunhackConfig.getInstance().autoPrecisionMiner || AutoMiner.currentTarget == null)
            return;

        double dist = particle.getBoundingBox().getCenter().squaredDistanceTo(AutoMiner.currentTarget.toCenterPos());
        if (dist < 1.5 * 1.5) {
            AutoMiner.precisionTarget = particle.getBoundingBox().getCenter();
        }
    }
}
