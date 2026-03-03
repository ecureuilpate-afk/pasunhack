package net.rominux.pasunhack;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
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
        VertexConsumer lineConsumer = vertexConsumers.getBuffer(RenderLayer.getLines());

        if (showPickobulus && client.player.getMainHandStack().getItem().toString().contains("pickaxe")) {
            HitResult hit = client.crosshairTarget;
            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = ((BlockHitResult) hit).getBlockPos();
                if (pos.isWithinDistance(camPos, 6.0)) {
                    Box box = new Box(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1, pos.getX() + 2, pos.getY() + 2,
                            pos.getZ() + 2).offset(-camPos.x, -camPos.y, -camPos.z);
                    drawBox(context.matrices(), lineConsumer, box, 0.0f, 1.0f, 0.0f, 1.0f);
                }
            }
        }

        if (showWaypoints) {
            for (CommissionsOverlay.CommissionWaypoint wp : CommissionsOverlay.waypoints) {
                Box box = new Box(wp.x - 0.5, wp.y, wp.z - 0.5, wp.x + 0.5, wp.y + 100, wp.z + 0.5)
                        .offset(-camPos.x, -camPos.y, -camPos.z);
                drawBox(context.matrices(), lineConsumer, box, 1.0f, 0.5f, 0.0f, 0.8f);
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
