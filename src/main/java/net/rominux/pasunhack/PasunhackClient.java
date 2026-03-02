package net.rominux.pasunhack;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class PasunhackClient implements ClientModInitializer {

    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        // Enregistrement de la touche F6 avec le constructeur à 3 arguments (plus stable)
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pasunhack.toggle_autominer",
                GLFW.GLFW_KEY_F6,
                "category.pasunhack.main"));

        // Event Tick: Exécuté 20 fois par seconde (à chaque tick du jeu)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                AutoMiner.toggle();
                String status = AutoMiner.isEnabled() ? "§aActivé" : "§cDésactivé";
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("AutoMiner: " + status), true);
                }
            }

            // Appel de la logique principale de l'Auto-Miner
            AutoMiner.tick(client);
        });
    }
}
