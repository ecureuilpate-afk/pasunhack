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

                double dist = Math.sqrt((wp.x - camPos.x) * (wp.x - camPos.x) + (wp.y - camPos.y) * (wp.y - camPos.y)
                        + (wp.z - camPos.z) * (wp.z - camPos.z));
                float scale = (float) Math.max(0.025, dist * 0.0035);

                context.matrices().push();
                context.matrices().translate(wp.x + 0.0 - camPos.x, wp.y + 2.0 - camPos.y, wp.z + 0.0 - camPos.z);
                context.matrices().multiply(camera.getRotation());
                context.matrices().scale(-scale, -scale, scale);

                String text = wp.name + " (" + (int) dist + "m)";
                float textWidth = (float) -textRenderer.getWidth(text) / 2;
                org.joml.Matrix4f posMat = context.matrices().peek().getPositionMatrix();

                textRenderer.draw(
                        text,
                        textWidth,
                        0f,
                        0x33FFFFFF, // Semi-transparent text for see-through
                        false,
                        posMat,
                        immediate,
                        net.minecraft.client.font.TextRenderer.TextLayerType.SEE_THROUGH,
                        0x40000000,
                        15728880);

                textRenderer.draw(
                        text,
                        textWidth,
                        0f,
                        0xFFFFFFFF, // Opaque text for normal rendering
                        false,
                        posMat,
                        immediate,
                        net.minecraft.client.font.TextRenderer.TextLayerType.NORMAL,
                        0x00000000,
                        15728880);
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
