package net.rominux.pasunhack;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import java.util.HashSet;
import java.util.Set;

public class AutoMiner {

    private static boolean enabled = false;
    private static BlockPos currentTarget = null;
    private static long miningStartTime = 0;
    private static Vec3d lastPlayerPos = null;

    // Listes gérées dynamiquement par ta GUI
    public static final Set<Block> WHITELIST = new HashSet<>(Set.of(Blocks.DIORITE, Blocks.DIAMOND_ORE));
    public static final Set<BlockPos> BLACKLIST_TEMP = new HashSet<>();

    public static boolean isEnabled() {
        return enabled;
    }

    public static void toggle() {
        enabled = !enabled;
        if (!enabled)
            cancelMining(MinecraftClient.getInstance());
        lastPlayerPos = null;
    }

    public static void tick(MinecraftClient client) {
        if (!enabled || client.player == null || client.world == null)
            return;

        // Solution de contournement universelle pour remplacer client.player.getPos()
        Vec3d currentPos = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ());

        // Failsafe 1 : Désactivation si le joueur se déplace (clavier/chute)
        if (lastPlayerPos != null && currentPos.squaredDistanceTo(lastPlayerPos) > 0.005) {
            enabled = false;
            client.player.sendMessage(Text.literal("§cAutoMiner en pause (Mouvement)"), true);
            cancelMining(client);
            return;
        }
        lastPlayerPos = currentPos;

        // Failsafe 2 : Timeout de 5 secondes sur un bloc buggé ou inatteignable
        if (currentTarget != null && miningStartTime > 0 && (System.currentTimeMillis() - miningStartTime) > 5000) {
            BLACKLIST_TEMP.add(currentTarget);
            cancelMining(client);
        }

        // Si on ne mine rien, on cherche une nouvelle cible
        if (currentTarget == null) {
            findAndAimBestTarget(client);
        }
        // Si on a une cible et qu'on la regarde (validée par le raycast), on mine
        else {
            mineAndValidateTarget(client);
        }
    }

    private static void findAndAimBestTarget(MinecraftClient client) {
        BlockPos bestTarget = null;
        double minAngleDist = Double.MAX_VALUE;
        int r = 5; // Pour couvrir un rayon sphérique de 4.5
        BlockPos playerPos = client.player.getBlockPos();
        Vec3d currentPos = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ());

        // Scanne la zone en 3D
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = playerPos.add(x, y, z);

                    // Vérification de la distance maximale absolue (4.5)
                    if (pos.toCenterPos().squaredDistanceTo(currentPos) > 4.5 * 4.5)
                        continue;
                    if (BLACKLIST_TEMP.contains(pos))
                        continue;

                    Block block = client.world.getBlockState(pos).getBlock();

                    // Si le bloc est dans notre liste configurée dans la GUI
                    if (WHITELIST.contains(block)) {
                        // Récupère le centre de la hitbox du bloc (souvent de taille 1x1x1)
                        Vec3d targetCenter = Vec3d.ofCenter(pos);

                        // Calcule l'angle requis depuis l'œil du joueur vers le bloc
                        Vec2f targetAngle = getYawPitch(client.player.getEyePos(), targetCenter);

                        // Évalue à quel point le bloc est proche de notre réticule
                        double angleDist = getAngleDistance(client.player.getYaw(), client.player.getPitch(),
                                targetAngle.x, targetAngle.y);

                        // On garde celui qui nécessite le moins de mouvement de caméra
                        if (angleDist < minAngleDist) {
                            minAngleDist = angleDist;
                            bestTarget = pos;
                        }
                    }
                }
            }
        }

        // Si un candidat potentiel a été trouvé
        if (bestTarget != null) {
            Vec3d targetCenter = Vec3d.ofCenter(bestTarget);
            Vec2f angle = getYawPitch(client.player.getEyePos(), targetCenter);

            // Applique la rotation - (côté client). Cela déclenche la rotation de la tête
            client.player.setYaw(angle.x);
            client.player.setPitch(angle.y);

            currentTarget = bestTarget;
        }
    }

    private static void mineAndValidateTarget(MinecraftClient client) {
        HitResult hit = client.player.raycast(4.5D, 1.0F, false);

        if (hit.getType() == HitResult.Type.BLOCK && ((BlockHitResult) hit).getBlockPos().equals(currentTarget)) {
            Direction side = ((BlockHitResult) hit).getSide();

            if (miningStartTime == 0) {
                client.interactionManager.attackBlock(currentTarget, side);
                client.player.swingHand(Hand.MAIN_HAND);
                miningStartTime = System.currentTimeMillis();
            } else {
                client.interactionManager.updateBlockBreakingProgress(currentTarget, side);
                client.player.swingHand(Hand.MAIN_HAND);
            }

            if (client.world.getBlockState(currentTarget).isAir()) {
                cancelMining(client);
            }
        } else {
            BLACKLIST_TEMP.add(currentTarget);
            cancelMining(client);
        }
    }

    private static void cancelMining(MinecraftClient client) {
        if (currentTarget != null && client.interactionManager != null) {
            client.interactionManager.cancelBlockBreaking();
        }
        currentTarget = null;
        miningStartTime = 0;
    }

    // --- MATHS UTILS --- //

    public static Vec2f getYawPitch(Vec3d eyePos, Vec3d target) {
        double dx = target.x - eyePos.x;
        double dy = target.y - eyePos.y;
        double dz = target.z - eyePos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        return new Vec2f(yaw, pitch);
    }

    public static double getAngleDistance(float yaw1, float pitch1, float yaw2, float pitch2) {
        float dyaw = MathHelper.wrapDegrees(yaw1 - yaw2);
        float dpitch = pitch1 - pitch2;
        return Math.sqrt(dyaw * dyaw + dpitch * dpitch);
    }
}
