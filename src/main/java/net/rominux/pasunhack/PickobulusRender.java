package net.rominux.pasunhack;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class PickobulusRender {
    public static void onLast(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return;

        boolean showWaypoints = PasunhackConfig.getInstance().showCommissionWaypoints;
        boolean showPickobulus = PasunhackConfig.getInstance().showPickobulusPreview;

        if (!showWaypoints && !showPickobulus)
            return;

        Camera camera = client.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        VertexConsumerProvider vertexConsumers = context.consumers() != null ? context.consumers()
                : client.getBufferBuilders().getEntityVertexConsumers();

        Item item = client.player.getMainHandStack().getItem();
        if (showPickobulus && (item.toString().contains("pickaxe") || item == Items.PRISMARINE_SHARD)) {
            Vec3d start = client.player.getEyePos();
            Vec3d end = start.add(client.player.getRotationVec(1.0f).multiply(20.0));
            HitResult hit = client.world.raycast(new net.minecraft.world.RaycastContext(start, end,
                    net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                    net.minecraft.world.RaycastContext.FluidHandling.NONE, client.player));

            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = ((BlockHitResult) hit).getBlockPos();
                if (pos.isWithinDistance(camPos, 20.0)) {
                    Box box = new Box(pos.getX() - 2, pos.getY() - 2, pos.getZ() - 2, pos.getX() + 3, pos.getY() + 3,
                            pos.getZ() + 3).offset(-camPos.x, -camPos.y, -camPos.z);
                    VertexConsumer lineConsumer = vertexConsumers.getBuffer(RenderLayer.getLines());
                    drawBox(context.matrices(), lineConsumer, box, 0.0f, 1.0f, 0.0f, 1.0f);
                }
            }
        }

        if (showWaypoints) {
            net.minecraft.client.font.TextRenderer textRenderer = client.textRenderer;
            net.minecraft.client.render.VertexConsumerProvider.Immediate immediate = client.getBufferBuilders()
                    .getEntityVertexConsumers();

            for (CommissionsOverlay.CommissionWaypoint wp : CommissionsOverlay.waypoints) {
                // Draw Pillar (Beacon Beam)
                Box box = new Box(wp.x - 0.1, wp.y, wp.z - 0.1, wp.x + 0.1, wp.y + 100, wp.z + 0.1)
                        .offset(-camPos.x, -camPos.y, -camPos.z);

                VertexConsumer lineConsumer = immediate.getBuffer(RenderLayer.getLines());
                drawBox(context.matrices(), lineConsumer, box, 1.0f, 0.5f, 0.0f, 0.8f);

                Vec3d waypointVec3d = new Vec3d(wp.x, wp.y, wp.z);
                double distance = camPos.distanceTo(waypointVec3d);
                String label = "§l§n" + wp.name + " §r(§a" + (int) distance + "m)";

                context.matrices().push();
                // 1. Déplacer au centre du waypoint (légèrement au-dessus)
                context.matrices().translate(waypointVec3d.x - camPos.x, waypointVec3d.y + 1.0 - camPos.y,
                        waypointVec3d.z - camPos.z);

                // 2. Faire pivoter le texte pour qu'il regarde toujours la caméra
                // (Billboarding)
                context.matrices()
                        .multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                context.matrices()
                        .multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

                // 3. Réduire la taille du texte (Scale)
                float scale = 0.1f;
                // On utilise un scale négatif pour que le texte ne soit pas inversé (miroir)
                context.matrices().scale(-scale, -scale, scale);

                // 4. Rendu du texte à travers les murs (SEE_THROUGH)
                float xOffset = -textRenderer.getWidth(label) / 2.0f; // Centrer le texte
                org.joml.Matrix4f positionMatrix = context.matrices().peek().getPositionMatrix();

                textRenderer.draw(
                        label,
                        xOffset,
                        0f,
                        0xFF000000, // Couleur du texte (Blanc)
                        false, // Pas d'ombre portée classique car on a un background
                        positionMatrix,
                        immediate,
                        net.minecraft.client.font.TextRenderer.TextLayerType.SEE_THROUGH, // C'est CA qui permet de voir
                                                                                          // à travers les murs
                        0xFF000000, // Fond opaque
                        15728880);

                // On force le rendu immédiat pour éviter des conflits de tampons
                immediate.draw();
                context.matrices().pop();
            }
        }
    }

    private static void drawBox(net.minecraft.client.util.math.MatrixStack matrices, VertexConsumer vertexConsumer,
            Box box, float r, float g, float b, float a) {
        org.joml.Matrix4f matrix = matrices.peek().getPositionMatrix();
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        vertexConsumer.vertex(matrix, minX, minY, minZ).color(r, g, b, a).normal(1, 0, 0);
        vertexConsumer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).normal(1, 0, 0);
        vertexConsumer.vertex(matrix, minX, minY, minZ).color(r, g, b, a).normal(0, 1, 0);
        vertexConsumer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).normal(0, 1, 0);
        vertexConsumer.vertex(matrix, minX, minY, minZ).color(r, g, b, a).normal(0, 0, 1);
        vertexConsumer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).normal(0, 0, 1);
        vertexConsumer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).normal(0, -1, 0);
        vertexConsumer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).normal(0, -1, 0);
        vertexConsumer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).normal(-1, 0, 0);
        vertexConsumer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).normal(-1, 0, 0);
        vertexConsumer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).normal(0, 0, 1);
        vertexConsumer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).normal(0, 0, 1);
        vertexConsumer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).normal(1, 0, 0);
        vertexConsumer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).normal(1, 0, 0);
        vertexConsumer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).normal(0, -1, 0);
        vertexConsumer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).normal(0, -1, 0);
        vertexConsumer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).normal(0, 0, -1);
        vertexConsumer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).normal(0, 0, -1);
        vertexConsumer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).normal(-1, 0, 0);
        vertexConsumer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).normal(-1, 0, 0);
        vertexConsumer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).normal(0, 1, 0);
        vertexConsumer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).normal(0, 1, 0);
        vertexConsumer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).normal(0, 0, -1);
        vertexConsumer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).normal(0, 0, -1);
    }
}
