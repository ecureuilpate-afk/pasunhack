package net.rominux.pasunhack;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CommissionsOverlay implements HudRenderCallback {
    public static final List<CommissionWaypoint> waypoints = new ArrayList<>();

    public static class CommissionWaypoint {
        public String name;
        public int x, y, z;

        public CommissionWaypoint(String name, int x, int y, int z) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        if (!PasunhackConfig.getInstance().showCommissionsHud)
            return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.player.networkHandler == null)
            return;

        List<PlayerListEntry> playerList = new ArrayList<>(client.player.networkHandler.getPlayerList());
        playerList.sort((a, b) -> {
            net.minecraft.scoreboard.Team teamA = a.getScoreboardTeam();
            net.minecraft.scoreboard.Team teamB = b.getScoreboardTeam();
            int result = Boolean.compare(a.getGameMode() == net.minecraft.world.GameMode.SPECTATOR,
                    b.getGameMode() == net.minecraft.world.GameMode.SPECTATOR);
            if (result != 0)
                return result;
            result = (teamA != null ? teamA.getName() : "").compareTo(teamB != null ? teamB.getName() : "");
            if (result != 0)
                return result;
            return a.getProfile().name().compareToIgnoreCase(b.getProfile().name());
        });

        List<String> commissionLines = new ArrayList<>();
        boolean foundCommissions = false;
        int linesToRead = 0;

        for (PlayerListEntry entry : playerList) {
            if (entry.getDisplayName() == null)
                continue;
            String cleanText = entry.getDisplayName().getString().replaceAll("§[0-9a-fk-or]", "").trim();

            if (foundCommissions) {
                if (cleanText.isEmpty() || linesToRead <= 0) {
                    break;
                }
                commissionLines.add(cleanText);
                linesToRead--;
            } else if (cleanText.contains("Commissions")) {
                foundCommissions = true;
                commissionLines.add("§l" + cleanText);
                linesToRead = 4;
            }
        }

        if (!commissionLines.isEmpty()) {
            int y = 5;
            int h = commissionLines.size() * 10 + 4;
            int maxW = 100;
            for (String s : commissionLines) {
                if (client.textRenderer.getWidth(s) > maxW)
                    maxW = client.textRenderer.getWidth(s);
            }
            context.fill(2, 2, maxW + 6, h, 0x90000000);

            waypoints.clear();
            for (String line : commissionLines) {
                context.drawTextWithShadow(client.textRenderer, Text.literal(line), 5, y, 0xFFFFFF);
                y += 10;

                if (PasunhackConfig.getInstance().showCommissionWaypoints) {
                    String lowerLine = line.toLowerCase();
                    if (lowerLine.contains("upper mines")) {
                        waypoints.add(new CommissionWaypoint("Upper Mines", 0, 100, -50));
                    } else if (lowerLine.contains("royal mines")) {
                        waypoints.add(new CommissionWaypoint("Royal Mines", 160, 140, 20));
                    } else if (lowerLine.contains("lava springs")) {
                        waypoints.add(new CommissionWaypoint("Lava Springs", 60, 200, 20));
                    } else if (lowerLine.contains("rampart's quarry")) {
                        waypoints.add(new CommissionWaypoint("Rampart's Quarry", -100, 140, 0));
                    } else if (lowerLine.contains("cliffside veins")) {
                        waypoints.add(new CommissionWaypoint("Cliffside Veins", 10, 130, 40));
                    } else if (lowerLine.contains("forge basin")) {
                        waypoints.add(new CommissionWaypoint("Forge Basin", 0, 170, -40));
                    } else if (lowerLine.contains("dwarven village")) {
                        waypoints.add(new CommissionWaypoint("Dwarven Village", 0, 200, 100));
                    } else if (lowerLine.contains("goblin burrows")) {
                        waypoints.add(new CommissionWaypoint("Goblin Burrows", -40, 140, 140));
                    } else if (lowerLine.contains("goblin slayer")) {
                        waypoints.add(new CommissionWaypoint("Goblin Slayer", -40, 140, 140));
                    } else if (lowerLine.contains("ice walker") || lowerLine.contains("great ice wall")) {
                        waypoints.add(new CommissionWaypoint("Ice Walker", 0, 130, 150));
                    }
                }
            }
        }
    }
}
