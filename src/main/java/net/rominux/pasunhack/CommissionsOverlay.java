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

        try {
            List<String> commissionLines = new ArrayList<>();
            waypoints.clear();

            Collection<PlayerListEntry> playerList = client.player.networkHandler.getPlayerList();
            List<PlayerListEntry> sortedList = new ArrayList<>(playerList);
            sortedList.sort((a, b) -> a.getProfile().name().compareToIgnoreCase(b.getProfile().name()));

            boolean foundCommissions = false;
            java.util.regex.Pattern COMM_PATTERN = java.util.regex.Pattern.compile("(?<name>.*): (?<progress>.*)%?");

            for (PlayerListEntry entry : sortedList) {
                if (entry.getDisplayName() == null)
                    continue;

                String string = entry.getDisplayName().getString();

                if (foundCommissions) {
                    if (!string.startsWith(" "))
                        break;

                    string = string.substring(1);
                    java.util.regex.Matcher matcher = COMM_PATTERN.matcher(string);
                    if (matcher.matches()) {
                        String name = matcher.group("name");
                        String progress = matcher.group("progress");
                        if (!progress.equals("DONE")) {
                            String displayLine = name + ": " + progress + "%";
                            commissionLines.add(displayLine);
                        }
                    }
                } else if (string.startsWith("Commissions")) {
                    foundCommissions = true;
                }
            }

            try (java.io.FileWriter writer = new java.io.FileWriter("C:\\Users\\Omain\\Desktop\\meincrac\\log4.txt")) {
                writer.write("--- FOUND COMMISSIONS ---\n");
                for (String c : commissionLines) {
                    writer.write(c + "\n");
                }
            } catch (Exception ignored) {
            }

            if (!commissionLines.isEmpty()) {
                int y = 5;
                int h = (commissionLines.size() + 1) * 10 + 4;
                int maxW = client.textRenderer.getWidth("§lCommissions");
                for (String s : commissionLines) {
                    if (client.textRenderer.getWidth(s) > maxW)
                        maxW = client.textRenderer.getWidth(s);
                }
                context.fill(2, 2, maxW + 6, h, 0x90000000);

                context.drawTextWithShadow(client.textRenderer, Text.literal("§lCommissions"), 5, y, 0xFFFFFF);
                y += 10;

                for (String line : commissionLines) {
                    context.drawTextWithShadow(client.textRenderer, Text.literal(line), 5, y, 0xFFFFFF);
                    y += 10;

                    if (PasunhackConfig.getInstance().showCommissionWaypoints) {
                        String lowerLine = line.toLowerCase();
                        if (lowerLine.contains("upper mines")) {
                            waypoints.add(new CommissionWaypoint("Upper Mines", -130, 174, -50));
                        } else if (lowerLine.contains("royal mines")) {
                            waypoints.add(new CommissionWaypoint("Royal Mines", 130, 154, 30));
                        } else if (lowerLine.contains("lava springs")) {
                            waypoints.add(new CommissionWaypoint("Lava Springs", 60, 197, -15));
                        } else if (lowerLine.contains("rampart's quarry") || lowerLine.contains("ramparts quarry")) {
                            waypoints.add(new CommissionWaypoint("Rampart's Quarry", -100, 150, -20));
                        } else if (lowerLine.contains("cliffside veins")) {
                            waypoints.add(new CommissionWaypoint("Cliffside Veins", 40, 128, 40));
                        } else if (lowerLine.contains("glacite walker")) {
                            waypoints.add(new CommissionWaypoint("Glacite Walker", 0, 128, 150));
                        } else if (lowerLine.contains("aquamarine")) {
                            waypoints.add(new CommissionWaypoint("Aquamarine", 20, 136, 370));
                        } else if (lowerLine.contains("onyx")) {
                            waypoints.add(new CommissionWaypoint("Onyx", 4, 127, 307));
                        } else if (lowerLine.contains("peridot")) {
                            waypoints.add(new CommissionWaypoint("Peridot", 66, 144, 284));
                        } else if (lowerLine.contains("citrine")) {
                            waypoints.add(new CommissionWaypoint("Citrine", -86, 143, 261));
                        } else if (lowerLine.contains("base camp")) {
                            waypoints.add(new CommissionWaypoint("Base Camp", -7, 126, 229));
                        }
                    }
                }
            }
        } catch (Exception e) {
            try (java.io.FileWriter writer = new java.io.FileWriter("C:\\Users\\Omain\\Desktop\\meincrac\\log5.txt")) {
                writer.write("Error: " + e.toString() + "\nMessage: " + e.getMessage());
            } catch (Exception ignored) {
            }
        }
    }
}
