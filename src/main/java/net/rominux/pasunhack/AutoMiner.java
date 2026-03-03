package net.rominux.pasunhack;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

import java.util.HashSet;
import java.util.Set;

public class AutoMiner {

    private static boolean enabled = false;
    private static long miningStartTime = 0;
    private static Vec3d lastPlayerPos = null;
    private static int aimWaitTicks = 0;

    public static Vec3d precisionTarget = null;
    public static BlockPos currentTarget = null;

    public static final Set<BlockPos> BLACKLIST_TEMP = new HashSet<>();

    public static boolean isEnabled() {
        return enabled;
    }

    public static void toggle() {
        enabled = !enabled;
        if (!enabled) {
            cancelMining(MinecraftClient.getInstance());
            BLACKLIST_TEMP.clear();
        }
        lastPlayerPos = null;
    }

    public static void tick(MinecraftClient client) {
        if (!enabled || client.player == null || client.world == null)
            return;

        Vec3d currentPos = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ());

        // Failsafe 1 : Désactivation auto si on se déplace (chute, marche)
        if (lastPlayerPos != null && currentPos.squaredDistanceTo(lastPlayerPos) > 0.005) {
            enabled = false;
            client.player.sendMessage(Text.literal("§cAutoMiner en pause (Mouvement)"), true);
            cancelMining(client);
            return;
        }
        lastPlayerPos = currentPos;

        // Failsafe 2 : Timeout si on reste bloqué trop longtemps sur l'activation d'un
        // bloc testé
        if (currentTarget != null && miningStartTime > 0 && (System.currentTimeMillis() - miningStartTime) > 5000) {
            BLACKLIST_TEMP.add(currentTarget);
            cancelMining(client);
        }

        if (currentTarget == null) {
            findAndAimBestTarget(client);
        } else {
            mineAndValidateTarget(client);
        }
    }

    private static void findAndAimBestTarget(MinecraftClient client) {
        BlockPos bestTarget = null;
        double minAngleDist = Double.MAX_VALUE;
        int r = 5;
        BlockPos playerPos = client.player.getBlockPos();
        Vec3d currentPos = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ());

        Set<Block> whitelist = getWhitelistedBlocks();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = playerPos.add(x, y, z);

                    if (pos.toCenterPos().squaredDistanceTo(currentPos) > 4.5 * 4.5)
                        continue;
                    if (BLACKLIST_TEMP.contains(pos))
                        continue;

                    Block block = client.world.getBlockState(pos).getBlock();

                    if (whitelist.contains(block)) {
                        Vec3d targetCenter = Vec3d.ofCenter(pos);
                        Vec3d[] pointsToTest = {
                                targetCenter,
                                targetCenter.add(0.45, 0, 0),
                                targetCenter.add(-0.45, 0, 0),
                                targetCenter.add(0, 0.45, 0),
                                targetCenter.add(0, -0.45, 0),
                                targetCenter.add(0, 0, 0.45),
                                targetCenter.add(0, 0, -0.45)
                        };

                        Vec3d validPoint = null;
                        for (Vec3d p : pointsToTest) {
                            BlockHitResult sightCheck = client.world.raycast(new RaycastContext(
                                    client.player.getEyePos(),
                                    p,
                                    RaycastContext.ShapeType.COLLIDER,
                                    RaycastContext.FluidHandling.NONE,
                                    client.player));

                            if (sightCheck.getType() == HitResult.Type.MISS || sightCheck.getBlockPos().equals(pos)) {
                                validPoint = p;
                                break;
                            }
                        }

                        if (validPoint == null) {
                            continue;
                        }

                        Vec2f targetAngle = getYawPitch(client.player.getEyePos(), validPoint);
                        double angleDist = getAngleDistance(client.player.getYaw(), client.player.getPitch(),
                                targetAngle.x, targetAngle.y);

                        // On trouve la cible demandant le minimum d'effort de rotation
                        if (angleDist < minAngleDist) {
                            minAngleDist = angleDist;
                            bestTarget = pos;
                        }
                    }
                }
            }
        }

        if (bestTarget != null) {
            Vec3d targetCenter = Vec3d.ofCenter(bestTarget);
            Vec2f angle = getYawPitch(client.player.getEyePos(), targetCenter);

            smoothLook(client, angle);

            currentTarget = bestTarget;
            precisionTarget = null;
            aimWaitTicks = 0; // Réinitialise les Ticks d'attente pour le Raycast
        }
    }

    private static void mineAndValidateTarget(MinecraftClient client) {
        if (currentTarget != null && client.player != null) {
            if (client.player.getEyePos().distanceTo(currentTarget.toCenterPos()) > 4.5) {
                BLACKLIST_TEMP.add(currentTarget);
                cancelMining(client);
                return;
            }

            // Maintien actif de l'alignement de la caméra vers le bloc
            Vec3d targetCenter = Vec3d.ofCenter(currentTarget);
            Vec2f angle = getYawPitch(client.player.getEyePos(), targetCenter);
            smoothLook(client, angle);
        }

        // Vérification immédiate que le bloc est toujours le bon (au cas où il est
        // subitement détruit par qqn d'autre ou remplacé)
        if (!getWhitelistedBlocks().contains(client.world.getBlockState(currentTarget).getBlock())) {
            cancelMining(client);
            return;
        }

        // Utilisation du crosshair pour valider la ligne de vue exacte
        HitResult hit = client.crosshairTarget;

        if (hit != null && hit.getType() == HitResult.Type.BLOCK
                && ((BlockHitResult) hit).getBlockPos().equals(currentTarget)) {
            // Le Raycast a réussi, on réinitialise nos "chances" d'alignement
            aimWaitTicks = 0;
            Direction side = ((BlockHitResult) hit).getSide();

            if (miningStartTime == 0) {
                miningStartTime = System.currentTimeMillis();
            }
            client.options.attackKey.setPressed(true);
        } else {
            // Le raycast a échoué. Au lieu de blacklist instantanément, on a un compteur de
            // tolérance (40 ticks max = 2s)
            aimWaitTicks++;
            if (aimWaitTicks > 40) {
                BLACKLIST_TEMP.add(currentTarget);
                cancelMining(client);
            }
        }
    }

    private static void cancelMining(MinecraftClient client) {
        if (client.options != null) {
            client.options.attackKey.setPressed(false);
        }
        if (currentTarget != null && client.interactionManager != null) {
            client.interactionManager.cancelBlockBreaking();
        }
        currentTarget = null;
        precisionTarget = null;
        miningStartTime = 0;
        aimWaitTicks = 0;
    }

    // Récupère dynamiquement les blocks depuis la configuration (GUI)
    private static Set<Block> getWhitelistedBlocks() {
        Set<Block> blocks = new HashSet<>();
        for (String id : PasunhackConfig.getInstance().blocksToMine) {
            try {
                Identifier identifier = Identifier.of(id);
                if (identifier != null && Registries.BLOCK.containsId(identifier)) {
                    blocks.add(Registries.BLOCK.get(identifier));
                }
            } catch (Throwable t) {
                // Ignore silentieusement si l'ID est invalide
            }
        }
        return blocks;
    }

    public static Vec2f getYawPitch(Vec3d eyePos, Vec3d target) {
        double dx = target.x - eyePos.x;
        double dy = target.y - eyePos.y;
        double dz = target.z - eyePos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        return new Vec2f(yaw, pitch);
    }

    private static void smoothLook(MinecraftClient client, Vec2f targetAngle) {
        if (client.player == null)
            return;
        float currentYaw = client.player.getYaw();
        float currentPitch = client.player.getPitch();

        Vec2f target = targetAngle;
        if (PasunhackConfig.getInstance().autoPrecisionMiner && precisionTarget != null) {
            target = getYawPitch(client.player.getEyePos(), precisionTarget);
        }

        if (getAngleDistance(currentYaw, currentPitch, target.x, target.y) >= 1.0) {
            float newYaw = MathHelper.lerpAngleDegrees(0.45f, currentYaw, target.x);
            float newPitch = MathHelper.lerpAngleDegrees(0.45f, currentPitch, target.y);

            client.player.setYaw(newYaw);
            client.player.setPitch(newPitch);
        }
    }

    public static double getAngleDistance(float yaw1, float pitch1, float yaw2, float pitch2) {
        float dyaw = MathHelper.wrapDegrees(yaw1 - yaw2);
        float dpitch = pitch1 - pitch2;
        return Math.sqrt(dyaw * dyaw + dpitch * dpitch);
    }
}
