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
import java.util.List;
import java.util.ArrayList;

public class PickobulusRender {
    private static List<BlockPos> titaniumBlocks = new ArrayList<>();
    private static long lastScanTime = 0;

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

            List<CommissionsOverlay.CommissionWaypoint> sortedWaypoints = new ArrayList<>(CommissionsOverlay.waypoints);
            sortedWaypoints.sort((w1, w2) -> {
                String n1 = w1.name.toLowerCase();
                String n2 = w2.name.toLowerCase();
                int p1 = n1.contains("mithril") ? 1 : (n1.contains("titanium") ? 2 : 3);
                int p2 = n2.contains("mithril") ? 1 : (n2.contains("titanium") ? 2 : 3);
                return Integer.compare(p1, p2);
            });

            java.util.Map<BlockPos, Integer> drawnCounts = new java.util.HashMap<>();

            for (CommissionsOverlay.CommissionWaypoint wp : sortedWaypoints) {
                BlockPos bPos = new BlockPos(wp.x, wp.y, wp.z);
                int count = drawnCounts.getOrDefault(bPos, 0);
                drawnCounts.put(bPos, count + 1);

                // Draw Pillar (Beacon Beam)
                Box box = new Box(wp.x - 0.1, wp.y, wp.z - 0.1, wp.x + 0.1, wp.y + 100, wp.z + 0.1)
                        .offset(-camPos.x, -camPos.y, -camPos.z);

                VertexConsumer lineConsumer = immediate.getBuffer(RenderLayer.getLines());
                drawBox(context.matrices(), lineConsumer, box, 1.0f, 0.5f, 0.0f, 0.8f);

                Vec3d waypointVec3d = new Vec3d(wp.x, wp.y, wp.z);
                double distance = camPos.distanceTo(waypointVec3d);
                String label = "\u00A7l\u00A7n" + wp.name + "\u00A7r \u00A7a(" + (int) distance + "m)";

                int textColor = 0xFFFFFFFF; // Blanc par défaut (Titanium ou autre)
                String lowerName = wp.name.toLowerCase();
                if (lowerName.contains("goblin") || lowerName.contains("glacite")) {
                    textColor = 0xFFFF5555; // Rouge
                } else if (lowerName.contains("mithril")) {
                    textColor = 0xFF5555FF; // Bleu
                } else if (lowerName.contains("emissary")) {
                    textColor = 0xFFFFAA00; // Orange
                }

                context.matrices().push();

                double yOffset = 1.0 - (count * 0.4);

                // 1. Déplacer au centre du waypoint (légèrement au-dessus + décalage)
                context.matrices().translate(waypointVec3d.x - camPos.x, waypointVec3d.y + yOffset - camPos.y,
                        waypointVec3d.z - camPos.z);

                // 2. Faire pivoter le texte pour qu'il regarde toujours la caméra
                // (Billboarding)
                context.matrices()
                        .multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                context.matrices()
                        .multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

                // 3. Réduire la taille du texte (Scale)
                float scale = 0.025f * (float) Math.max(2.0, distance / 10.0);
                // On utilise un scale négatif pour que le texte ne soit pas inversé (miroir)
                context.matrices().scale(-scale, -scale, scale);

                // 4. Rendu du texte à travers les murs (SEE_THROUGH)
                float xOffset = -textRenderer.getWidth(label) / 2.0f; // Centrer le texte
                org.joml.Matrix4f positionMatrix = context.matrices().peek().getPositionMatrix();

                textRenderer.draw(
                        label,
                        xOffset,
                        0f,
                        textColor, // Couleur du texte
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

        if (PasunhackConfig.getInstance().titaniumTracer) {
            boolean hasTitanium = false;
            for (String comm : CommissionsOverlay.activeCommissions) {
                if (comm.toLowerCase().contains("titanium")) {
                    hasTitanium = true;
                    break;
                }
            }
            if (hasTitanium) {
                long time = System.currentTimeMillis();
                if (time - lastScanTime > 1500) {
                    lastScanTime = time;
                    int radius = PasunhackConfig.getInstance().titaniumTracerRadius;
                    BlockPos playerPos = client.player.getBlockPos();
                    new Thread(() -> {
                        try {
                            List<BlockPos> temp = new ArrayList<>();
                            for (BlockPos bp : BlockPos.iterate(playerPos.add(-radius, -radius, -radius),
                                    playerPos.add(radius, radius, radius))) {
                                net.minecraft.block.Block block = client.world.getBlockState(bp).getBlock();
                                if (block == net.minecraft.block.Blocks.POLISHED_DIORITE
                                        || block == net.minecraft.block.Blocks.END_STONE) {
                                    temp.add(bp.toImmutable());
                                }
                            }
                            if (!temp.isEmpty()) {
                                temp.sort(java.util.Comparator.comparingDouble(p -> p.getSquaredDistance(playerPos)));
                                titaniumBlocks = java.util.Collections.singletonList(temp.get(0));
                            } else {
                                titaniumBlocks = new ArrayList<>();
                            }
                        } catch (Exception ignored) {
                        }
                    }).start();
                }

                net.minecraft.client.render.VertexConsumerProvider.Immediate immediate = client.getBufferBuilders()
                        .getEntityVertexConsumers();
                VertexConsumer lineConsumer = immediate.getBuffer(RenderLayer.getLines());
                for (BlockPos pos : titaniumBlocks) {
                    Vec3d target = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    drawTracer(context.matrices(), lineConsumer, camPos, target, 1f, 1f, 1f, 1f); // White
                }
                immediate.draw();
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

    private static void drawTracer(net.minecraft.client.util.math.MatrixStack matrices, VertexConsumer vertexConsumer,
            Vec3d camPos, Vec3d target, float r, float g, float b, float a) {
        org.joml.Matrix4f matrix = matrices.peek().getPositionMatrix();
        Vec3d end = target.subtract(camPos);
        org.joml.Vector3f dir = new org.joml.Vector3f((float) end.x, (float) end.y, (float) end.z).normalize();
        vertexConsumer.vertex(matrix, 0, 0, 0).color(r, g, b, a).normal(dir.x, dir.y, dir.z);
        vertexConsumer.vertex(matrix, (float) end.x, (float) end.y, (float) end.z).color(r, g, b, a).normal(dir.x,
                dir.y, dir.z);
    }
}
