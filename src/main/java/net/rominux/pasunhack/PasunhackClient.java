package net.rominux.pasunhack;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class PasunhackClient implements ClientModInitializer {

    private static boolean wasF6Pressed = false;

    @Override
    public void onInitializeClient() {
        // Event Tick: Exécuté 20 fois par seconde (à chaque tick du jeu)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            
            // Lecture directe de la touche F6 au niveau du système (100% fiable)
            if (client.getWindow() != null) {
                long window = client.getWindow().getHandle();
                boolean isF6Pressed = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_F6) == GLFW.GLFW_PRESS;

                if (isF6Pressed && !wasF6Pressed) {
                    AutoMiner.toggle();
                    String status = AutoMiner.isEnabled() ? "§aActivé" : "§cDésactivé";
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("AutoMiner: " + status), true);
                    }
                }
                wasF6Pressed = isF6Pressed;
            }

            // Appel de la logique principale de l'Auto-Miner
            AutoMiner.tick(client);
        });
    }
}
