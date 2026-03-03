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
    public static final List<CommissionWaypoint> waypoints = new java.util.concurrent.CopyOnWriteArrayList<>();

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
            boolean foundCommissions = false;
            List<String> commissionLines = new ArrayList<>();
            waypoints.clear();

            // Collect and sort strings from tab list alphabetically
            List<PlayerListEntry> sortedEntries = new ArrayList<>(client.player.networkHandler.getPlayerList());
            sortedEntries.sort((a, b) -> a.getProfile().name().compareToIgnoreCase(b.getProfile().name()));

            List<String> rawLines = new ArrayList<>();
            for (PlayerListEntry entry : sortedEntries) {
                if (entry.getDisplayName() != null) {
                    rawLines.add(entry.getDisplayName().getString());
                } else {
                    rawLines.add(entry.getProfile().name());
                }
            }

            String areaName = "";
            String mithrilPowder = "";
            String pickobulusStatus = "";

            for (String string : rawLines) {
                // Remove weird formatting chars before anything
                string = string.replaceAll("§[0-9a-fk-or]", "").trim();

                if (string.startsWith("Area: ")) {
                    areaName = string.substring(6).trim();
                } else if (string.startsWith("Mithril: ")) {
                    mithrilPowder = string.substring(9).trim();
                } else if (string.startsWith("Pickobulus: ")) {
                    pickobulusStatus = string.substring(12).trim();
                }

                if (string.equals("Commissions:") || string.equals("Commissions")) {
                    foundCommissions = true;
                    continue;
                }

                if (foundCommissions) {
                    if (string.isEmpty() || string.endsWith(":") || string.startsWith("Skills")
                            || string.startsWith("Events")
                            || string.startsWith("Dungeons") || string.startsWith("Powders")
                            || string.startsWith("Pickaxe Ability")) {
                        foundCommissions = false; // Stop parsing commissions when hitting the end of the section
                        continue;
                    }

                    if (string.contains(":")) {
                        String[] parts = string.split(":", 2);
                        String name = parts[0].trim();
                        String progress = parts[1].trim();

                        // Prevent adding DONE commissions or duplicate entries for display
                        if (!progress.equalsIgnoreCase("DONE") && !progress.isEmpty()) {
                            commissionLines.add(name + ": " + progress);
                        }
                    }
                }
            }

            try (java.io.FileWriter writer = new java.io.FileWriter("C:\\Users\\Omain\\Desktop\\meincrac\\log4.txt")) {
                writer.write("--- FOUND COMMISSIONS ---\n");
                for (String c : commissionLines) {
                    writer.write(c + "\n");
                }
            } catch (Exception ignored) {
            }

            List<String> displayLines = new ArrayList<>();
            if (!areaName.isEmpty())
                displayLines.add("§l" + areaName);
            if (!mithrilPowder.isEmpty())
                displayLines.add("Mithril: " + mithrilPowder);
            if (!pickobulusStatus.isEmpty())
                displayLines.add("Pickobulus: " + pickobulusStatus);
            if (!commissionLines.isEmpty()) {
                displayLines.add(""); // Empty line separator
                displayLines.add("§lCommissions");
                displayLines.addAll(commissionLines);
            }

            if (!displayLines.isEmpty()) {
                int y = 5;
                int maxW = 50; // Minimum width
                for (String s : displayLines) {
                    int w = client.textRenderer.getWidth(s.replaceAll("§[0-9a-fk-or]", ""));
                    if (w > maxW)
                        maxW = w;
                }

                int h = displayLines.size() * 10 + 4;
                context.fill(2, 2, maxW + 8, h + 2, 0x90000000); // 0xAARRGGBB format for color

                for (String line : displayLines) {
                    int color = 0xFFFFFFFF; // White text 100% opacity
                    if (line.equals("§l" + areaName))
                        color = 0xFF55FF55; // Light green for area
                    else if (line.equals("§lCommissions"))
                        color = 0xFFFFAA00; // Orange for Commissions title

                    context.drawTextWithShadow(client.textRenderer, Text.literal(line), 5, y, color);
                    y += 10;
                }

                if (PasunhackConfig.getInstance().showCommissionWaypoints) {
                    for (String line : commissionLines) {
                        String lowerLine = line.toLowerCase();
                        if (lowerLine.contains("upper mines")) {
                            waypoints.add(new CommissionWaypoint("Upper Mines", -130, 174, -50));
                        } else if (lowerLine.contains("royal mines")) {
                            waypoints.add(new CommissionWaypoint("Royal Mines", 130, 154, 30));
                        } else if (lowerLine.contains("lava springs") || lowerLine.contains("lava spring")) {
                            waypoints.add(new CommissionWaypoint("Lava Springs", 60, 197, -15));
                        } else if (lowerLine.contains("rampart's quarry") || lowerLine.contains("ramparts quarry")
                                || lowerLine.contains("rampart")) {
                            waypoints.add(new CommissionWaypoint("Rampart's Quarry", -100, 150, -20));
                        } else if (lowerLine.contains("cliffside veins")) {
                            waypoints.add(new CommissionWaypoint("Cliffside Veins", 40, 128, 40));
                        } else if (lowerLine.contains("glacite walker")) {
                            waypoints.add(new CommissionWaypoint("Glacite Walker", 0, 128, 150));
                        } else if (lowerLine.contains("aquamarine")) {
                            waypoints.add(new CommissionWaypoint("Aquamarine", 20, 136, 370));
                            waypoints.add(new CommissionWaypoint("Aquamarine", -14, 132, 386));
                            waypoints.add(new CommissionWaypoint("Aquamarine", 6, 137, 411));
                            waypoints.add(new CommissionWaypoint("Aquamarine", 50, 117, 302));
                        } else if (lowerLine.contains("onyx")) {
                            waypoints.add(new CommissionWaypoint("Onyx", 4, 127, 307));
                            waypoints.add(new CommissionWaypoint("Onyx", -3, 139, 434));
                            waypoints.add(new CommissionWaypoint("Onyx", 77, 118, 411));
                            waypoints.add(new CommissionWaypoint("Onyx", -68, 130, 404));
                        } else if (lowerLine.contains("peridot")) {
                            waypoints.add(new CommissionWaypoint("Peridot", 66, 144, 284));
                            waypoints.add(new CommissionWaypoint("Peridot", 94, 154, 284));
                            waypoints.add(new CommissionWaypoint("Peridot", -62, 147, 303));
                            waypoints.add(new CommissionWaypoint("Peridot", -77, 119, 283));
                            waypoints.add(new CommissionWaypoint("Peridot", 87, 122, 394));
                            waypoints.add(new CommissionWaypoint("Peridot", -73, 122, 456));
                        } else if (lowerLine.contains("citrine")) {
                            waypoints.add(new CommissionWaypoint("Citrine", -86, 143, 261));
                            waypoints.add(new CommissionWaypoint("Citrine", 74, 150, 327));
                            waypoints.add(new CommissionWaypoint("Citrine", 63, 137, 343));
                            waypoints.add(new CommissionWaypoint("Citrine", 38, 119, 386));
                            waypoints.add(new CommissionWaypoint("Citrine", 55, 150, 400));
                            waypoints.add(new CommissionWaypoint("Citrine", -45, 127, 415));
                            waypoints.add(new CommissionWaypoint("Citrine", -60, 144, 424));
                            waypoints.add(new CommissionWaypoint("Citrine", -54, 132, 410));
                        } else if (lowerLine.contains("base camp") || lowerLine.contains("campfire")) {
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
